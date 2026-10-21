#!/usr/bin/env bash
# Installs or updates the Pulse backend on an Amazon Linux 2023 EC2 instance.
#   sudo bash deploy/setup.sh            # first install: asks for the API keys
#   sudo bash deploy/setup.sh --keys     # replace the API keys
#   sudo bash deploy/setup.sh --service-account ~/live-fame-sa.json   # enable the points economy (wallets in Firestore)
#   sudo bash deploy/setup.sh            # later runs: updates the code and restarts
# HTTPS comes from Caddy with a free Let's Encrypt certificate for <ip-with-dashes>.sslip.io.
set -euo pipefail

[[ $EUID -eq 0 ]] || { echo "Run with sudo."; exit 1; }
SRC="$(cd "$(dirname "$0")/.." && pwd)"
APP=/opt/pulse
ENV_FILE=/etc/pulse/env

echo "== Node.js"
if ! command -v node >/dev/null || [[ "$(node -p 'process.versions.node.split(".")[0]')" -lt 20 ]]; then
  dnf install -y nodejs22 || dnf install -y nodejs20
  for n in node-22 node-20; do command -v "$n" >/dev/null && ln -sf "$(command -v "$n")" /usr/local/bin/node && break; done
fi
NODE="$(command -v node)"
echo "using $NODE $("$NODE" --version)"

echo "== Caddy (HTTPS)"
if ! command -v caddy >/dev/null; then
  curl -fsSL "https://caddyserver.com/api/download?os=linux&arch=amd64" -o /usr/local/bin/caddy
  chmod 755 /usr/local/bin/caddy
fi
id caddy >/dev/null 2>&1 || useradd --system --home-dir /var/lib/caddy --create-home --shell /sbin/nologin caddy

echo "== Public hostname"
TOKEN="$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')"
IP="$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4)"
HOST="${PULSE_HOST:-${IP//./-}.sslip.io}"
echo "backend URL will be https://$HOST"

echo "== API keys"
mkdir -p /etc/pulse
if [[ ! -s "$ENV_FILE" || "${1:-}" == "--keys" ]]; then
  read -rsp "Groq API key (input hidden): " GROQ; echo
  read -rsp "Gemini API key (input hidden): " GEMINI; echo
  (umask 077; cat > "$ENV_FILE" <<EOF
GROQ_API_KEY=$GROQ
GEMINI_API_KEY=$GEMINI
FIREBASE_PROJECT_ID=live-fame
GROQ_MODEL=whisper-large-v3
GEMINI_MODEL=gemini-flash-lite-latest,gemini-flash-latest
EOF
  )
  chmod 600 "$ENV_FILE"
fi

echo "== App code"
id pulse >/dev/null 2>&1 || useradd --system --no-create-home --shell /sbin/nologin pulse

# The points economy writes wallets with a Firebase service account. Readable by the service user only.
SA_FILE=/etc/pulse/service-account.json
if [[ "${1:-}" == "--service-account" ]]; then
  [[ -s "${2:-}" ]] || { echo "Usage: sudo bash deploy/setup.sh --service-account <key.json>"; exit 1; }
  install -m 640 -o root -g pulse "$2" "$SA_FILE"
  shred -u "$2" 2>/dev/null || rm -f "$2"
  grep -q '^GOOGLE_APPLICATION_CREDENTIALS=' "$ENV_FILE" || echo "GOOGLE_APPLICATION_CREDENTIALS=$SA_FILE" >> "$ENV_FILE"
  echo "service account installed; the points economy is on"
fi
id pulse >/dev/null 2>&1 || useradd --system --no-create-home --shell /sbin/nologin pulse
mkdir -p "$APP"
cp -r "$SRC/src" "$SRC/package.json" "$APP/"
chown -R root:root "$APP"

cat > /etc/systemd/system/pulse.service <<EOF
[Unit]
Description=Pulse backend
After=network-online.target
Wants=network-online.target

[Service]
User=pulse
EnvironmentFile=$ENV_FILE
WorkingDirectory=$APP
ExecStart=$NODE $APP/src/server.mjs
Restart=always
RestartSec=2
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

mkdir -p /etc/caddy
cat > /etc/caddy/Caddyfile <<EOF
$HOST {
	encode gzip
	request_body {
		max_size 1MB
	}
	reverse_proxy 127.0.0.1:8787
}
EOF

cat > /etc/systemd/system/caddy.service <<EOF
[Unit]
Description=Caddy HTTPS proxy
After=network-online.target
Wants=network-online.target

[Service]
User=caddy
Environment=HOME=/var/lib/caddy
ExecStart=/usr/local/bin/caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
ExecReload=/usr/local/bin/caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
AmbientCapabilities=CAP_NET_BIND_SERVICE
Restart=always

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now pulse caddy
systemctl restart pulse caddy

echo "== Checking the API keys"
(set -a; source "$ENV_FILE"; set +a; cd "$APP" && "$NODE" src/check.mjs) || echo "Key check failed: fix with 'sudo bash deploy/setup.sh --keys'."

echo "== Checking HTTPS (the first certificate can take up to a minute)"
for _ in $(seq 1 12); do
  if curl -fsS "https://$HOST/healthz" >/dev/null 2>&1; then
    echo "ok    https://$HOST is live. Put this in the app's local.properties:"
    echo "      pulse.backendUrl=https://$HOST"
    exit 0
  fi
  sleep 5
done
echo "HTTPS isn't answering yet. Check that the security group allows ports 80 and 443, then: sudo journalctl -u caddy -n 50"
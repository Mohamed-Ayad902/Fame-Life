# Pulse backend

The trusted server between the app and the AI providers. The app never holds a provider key.

| Route | Body | Does |
| --- | --- | --- |
| `POST /v1/transcribe` | one utterance as 16 kHz mono WAV (≤ 1 MB) | Groq Whisper; detects Arabic/English itself. Returns `{text, language}`. |
| `POST /v1/audience` | `AudienceRequest` JSON (`PROTOCOL_VERSION = 1`) | Gemini writes the comments. Returns `AudienceResponseDto` JSON. |
| `GET /healthz` | – | liveness |

Every `/v1` call needs `Authorization: Bearer <Firebase ID token>` for project `live-fame`; the server verifies the
signature itself (no Admin SDK). Per-user limits (in memory, reset on restart) protect the free provider quotas.
Logs hold the route, status, latency and a hashed uid, never speech or comments. Audio is forwarded to Groq and not
stored.

No npm dependencies: Node 20+ only.

```
npm test        # unit and HTTP tests, no network or keys needed
npm run check   # with the keys in the environment: verifies Groq, makes one real Gemini request
```

## Deploy on EC2 (Amazon Linux 2023)

1. Security group: allow inbound TCP 80 and 443 from anywhere (Let's Encrypt and the app). Limit SSH (22) to your IP.
2. Give the instance an Elastic IP, so the address (and the `sslip.io` hostname built from it) survives a stop/start.
3. From the repo root on your computer:
   ```
   scp -i HandiMart-app-key.pem -r backend ec2-user@<public-ip>:~/
   ssh -t -i HandiMart-app-key.pem ec2-user@<public-ip> 'sudo bash backend/deploy/setup.sh'
   ```
   The script asks for the Groq and Gemini keys (hidden input) and stores them in `/etc/pulse/env` (root-only).
   It prints the backend URL at the end.
4. Put that URL in the app's `local.properties`: `pulse.backendUrl=https://<ip-with-dashes>.sslip.io`

Update the code later by repeating step 3. Replace keys with `sudo bash backend/deploy/setup.sh --keys`.
Logs: `sudo journalctl -u pulse -f`. Models are set in `/etc/pulse/env` (`GROQ_MODEL`, and `GEMINI_MODEL` as a comma-separated list tried in order
when a model is busy or slow, all within 5 s); restart with
`sudo systemctl restart pulse` after editing.

## Costs

Groq and Gemini free tiers have per-minute and per-day limits that the providers can change; when one is hit the
server answers 503 and the app falls back to its on-device audience. Gemini's free tier allows Google to use requests
to improve its products, so switch to a paid key before real users. The EC2 instance and its public IPv4 address are
billed by AWS unless covered by your Free Tier.

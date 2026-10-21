/**
 * Per-user request limits held in memory. They protect the free provider quotas from one runaway client; they reset
 * when the server restarts, so they are not a billing record.
 */
export class RateLimiter {
  constructor({ perMinute, perDay, now = () => Date.now() }) {
    this.perMinute = perMinute
    this.perDay = perDay
    this.now = now
    this.users = new Map()
  }

  /** Records the request and returns true, or returns false when the user is over a limit. */
  take(uid) {
    const now = this.now()
    const day = Math.floor(now / 86_400_000)
    let user = this.users.get(uid)
    if (!user || user.day !== day) {
      user = { day, dayCount: 0, recent: [] }
      this.users.set(uid, user)
    }
    user.recent = user.recent.filter((t) => now - t < 60_000)
    if (user.recent.length >= this.perMinute || user.dayCount >= this.perDay) return false
    user.recent.push(now)
    user.dayCount++
    if (this.users.size > 10_000) this.prune(day)
    return true
  }

  prune(day) {
    for (const [uid, user] of this.users) if (user.day !== day) this.users.delete(uid)
  }
}

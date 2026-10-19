package org.mayad.fame_life.core.platform

import org.mayad.fame_life.data.local.nowEpochMs

/** Opens the platform share sheet. Never posts anywhere by itself. Injected so ViewModels can be tested. */
fun interface ShareService {
    fun share(text: String)
}

/** Wall-clock time in epoch milliseconds, for records that are stored. */
fun interface EpochClock {
    fun now(): Long
}

/** Whether the OS asks for reduced motion, read when needed (it can change while the app runs). */
fun interface MotionPreference {
    fun reduced(): Boolean
}

internal val PlatformShare = ShareService(::shareText)
internal val SystemEpochClock = EpochClock(::nowEpochMs)
internal val SystemMotionPreference = MotionPreference(::isReduceMotionEnabled)

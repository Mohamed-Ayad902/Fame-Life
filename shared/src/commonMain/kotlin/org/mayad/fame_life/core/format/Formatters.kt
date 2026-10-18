package org.mayad.fame_life.core.format

import kotlin.math.abs
import kotlin.math.roundToInt

/** 24.5K, 1.2M, 870. One decimal below 100 of a unit, none above, never a trailing ".0". */
fun compactCount(value: Int): String {
    val v = abs(value.toLong())
    val sign = if (value < 0) "-" else ""
    fun scaled(unit: Long, suffix: String): String {
        val tenths = (v * 10.0 / unit).roundToInt()
        val text = if (tenths >= 1000 || tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        return sign + text + suffix
    }
    return when {
        v >= 999_950_000 -> scaled(1_000_000_000, "B")
        v >= 999_950 -> scaled(1_000_000, "M")
        v >= 1_000 -> scaled(1_000, "K")
        else -> "$sign$v"
    }
}

/** mm:ss under an hour, h:mm:ss above. */
fun clock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    fun two(n: Int) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "${two(m)}:${two(sec)}"
}

/** "12 min", "1 h 05 min", "45 s". */
fun durationLabel(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min"
        else -> "${s / 3600} h ${((s % 3600) / 60).toString().padStart(2, '0')} min"
    }
}

fun hoursAgoLabel(hours: Int): String = when {
    hours <= 0 -> "now"
    hours < 24 -> "${hours}h"
    else -> "${hours / 24}d"
}

package org.mayad.fame_life.domain.ai

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Cuts a microphone stream into utterances by loudness, so each spoken sentence can be transcribed on its own.
 *
 * It adapts to the room: the first few hundred milliseconds only measure the noise floor, the floor then follows
 * the quiet level between sentences, and speech starts once a few frames in a row are clearly louder than that. Each utterance keeps a short lead-in so the first syllable isn't
 * clipped. Feed it fixed-size frames of 16-bit mono PCM; it returns a finished utterance when one ends.
 */
class UtteranceDetector(
    private val sampleRate: Int = SAMPLE_RATE,
    private val frameMs: Int = FRAME_MS,
    private val startFrames: Int = 3,
    private val endSilenceMs: Int = 800,
    private val preRollMs: Int = 300,
    private val minSpeechMs: Int = 350,
    private val maxUtteranceMs: Int = 15_000,
    private val marginDb: Double = 12.0,
    private val minThresholdDb: Double = -50.0,
    private val calibrationMs: Int = 300,
) {
    private var noiseFloorDb = -60.0
    private var calibratedFrames = 0
    private val preRoll = ArrayDeque<ShortArray>()
    private val pending = ArrayDeque<ShortArray>()
    private val utterance = ArrayList<ShortArray>()
    private var speaking = false
    private var voicedMs = 0
    private var silenceMs = 0

    val isSpeaking: Boolean get() = speaking

    /** Accepts one frame and returns a finished utterance's samples, or null. The frame is copied. */
    fun accept(frame: ShortArray): ShortArray? {
        val copy = frame.copyOf()
        val db = levelDb(copy)
        if (calibratedFrames * frameMs < calibrationMs) {
            // Without this, a room louder than the starting floor would read as speech before the floor could learn it.
            calibratedFrames++
            noiseFloorDb = if (calibratedFrames == 1) db else noiseFloorDb + (db - noiseFloorDb) / calibratedFrames
            preRoll.addLast(copy)
            while (preRoll.size * frameMs > preRollMs) preRoll.removeFirst()
            return null
        }
        val voiced = db > max(noiseFloorDb + marginDb, minThresholdDb)
        return if (speaking) continueUtterance(copy, voiced) else listen(copy, db, voiced)
    }

    /** Ends any utterance in progress (for example when the mic stops) and returns it if it holds enough speech. */
    fun flush(): ShortArray? = if (speaking) finish() else null

    private fun listen(frame: ShortArray, db: Double, voiced: Boolean): ShortArray? {
        if (voiced) {
            pending.addLast(frame)
            if (pending.size >= startFrames) {
                speaking = true
                utterance.addAll(preRoll)
                utterance.addAll(pending)
                voicedMs = pending.size * frameMs
                silenceMs = 0
                preRoll.clear()
                pending.clear()
            }
            return null
        }
        // Quiet frame: the room's level moves the floor, falling quickly and rising slowly.
        noiseFloorDb += (db - noiseFloorDb) * (if (db < noiseFloorDb) 0.2 else 0.02)
        preRoll.addAll(pending)
        pending.clear()
        preRoll.addLast(frame)
        while (preRoll.size * frameMs > preRollMs) preRoll.removeFirst()
        return null
    }

    private fun continueUtterance(frame: ShortArray, voiced: Boolean): ShortArray? {
        utterance.add(frame)
        if (voiced) {
            voicedMs += frameMs
            silenceMs = 0
        } else {
            silenceMs += frameMs
        }
        return when {
            silenceMs >= endSilenceMs -> finish()
            utterance.size * frameMs >= maxUtteranceMs -> finish()
            else -> null
        }
    }

    private fun finish(): ShortArray? {
        // Keep a little of the trailing quiet; the rest is just the pause that ended the sentence.
        val trailingFrames = (silenceMs - TRAILING_KEEP_MS).coerceAtLeast(0) / frameMs
        val frames = utterance.subList(0, utterance.size - trailingFrames)
        val result = if (voicedMs >= minSpeechMs) concat(frames) else null
        utterance.clear()
        speaking = false
        voicedMs = 0
        silenceMs = 0
        return result
    }

    private fun concat(frames: List<ShortArray>): ShortArray {
        val out = ShortArray(frames.sumOf { it.size })
        var offset = 0
        for (f in frames) {
            f.copyInto(out, offset)
            offset += f.size
        }
        return out
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_MS = 30
        const val FRAME_SAMPLES = SAMPLE_RATE * FRAME_MS / 1000
        private const val TRAILING_KEEP_MS = 200

        /** RMS level in dBFS (0 is full scale, silence is around -90). */
        fun levelDb(frame: ShortArray): Double {
            if (frame.isEmpty()) return -90.0
            var sum = 0.0
            for (s in frame) sum += s.toDouble() * s
            val rms = sqrt(sum / frame.size) / 32768.0
            return if (rms <= 1e-9) -90.0 else max(20 * log10(rms), -90.0)
        }
    }
}

/** Wraps 16-bit mono PCM in a WAV container, the format the transcription server accepts. */
object Wav {
    fun encode(samples: ShortArray, sampleRate: Int = UtteranceDetector.SAMPLE_RATE): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArray(44 + dataSize)
        fun putString(at: Int, s: String) = s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
        fun putInt(at: Int, v: Int) { for (i in 0 until 4) out[at + i] = (v shr (8 * i)).toByte() }
        fun putShort(at: Int, v: Int) { out[at] = v.toByte(); out[at + 1] = (v shr 8).toByte() }
        putString(0, "RIFF"); putInt(4, 36 + dataSize); putString(8, "WAVE")
        putString(12, "fmt "); putInt(16, 16); putShort(20, 1); putShort(22, 1)
        putInt(24, sampleRate); putInt(28, sampleRate * 2); putShort(32, 2); putShort(34, 16)
        putString(36, "data"); putInt(40, dataSize)
        samples.forEachIndexed { i, s -> putShort(44 + i * 2, s.toInt()) }
        return out
    }
}

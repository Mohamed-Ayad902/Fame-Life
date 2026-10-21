package org.mayad.fame_life.domain.ai

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UtteranceDetectorTest {
    private val frame = UtteranceDetector.FRAME_SAMPLES
    private val random = Random(7)

    private fun noise(amplitude: Int) = ShortArray(frame) { random.nextInt(-amplitude, amplitude + 1).toShort() }
    private fun voice(amplitude: Double = 8000.0) = ShortArray(frame) { (amplitude * sin(2 * PI * 220 * it / 16_000.0)).toInt().toShort() }

    /** Feeds frames and collects every finished utterance. */
    private fun UtteranceDetector.run(frames: List<ShortArray>) = frames.mapNotNull { accept(it) }

    private fun ms(duration: Int, make: () -> ShortArray) = List(duration / UtteranceDetector.FRAME_MS) { make() }

    @Test
    fun aSentenceBetweenPausesBecomesOneUtteranceWithItsLeadIn() {
        val detector = UtteranceDetector()
        val out = detector.run(ms(1_000) { noise(60) } + ms(1_200) { voice() } + ms(1_000) { noise(60) })
        assertEquals(1, out.size)
        val seconds = out.single().size / 16_000.0
        // 1.2 s of speech plus up to 0.3 s lead-in and 0.2 s of trailing quiet.
        assertTrue(seconds in 1.2..1.75, "utterance was $seconds s")
    }

    @Test
    fun clicksAndRoomNoiseAreIgnored() {
        val detector = UtteranceDetector()
        val out = detector.run(ms(2_000) { noise(300) } + listOf(voice(), voice()) + ms(2_000) { noise(300) })
        assertTrue(out.isEmpty())
        assertNull(detector.flush())
    }

    @Test
    fun aSteadyNoisyRoomRaisesTheThreshold() {
        val detector = UtteranceDetector()
        // A loud fan: well above the absolute minimum, but constant, so it becomes the floor.
        val out = detector.run(ms(4_000) { noise(1_500) })
        assertTrue(out.isEmpty())
        assertNotNull(detector.run(ms(800) { voice(12_000.0) } + ms(1_000) { noise(1_500) }).singleOrNull())
    }

    @Test
    fun longSpeechIsCutAtTheMaximum() {
        val detector = UtteranceDetector(maxUtteranceMs = 3_000)
        val out = detector.run(ms(500) { noise(60) } + ms(7_000) { voice() })
        assertEquals(2, out.size)
        assertTrue(out.all { it.size <= 3_300 * 16 })
        assertNotNull(detector.flush())
    }

    @Test
    fun twoSentencesAreSeparated() {
        val detector = UtteranceDetector()
        val out = detector.run(ms(500) { noise(60) } + ms(900) { voice() } + ms(1_000) { noise(60) } + ms(900) { voice() } + ms(1_000) { noise(60) })
        assertEquals(2, out.size)
    }

    @Test
    fun wavHeaderDescribes16kMono16Bit() {
        val wav = Wav.encode(ShortArray(10) { (it * 1000).toShort() })
        assertEquals("RIFF", wav.decodeToString(0, 4))
        assertEquals("WAVE", wav.decodeToString(8, 12))
        assertEquals("data", wav.decodeToString(36, 40))
        fun int(at: Int) = (0 until 4).sumOf { (wav[at + it].toInt() and 0xFF) shl (8 * it) }
        assertEquals(16_000, int(24))
        assertEquals(20, int(40))
        assertEquals(64, wav.size)
        // Sample 1 = 1000 = 0x03E8, little endian.
        assertEquals(0xE8.toByte(), wav[46])
        assertEquals(0x03.toByte(), wav[47])
    }
}

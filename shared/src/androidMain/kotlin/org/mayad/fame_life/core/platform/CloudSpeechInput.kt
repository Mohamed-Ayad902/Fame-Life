package org.mayad.fame_life.core.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mayad.fame_life.domain.ai.SpeechEvent
import org.mayad.fame_life.domain.ai.SpeechInput
import org.mayad.fame_life.domain.ai.SpeechStatus
import org.mayad.fame_life.domain.ai.UtteranceDetector
import org.mayad.fame_life.domain.simulation.AudienceLanguage

/**
 * Speech recognition through the backend's Whisper route, which detects Arabic or English by itself. The mic is read
 * here, cut into sentences by [UtteranceDetector], and each sentence is uploaded once. Audio is kept in memory only
 * until it is sent and is never written to disk.
 */
internal class CloudSpeechInput(
    private val context: Context,
    private val transcribe: suspend (ShortArray) -> String,
) : SpeechInput {

    // The language is detected per sentence, so the room's setting doesn't constrain recognition.
    override fun listen(language: AudienceLanguage): Flow<SpeechEvent> = channelFlow {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            send(SpeechEvent.Status(SpeechStatus.PERMISSION_DENIED))
            return@channelFlow
        }
        send(SpeechEvent.Status(SpeechStatus.STARTING))
        val record = openRecorder() ?: run {
            send(SpeechEvent.Status(SpeechStatus.UNAVAILABLE))
            return@channelFlow
        }

        // Sentences wait here while earlier ones upload; if the network stalls, the oldest are dropped.
        val utterances = Channel<ShortArray>(capacity = 3, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        launch {
            for (samples in utterances) {
                try {
                    val text = transcribe(samples)
                    if (text.isNotEmpty()) send(SpeechEvent.Transcript(text, isFinal = true))
                    send(SpeechEvent.Status(SpeechStatus.LISTENING))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("Pulse: transcription failed (${e::class.simpleName}: ${e.message})")
                    send(SpeechEvent.Status(SpeechStatus.ERROR))
                }
            }
        }

        withContext(Dispatchers.IO) {
            val detector = UtteranceDetector()
            val frame = ShortArray(UtteranceDetector.FRAME_SAMPLES)
            try {
                record.startRecording()
                send(SpeechEvent.Status(SpeechStatus.LISTENING))
                while (isActive) {
                    if (!readFrame(record, frame)) {
                        send(SpeechEvent.Status(SpeechStatus.UNAVAILABLE))
                        break
                    }
                    detector.accept(frame)?.let { utterances.trySend(it) }
                }
            } catch (e: SecurityException) {
                send(SpeechEvent.Status(SpeechStatus.PERMISSION_DENIED))
            } finally {
                runCatching { record.stop() }
                record.release()
                utterances.close()
            }
        }
    }

    @SuppressLint("MissingPermission") // Checked at the start of listen().
    private fun openRecorder(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(UtteranceDetector.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) return null
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                UtteranceDetector.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, UtteranceDetector.FRAME_SAMPLES * 2 * 8),
            )
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return null
        }
        return record
    }

    /** Fills [frame] completely; false when the recorder fails. */
    private fun readFrame(record: AudioRecord, frame: ShortArray): Boolean {
        var offset = 0
        while (offset < frame.size) {
            val read = record.read(frame, offset, frame.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }
}

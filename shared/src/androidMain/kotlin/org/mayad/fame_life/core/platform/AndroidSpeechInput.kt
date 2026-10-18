package org.mayad.fame_life.core.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.mayad.fame_life.domain.ai.SpeechEvent
import org.mayad.fame_life.domain.ai.SpeechInput
import org.mayad.fame_life.domain.ai.SpeechStatus
import org.mayad.fame_life.domain.simulation.AudienceLanguage
import java.util.Locale

/**
 * Android speech recognition. The platform recognizer stops after each phrase, so this restarts it until the
 * flow is cancelled. Audio goes to the system recognizer only; the app receives text and never keeps audio.
 */
internal class AndroidSpeechInput(private val context: Context) : SpeechInput {

    override fun listen(language: AudienceLanguage): Flow<SpeechEvent> = callbackFlow {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            trySend(SpeechEvent.Status(SpeechStatus.PERMISSION_DENIED))
            close()
            awaitClose()
            return@callbackFlow
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechEvent.Status(SpeechStatus.UNAVAILABLE))
            close()
            awaitClose()
            return@callbackFlow
        }

        val handler = Handler(Looper.getMainLooper())
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag(language))
        }
        var active = true
        var consecutiveErrors = 0

        fun restart(delayMs: Long) {
            handler.postDelayed({ if (active) recognizer.startListening(intent) }, delayMs)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Status(SpeechStatus.LISTENING))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                bestText(partialResults)?.let { trySend(SpeechEvent.Transcript(it, isFinal = false)) }
            }

            override fun onResults(results: Bundle?) {
                consecutiveErrors = 0
                bestText(results)?.let { trySend(SpeechEvent.Transcript(it, isFinal = true)) }
                restart(150)
            }

            override fun onError(error: Int) {
                when (error) {
                    // Silence or nothing recognisable: keep listening quietly.
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restart(150)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> restart(600)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        trySend(SpeechEvent.Status(SpeechStatus.PERMISSION_DENIED))
                        close()
                    }
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                        trySend(SpeechEvent.Status(SpeechStatus.UNAVAILABLE))
                        close()
                    }
                    else -> {
                        consecutiveErrors++
                        if (consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
                            trySend(SpeechEvent.Status(SpeechStatus.UNAVAILABLE))
                            close()
                        } else {
                            trySend(SpeechEvent.Status(SpeechStatus.ERROR))
                            restart(500L * consecutiveErrors)
                        }
                    }
                }
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        trySend(SpeechEvent.Status(SpeechStatus.STARTING))
        recognizer.startListening(intent)

        awaitClose {
            active = false
            handler.removeCallbacksAndMessages(null)
            recognizer.cancel()
            recognizer.destroy()
        }
    }.flowOn(Dispatchers.Main) // SpeechRecognizer must be created and used on the main thread.

    private fun bestText(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun languageTag(language: AudienceLanguage): String = when (language) {
        AudienceLanguage.ENGLISH -> "en-US"
        AudienceLanguage.ARABIC -> "ar"
        // Mixed rooms follow the phone's language, which is what a code-switching speaker most likely set.
        AudienceLanguage.MIXED -> Locale.getDefault().toLanguageTag()
    }

    private companion object {
        const val MAX_CONSECUTIVE_ERRORS = 5
    }
}

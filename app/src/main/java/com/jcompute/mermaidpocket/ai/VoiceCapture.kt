package com.jcompute.mermaidpocket.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceCapture(
    context: Context,
    private val onListening: () -> Unit,
    private val onPartial: (String) -> Unit,
    private val onTranscript: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var finished = false

    val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(app)

    fun start() {
        main.post {
            if (!available) {
                onError("Speech recognition is not available on this device.")
                return@post
            }
            releaseLocked()
            finished = false
            val rec = SpeechRecognizer.createSpeechRecognizer(app)
            rec.setRecognitionListener(listener)
            recognizer = rec
            rec.startListening(intent())
        }
    }

    fun cancel() {
        main.post { releaseLocked() }
    }

    fun destroy() {
        main.post { releaseLocked() }
    }

    private fun intent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a diagram change")
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onListening()
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (finished) return
            finished = true
            releaseLocked()
            onError(message(error))
        }

        override fun onResults(results: Bundle?) {
            if (finished) return
            finished = true
            val text = firstResult(results)
            releaseLocked()
            if (text.isNullOrBlank()) {
                onError("Could not hear that. Try again.")
            } else {
                onTranscript(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults)
            if (!text.isNullOrBlank()) onPartial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun firstResult(bundle: Bundle?): String? {
        val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        return list?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun releaseLocked() {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    companion object {
        fun message(error: Int): String {
            return when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Microphone error."
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition stopped."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed for voice edit"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Needs network + OpenRouter key"
                SpeechRecognizer.ERROR_NO_MATCH -> "Could not hear that. Try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy."
                SpeechRecognizer.ERROR_SERVER -> "Speech recognition failed."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Try again."
                else -> "Speech recognition failed."
            }
        }
    }
}

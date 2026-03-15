package com.deepanjanxyz.notepad.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Fully offline voice input using Android's built-in SpeechRecognizer.
 * EXTRA_PREFER_OFFLINE = true ensures it uses the on-device model.
 * No INTERNET permission required.
 */
class VoiceInputManager(private val context: Context) {

    interface Listener {
        fun onResult(text: String)
        fun onError(message: String)
        fun onReadyForSpeech()
        fun onEndOfSpeech()
    }

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(listener: Listener) {
        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = listener.onReadyForSpeech()
                override fun onEndOfSpeech()                   = listener.onEndOfSpeech()
                override fun onBeginningOfSpeech()             = Unit
                override fun onRmsChanged(rmsdB: Float)        = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onPartialResults(partial: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) listener.onResult(matches[0])
                    else listener.onError("No result")
                }

                override fun onError(error: Int) {
                    listener.onError(errorMessage(error))
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)   // ← key: force offline
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            startListening(intent)
        }
    }

    fun stop() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH           -> "No speech match found"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT     -> "Speech timeout"
        SpeechRecognizer.ERROR_AUDIO              -> "Audio recording error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY    -> "Recognizer busy"
        else                                       -> "Recognition error ($code)"
    }
}

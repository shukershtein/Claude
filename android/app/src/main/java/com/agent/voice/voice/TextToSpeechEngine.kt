package com.agent.voice.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class TextToSpeechEngine(context: Context) {

    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.forLanguageTag("he-IL")
    }

    /**
     * Speaks the text and suspends until done. If the engine isn't ready yet, returns immediately.
     */
    suspend fun speak(text: String) = suspendCancellableCoroutine<Unit> { cont ->
        if (!ready) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val utteranceId = "utt-${System.currentTimeMillis()}"

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (cont.isActive) cont.resume(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (cont.isActive) cont.resume(Unit)
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                if (cont.isActive) cont.resume(Unit)
            }
        })

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        cont.invokeOnCancellation {
            tts.stop()
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

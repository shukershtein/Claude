package com.agent.voice.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agent.voice.net.BackendClient
import com.agent.voice.voice.SpeechToText
import com.agent.voice.voice.TextToSpeechEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AgentPhase { Idle, Listening, Thinking, Speaking, Error }

data class TranscriptLine(val role: String, val text: String)

data class UiState(
    val phase: AgentPhase = AgentPhase.Idle,
    val transcript: List<TranscriptLine> = emptyList(),
    val errorMessage: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val stt = SpeechToText(app)
    private val tts = TextToSpeechEngine(app)
    private val backend = BackendClient()
    private val conversationId = UUID.randomUUID().toString()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var listenJob: Job? = null

    fun startListening() {
        if (_state.value.phase != AgentPhase.Idle) return
        _state.update { it.copy(phase = AgentPhase.Listening, errorMessage = null) }

        listenJob = viewModelScope.launch {
            val transcript = stt.listen("he-IL")
            if (transcript.isNullOrBlank()) {
                _state.update { it.copy(phase = AgentPhase.Idle) }
                return@launch
            }

            _state.update {
                it.copy(
                    phase = AgentPhase.Thinking,
                    transcript = it.transcript + TranscriptLine("user", transcript),
                )
            }

            val reply = runCatching { backend.chat(conversationId, transcript) }
                .getOrElse { err ->
                    _state.update {
                        it.copy(
                            phase = AgentPhase.Error,
                            errorMessage = err.message ?: "Backend error",
                        )
                    }
                    return@launch
                }

            _state.update {
                it.copy(
                    phase = AgentPhase.Speaking,
                    transcript = it.transcript + TranscriptLine("assistant", reply),
                )
            }

            tts.speak(reply)
            _state.update { it.copy(phase = AgentPhase.Idle) }
        }
    }

    fun stopListening() {
        if (_state.value.phase == AgentPhase.Listening) {
            stt.stop()
        }
    }

    override fun onCleared() {
        listenJob?.cancel()
        stt.cancel()
        tts.shutdown()
        super.onCleared()
    }
}

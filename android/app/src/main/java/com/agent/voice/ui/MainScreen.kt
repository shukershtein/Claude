package com.agent.voice.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.agent.voice.R

@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            TranscriptList(state.transcript, modifier = Modifier.weight(1f))

            StatusLabel(state)

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasMicPermission) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                } else {
                    PushToTalkButton(
                        phase = state.phase,
                        onPress = { vm.startListening() },
                        onRelease = { vm.stopListening() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptList(lines: List<TranscriptLine>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lines) { line ->
            val isUser = line.role == "user"
            Text(
                text = line.text,
                fontSize = 18.sp,
                textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                color = if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusLabel(state: UiState) {
    val text = when (state.phase) {
        AgentPhase.Idle -> ""
        AgentPhase.Listening -> stringResource(R.string.listening)
        AgentPhase.Thinking -> stringResource(R.string.thinking)
        AgentPhase.Speaking -> stringResource(R.string.speaking)
        AgentPhase.Error -> state.errorMessage ?: "Error"
    }
    Text(
        text = text,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PushToTalkButton(
    phase: AgentPhase,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    val color = when (phase) {
        AgentPhase.Listening -> Color(0xFFE53935)
        AgentPhase.Thinking, AgentPhase.Speaking -> Color(0xFFFFA726)
        AgentPhase.Error -> Color(0xFF757575)
        AgentPhase.Idle -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(color)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) {
                            onPress()
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) {
                                    onRelease()
                                    break
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.hold_to_speak),
            color = Color.White,
            fontSize = 16.sp,
        )
    }
}

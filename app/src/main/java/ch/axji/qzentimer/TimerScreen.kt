package ch.axji.qzentimer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hauptbildschirm: Moduswahl, Phasenanzeige, Zeitregler, Tabata-Einstellungen sowie Start/Pause und Zurücksetzen.
 * Die Oberfläche hält selbst keinen Zustand, alle Änderungen gehen über das [TimerViewModel].
 */
@Composable
fun TimerScreen(viewModel: TimerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    // Bildschirm anlassen, solange der Timer läuft.
    val view = LocalView.current
    DisposableEffect(state.isRunning) {
        view.keepScreenOn = state.isRunning
        onDispose { view.keepScreenOn = false }
    }

    // Kurznamen für die Felder des aktuellen Zustands, damit die Oberfläche unten lesbar bleibt
    val timerMode = state.mode
    val isRunning = state.isRunning
    val currentTimeSeconds = state.currentSeconds
    val totalTimeSeconds = state.totalSeconds
    val pomodoroCycle = state.pomodoroCycle
    val isPomodoroWorkPhase = state.isPomodoroWork
    val tabataCycle = state.tabataCycle
    val tabataPhase = state.tabataPhase
    val tabataWorkSeconds = state.tabataWorkSeconds
    val tabataBreakSeconds = state.tabataBreakSeconds

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.timer_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Auswahl des Modus (Manuell, Pomodoro, Tabata); während der Timer läuft, ist sie gesperrt
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.Center
        ) {
            TimerMode.entries.forEach { mode ->
                val selected = timerMode == mode
                TextButton(
                    onClick = { viewModel.selectMode(mode) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        when (mode) {
                            TimerMode.MANUAL -> stringResource(R.string.mode_manual)
                            TimerMode.POMODORO -> stringResource(R.string.mode_pomodoro)
                            TimerMode.TABATA -> stringResource(R.string.mode_tabata)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Anzeige der Phase, z. B. "Arbeit (Zyklus 2/4)"
        if (timerMode != TimerMode.MANUAL) {
            val phaseText = when (timerMode) {
                TimerMode.POMODORO -> {
                    val label = if (isPomodoroWorkPhase) stringResource(R.string.work) 
                               else if (pomodoroCycle == 4) stringResource(R.string.long_break) 
                               else stringResource(R.string.break_label)
                    "$label (${stringResource(R.string.cycle)} $pomodoroCycle/4)"
                }
                TimerMode.TABATA -> {
                    val label = when (tabataPhase) {
                        TabataPhase.PREPARE -> stringResource(R.string.prepare)
                        TabataPhase.WORK -> stringResource(R.string.work)
                        TabataPhase.BREAK -> stringResource(R.string.break_label)
                    }
                    if (tabataPhase == TabataPhase.PREPARE) label else "$label (${stringResource(R.string.cycle)} $tabataCycle/8)"
                }
                else -> ""
            }
            Text(
                text = phaseText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .padding(16.dp)
        ) {
            CircularTimerPicker(
                currentTimeSeconds = currentTimeSeconds,
                totalTimeSeconds = if (timerMode == TimerMode.MANUAL) 0 else totalTimeSeconds,
                isTimerRunning = isRunning,
                onTimeChange = viewModel::setManualTime,
                modifier = Modifier.fillMaxSize()
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .pointerInput(currentTimeSeconds) {
                        detectTapGestures { viewModel.toggleRunning() }
                    }
            ) {
                Text(
                    text = formatTime(currentTimeSeconds),
                    fontSize = if (currentTimeSeconds >= 3600) 48.sp else 64.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Dauer von Arbeit und Pause bei Tabata, nur änderbar, solange der Timer steht
        if (timerMode == TimerMode.TABATA && !isRunning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                TabataSettingItem(
                    label = stringResource(R.string.work),
                    value = tabataWorkSeconds,
                    onValueChange = { viewModel.changeTabataWork(it - tabataWorkSeconds) }
                )
                TabataSettingItem(
                    label = stringResource(R.string.break_label),
                    value = tabataBreakSeconds,
                    onValueChange = { viewModel.changeTabataBreak(it - tabataBreakSeconds) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = viewModel::toggleRunning,
                enabled = currentTimeSeconds > 0,
                modifier = Modifier.width(150.dp)
            ) {
                Text(if (isRunning) stringResource(R.string.pause) else stringResource(R.string.start))
            }

            OutlinedButton(
                onClick = viewModel::reset,
                modifier = Modifier.width(150.dp)
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

/** Einstellung mit Minus- und Plus-Knopf für eine Dauer in Sekunden. */
@Composable
fun TabataSettingItem(label: String, value: Long, onValueChange: (Long) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(value.toString() + "s", fontWeight = FontWeight.Bold)
            IconButton(onClick = { onValueChange(value + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

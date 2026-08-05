package com.example.qzentimer

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qzentimer.ui.theme.QZenTimerTheme
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

enum class TimerMode {
    MANUAL, POMODORO, TABATA
}

enum class TabataPhase {
    PREPARE, WORK, BREAK
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QZenTimerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    var timerMode by remember { mutableStateOf(TimerMode.MANUAL) }
    
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    var totalTimeSeconds by remember { mutableLongStateOf(0L) }
    var currentTimeSeconds by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }

    // Pomodoro State
    var pomodoroCycle by remember { mutableIntStateOf(1) }
    var isPomodoroWorkPhase by remember { mutableStateOf(true) }

    // Tabata State
    var tabataCycle by remember { mutableIntStateOf(1) }
    var tabataPhase by remember { mutableStateOf(TabataPhase.PREPARE) }
    var tabataWorkSeconds by remember { mutableLongStateOf(20L) }
    var tabataBreakSeconds by remember { mutableLongStateOf(10L) }

    fun startPomodoroPhase() {
        currentTimeSeconds = if (isPomodoroWorkPhase) 25 * 60L else {
            if (pomodoroCycle == 4) 20 * 60L else 5 * 60L
        }
        totalTimeSeconds = currentTimeSeconds
    }

    fun startTabataPhase() {
        currentTimeSeconds = when (tabataPhase) {
            TabataPhase.PREPARE -> 10L
            TabataPhase.WORK -> tabataWorkSeconds
            TabataPhase.BREAK -> tabataBreakSeconds
        }
        totalTimeSeconds = currentTimeSeconds
    }

    LaunchedEffect(key1 = isRunning, key2 = currentTimeSeconds) {
        if (isRunning && currentTimeSeconds > 0) {
            if (timerMode == TimerMode.TABATA && currentTimeSeconds <= 3) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
            }
            delay(1.seconds)
            currentTimeSeconds -= 1
            if (timerMode == TimerMode.TABATA && currentTimeSeconds == 0L) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            }
        } else if (isRunning && currentTimeSeconds == 0L) {
            when (timerMode) {
                TimerMode.MANUAL -> isRunning = false
                TimerMode.POMODORO -> {
                    if (isPomodoroWorkPhase) {
                        isPomodoroWorkPhase = false
                    } else {
                        isPomodoroWorkPhase = true
                        pomodoroCycle = if (pomodoroCycle >= 4) 1 else pomodoroCycle + 1
                    }
                    startPomodoroPhase()
                }
                TimerMode.TABATA -> {
                    when (tabataPhase) {
                        TabataPhase.PREPARE -> {
                            tabataPhase = TabataPhase.WORK
                            tabataCycle = 1
                        }
                        TabataPhase.WORK -> {
                            tabataPhase = TabataPhase.BREAK
                        }
                        TabataPhase.BREAK -> {
                            if (tabataCycle >= 8) {
                                isRunning = false
                                tabataPhase = TabataPhase.PREPARE
                            } else {
                                tabataCycle++
                                tabataPhase = TabataPhase.WORK
                            }
                        }
                    }
                    if (isRunning) startTabataPhase()
                }
            }
        }
    }

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

        // Mode Selector
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
                    onClick = {
                        if (!isRunning) {
                            timerMode = mode
                            isRunning = false
                            currentTimeSeconds = 0
                            totalTimeSeconds = 0
                            if (mode == TimerMode.POMODORO) {
                                pomodoroCycle = 1
                                isPomodoroWorkPhase = true
                                startPomodoroPhase()
                            } else if (mode == TimerMode.TABATA) {
                                tabataCycle = 1
                                tabataPhase = TabataPhase.PREPARE
                                startTabataPhase()
                            }
                        }
                    },
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

        // Phase Information
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
                onTimeChange = { newSeconds ->
                    if (!isRunning && timerMode == TimerMode.MANUAL) {
                        currentTimeSeconds = newSeconds
                        totalTimeSeconds = newSeconds
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .pointerInput(currentTimeSeconds) {
                        detectTapGestures {
                            if (currentTimeSeconds > 0) {
                                isRunning = !isRunning
                            }
                        }
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

        // Tabata Settings
        if (timerMode == TimerMode.TABATA && !isRunning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                TabataSettingItem(
                    label = stringResource(R.string.work),
                    value = tabataWorkSeconds,
                    onValueChange = { 
                        tabataWorkSeconds = it.coerceIn(1, 3600)
                        if (tabataPhase == TabataPhase.WORK) {
                            currentTimeSeconds = tabataWorkSeconds
                            totalTimeSeconds = tabataWorkSeconds
                        }
                    }
                )
                TabataSettingItem(
                    label = stringResource(R.string.break_label),
                    value = tabataBreakSeconds,
                    onValueChange = { 
                        tabataBreakSeconds = it.coerceIn(1, 3600)
                        if (tabataPhase == TabataPhase.BREAK) {
                            currentTimeSeconds = tabataBreakSeconds
                            totalTimeSeconds = tabataBreakSeconds
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (currentTimeSeconds > 0) {
                        isRunning = !isRunning
                    }
                },
                enabled = currentTimeSeconds > 0,
                modifier = Modifier.width(150.dp)
            ) {
                Text(if (isRunning) stringResource(R.string.pause) else stringResource(R.string.start))
            }

            OutlinedButton(
                onClick = {
                    isRunning = false
                    when (timerMode) {
                        TimerMode.MANUAL -> {
                            currentTimeSeconds = 0
                            totalTimeSeconds = 0
                        }
                        TimerMode.POMODORO -> {
                            pomodoroCycle = 1
                            isPomodoroWorkPhase = true
                            startPomodoroPhase()
                        }
                        TimerMode.TABATA -> {
                            tabataCycle = 1
                            tabataPhase = TabataPhase.PREPARE
                            startTabataPhase()
                        }
                    }
                },
                modifier = Modifier.width(150.dp)
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

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

@Composable
fun CircularTimerPicker(
    currentTimeSeconds: Long,
    totalTimeSeconds: Long,
    isTimerRunning: Boolean,
    onTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokeWidth = 20.dp
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    
    // Determine colors based on time or phase
    val (backgroundColor, progressColor) = if (totalTimeSeconds > 0) {
        // For Pomodoro/Tabata we use a simpler color scheme or fixed colors
        trackColor to MaterialTheme.colorScheme.primary
    } else {
        // Manual mode dynamic colors
        when {
            currentTimeSeconds > 5 * 3600 -> Color.Blue to Color.Blue
            currentTimeSeconds > 4 * 3600 -> Color.Green to Color.Blue
            currentTimeSeconds > 3 * 3600 -> Color.Yellow to Color.Green
            currentTimeSeconds > 2 * 3600 -> Color(0xFFFFA500) to Color.Yellow
            currentTimeSeconds > 1 * 3600 -> Color.Red to Color(0xFFFFA500)
            else -> trackColor to Color.Red
        }
    }

    val currentSecondsState = rememberUpdatedState(currentTimeSeconds)
    val onTimeChangeState = rememberUpdatedState(onTimeChange)

    var lastAngle by remember { mutableStateOf<Double?>(null) }
    var dragAccumulator by remember { mutableDoubleStateOf(0.0) }
    
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Canvas(
        modifier = modifier
            .pointerInput(isTimerRunning) {
                if (!isTimerRunning) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val startAngle = Math.toDegrees(atan2((offset.y - center.y).toDouble(), (offset.x - center.x).toDouble())) + 90
                            lastAngle = if (startAngle < 0) startAngle + 360 else startAngle
                            dragAccumulator = 0.0
                        },
                        onDragEnd = { lastAngle = null },
                        onDragCancel = { lastAngle = null },
                        onDrag = { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchPoint = change.position
                            val dist = sqrt((touchPoint.x - center.x).pow(2) + (touchPoint.y - center.y).pow(2))
                            
                            if (dist > 100f) {
                                var currentAngle = Math.toDegrees(atan2((touchPoint.y - center.y).toDouble(), (touchPoint.x - center.x).toDouble())) + 90
                                if (currentAngle < 0) currentAngle += 360
                                
                                if (lastAngle != null) {
                                    var delta = currentAngle - lastAngle!!
                                    if (delta > 180) delta -= 360
                                    if (delta < -180) delta += 360
                                    
                                    dragAccumulator += delta
                                    
                                    if (abs(dragAccumulator) >= 6.0) {
                                        val minutesToChange = (dragAccumulator / 6.0).toInt()
                                        val newTotal = (currentSecondsState.value + minutesToChange * 60).coerceIn(0, 24 * 3600)
                                        onTimeChangeState.value(newTotal)
                                        dragAccumulator -= minutesToChange * 6.0
                                    }
                                }
                                lastAngle = currentAngle
                            }
                        }
                    )
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2) - strokeWidth.toPx()

        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth.toPx())
        )

        val sweepAngle = if (totalTimeSeconds > 0) {
            (currentTimeSeconds.toFloat() / totalTimeSeconds.toFloat()) * 360f
        } else {
            val minutesInHour = if (currentTimeSeconds > 0 && currentTimeSeconds % 3600 == 0L) 3600 else currentTimeSeconds % 3600
            (minutesInHour / 3600f) * 360f
        }

        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        
        for (i in 0 until 12) {
            val tickAngle = i * 30f - 90f
            val tickAngleRad = Math.toRadians(tickAngle.toDouble())
            
            val innerTickRadius = radius - (strokeWidth.toPx() / 2) - 4.dp.toPx()
            val outerTickRadius = radius + (strokeWidth.toPx() / 2) + 22.dp.toPx()
            
            val startTick = Offset(
                center.x + innerTickRadius * cos(tickAngleRad).toFloat(),
                center.y + innerTickRadius * sin(tickAngleRad).toFloat()
            )
            val endTick = Offset(
                center.x + outerTickRadius * cos(tickAngleRad).toFloat(),
                center.y + outerTickRadius * sin(tickAngleRad).toFloat()
            )
            drawLine(
                color = tickColor,
                start = startTick,
                end = endTick,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

package ch.axji.qzentimer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Runder Zeitregler und Fortschrittsanzeige.
 *
 * Im manuellen Modus (`totalTimeSeconds == 0`) stellt man die Zeit durch Drehen mit dem Finger ein: Jede 6 Grad Drehung
 * entsprechen einer Minute, ein voller Kreis also einer Stunde. Die Farbe wechselt mit jeder weiteren Stunde.
 * In den anderen Modi zeigt der Bogen den Anteil der verbleibenden Zeit und der Regler ist gesperrt.
 *
 * @param currentTimeSeconds Verbleibende Sekunden
 * @param totalTimeSeconds Dauer der aktuellen Phase, 0 im manuellen Modus
 * @param isTimerRunning Während der Timer läuft, lässt sich die Zeit nicht ändern
 * @param onTimeChange Wird mit der neuen Zeit in Sekunden aufgerufen
 */
@Composable
fun CircularTimerPicker(
    currentTimeSeconds: Long,
    totalTimeSeconds: Long,
    isTimerRunning: Boolean,
    onTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Breite des Rings und Farbe der Spur, auf der der Fortschritt läuft
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

    // rememberUpdatedState: Die Geste läuft weiter, sieht aber immer die neuesten Werte
    val currentSecondsState = rememberUpdatedState(currentTimeSeconds)
    val onTimeChangeState = rememberUpdatedState(onTimeChange)

    var lastAngle by remember { mutableStateOf<Double?>(null) }
    var dragAccumulator by remember { mutableDoubleStateOf(0.0) }
    
    // Farbe der 12 Striche rund um den Ring
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
                            
                            // Berührungen nahe der Mitte ignorieren, dort schwankt der Winkel stark
                            if (dist > 100f) {
                                var currentAngle = Math.toDegrees(atan2((touchPoint.y - center.y).toDouble(), (touchPoint.x - center.x).toDouble())) + 90
                                if (currentAngle < 0) currentAngle += 360
                                
                                if (lastAngle != null) {
                                    var delta = currentAngle - lastAngle!!
                                    // Sprung über die 0-Grad-Linie ausgleichen (z. B. 359 auf 1 Grad)
                                    if (delta > 180) delta -= 360
                                    if (delta < -180) delta += 360
                                    
                                    dragAccumulator += delta
                                    
                                    // Pro 6 Grad Drehung eine Minute dazu oder weg (maximal 24 Stunden)
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

        // Länge des Bogens: Anteil der Restzeit (Pomodoro/Tabata) oder Minuten der aktuellen Stunde (manuell)
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
        
        // 12 Striche wie auf einem Zifferblatt, alle 30 Grad
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

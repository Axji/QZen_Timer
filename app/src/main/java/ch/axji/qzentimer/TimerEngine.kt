package ch.axji.qzentimer

enum class TimerMode {
    MANUAL, POMODORO, TABATA
}

enum class TabataPhase {
    PREPARE, WORK, BREAK
}

/** Ton, den die Oberfläche zu einem Takt abspielen soll. */
enum class TimerSound {
    COUNTDOWN_PIP, PHASE_END
}

data class TimerState(
    val mode: TimerMode = TimerMode.MANUAL,
    val totalSeconds: Long = 0,
    val currentSeconds: Long = 0,
    val isRunning: Boolean = false,
    val pomodoroCycle: Int = 1,
    val isPomodoroWork: Boolean = true,
    val tabataCycle: Int = 1,
    val tabataPhase: TabataPhase = TabataPhase.PREPARE,
    val tabataWorkSeconds: Long = DEFAULT_TABATA_WORK,
    val tabataBreakSeconds: Long = DEFAULT_TABATA_BREAK,
) {
    companion object {
        const val POMODORO_CYCLES = 4
        const val TABATA_CYCLES = 8
        const val POMODORO_WORK = 25 * 60L
        const val POMODORO_BREAK = 5 * 60L
        const val POMODORO_LONG_BREAK = 20 * 60L
        const val TABATA_PREPARE = 10L
        const val DEFAULT_TABATA_WORK = 20L
        const val DEFAULT_TABATA_BREAK = 10L
        const val MAX_MANUAL_SECONDS = 24 * 3600L
        const val MAX_TABATA_SECONDS = 3600L
    }
}

data class TickResult(val state: TimerState, val sound: TimerSound? = null)

/** Reine Timer-Logik ohne Android-Abhängigkeiten, damit sie sich testen lässt. */
object TimerEngine {

    fun selectMode(state: TimerState, mode: TimerMode): TimerState =
        if (state.isRunning) state else reset(state.copy(mode = mode))

    fun reset(state: TimerState): TimerState {
        val stopped = state.copy(isRunning = false)
        return when (state.mode) {
            TimerMode.MANUAL -> stopped.copy(currentSeconds = 0, totalSeconds = 0)
            TimerMode.POMODORO -> startPomodoroPhase(stopped.copy(pomodoroCycle = 1, isPomodoroWork = true))
            TimerMode.TABATA -> startTabataPhase(stopped.copy(tabataCycle = 1, tabataPhase = TabataPhase.PREPARE))
        }
    }

    fun toggleRunning(state: TimerState): TimerState =
        if (state.currentSeconds > 0) state.copy(isRunning = !state.isRunning) else state

    fun setManualTime(state: TimerState, seconds: Long): TimerState {
        if (state.isRunning || state.mode != TimerMode.MANUAL) return state
        val value = seconds.coerceIn(0, TimerState.MAX_MANUAL_SECONDS)
        return state.copy(currentSeconds = value, totalSeconds = value)
    }

    fun setTabataWork(state: TimerState, seconds: Long): TimerState {
        val value = seconds.coerceIn(1, TimerState.MAX_TABATA_SECONDS)
        val updated = state.copy(tabataWorkSeconds = value)
        return if (state.tabataPhase == TabataPhase.WORK) updated.copy(currentSeconds = value, totalSeconds = value) else updated
    }

    fun setTabataBreak(state: TimerState, seconds: Long): TimerState {
        val value = seconds.coerceIn(1, TimerState.MAX_TABATA_SECONDS)
        val updated = state.copy(tabataBreakSeconds = value)
        return if (state.tabataPhase == TabataPhase.BREAK) updated.copy(currentSeconds = value, totalSeconds = value) else updated
    }

    /** Eine Sekunde weiter. Wird nur aufgerufen, solange der Timer läuft. */
    fun tick(state: TimerState): TickResult {
        if (!state.isRunning || state.currentSeconds <= 0) return TickResult(state)

        val isTabata = state.mode == TimerMode.TABATA
        var sound: TimerSound? = null
        // Kurzer Ton in den letzten drei Sekunden einer Tabata-Phase.
        if (isTabata && state.currentSeconds <= 3) sound = TimerSound.COUNTDOWN_PIP

        var next = state.copy(currentSeconds = state.currentSeconds - 1)
        if (next.currentSeconds == 0L) {
            if (isTabata) sound = TimerSound.PHASE_END
            next = nextPhase(next)
        }
        return TickResult(next, sound)
    }

    private fun nextPhase(state: TimerState): TimerState = when (state.mode) {
        TimerMode.MANUAL -> state.copy(isRunning = false)
        TimerMode.POMODORO -> {
            if (state.isPomodoroWork) {
                startPomodoroPhase(state.copy(isPomodoroWork = false))
            } else {
                val cycle = if (state.pomodoroCycle >= TimerState.POMODORO_CYCLES) 1 else state.pomodoroCycle + 1
                startPomodoroPhase(state.copy(isPomodoroWork = true, pomodoroCycle = cycle))
            }
        }
        TimerMode.TABATA -> when (state.tabataPhase) {
            TabataPhase.PREPARE -> startTabataPhase(state.copy(tabataPhase = TabataPhase.WORK, tabataCycle = 1))
            TabataPhase.WORK -> startTabataPhase(state.copy(tabataPhase = TabataPhase.BREAK))
            TabataPhase.BREAK ->
                if (state.tabataCycle >= TimerState.TABATA_CYCLES) {
                    startTabataPhase(state.copy(isRunning = false, tabataPhase = TabataPhase.PREPARE))
                        .copy(isRunning = false)
                } else {
                    startTabataPhase(state.copy(tabataCycle = state.tabataCycle + 1, tabataPhase = TabataPhase.WORK))
                }
        }
    }

    private fun startPomodoroPhase(state: TimerState): TimerState {
        val seconds = when {
            state.isPomodoroWork -> TimerState.POMODORO_WORK
            state.pomodoroCycle == TimerState.POMODORO_CYCLES -> TimerState.POMODORO_LONG_BREAK
            else -> TimerState.POMODORO_BREAK
        }
        return state.copy(currentSeconds = seconds, totalSeconds = seconds)
    }

    private fun startTabataPhase(state: TimerState): TimerState {
        val seconds = when (state.tabataPhase) {
            TabataPhase.PREPARE -> TimerState.TABATA_PREPARE
            TabataPhase.WORK -> state.tabataWorkSeconds
            TabataPhase.BREAK -> state.tabataBreakSeconds
        }
        return state.copy(currentSeconds = seconds, totalSeconds = seconds)
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

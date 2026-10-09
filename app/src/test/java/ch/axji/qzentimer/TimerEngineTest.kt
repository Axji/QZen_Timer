package ch.axji.qzentimer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private fun running(state: TimerState) = state.copy(isRunning = true)

    @Test
    fun manualTimeIsClampedAndIgnoredWhileRunning() {
        val set = TimerEngine.setManualTime(TimerState(), 10 * 3600 * 10L)
        assertEquals(TimerState.MAX_MANUAL_SECONDS, set.currentSeconds)

        val locked = TimerEngine.setManualTime(running(set), 60)
        assertEquals(TimerState.MAX_MANUAL_SECONDS, locked.currentSeconds)
    }

    @Test
    fun toggleNeedsTimeOnTheClock() {
        assertFalse(TimerEngine.toggleRunning(TimerState()).isRunning)
        val ready = TimerEngine.setManualTime(TimerState(), 60)
        assertTrue(TimerEngine.toggleRunning(ready).isRunning)
    }

    @Test
    fun manualTimerStopsAtZero() {
        var state = running(TimerEngine.setManualTime(TimerState(), 2))
        state = TimerEngine.tick(state).state
        assertEquals(1, state.currentSeconds)
        state = TimerEngine.tick(state).state
        assertEquals(0, state.currentSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun pomodoroRunsThroughFourCyclesWithLongBreak() {
        var state = TimerEngine.selectMode(TimerState(), TimerMode.POMODORO)
        assertEquals(TimerState.POMODORO_WORK, state.currentSeconds)
        state = running(state)

        repeat(3) { cycle ->
            state = state.copy(currentSeconds = 1)
            state = TimerEngine.tick(state).state
            assertFalse(state.isPomodoroWork)
            assertEquals(cycle + 1, state.pomodoroCycle)
            assertEquals(TimerState.POMODORO_BREAK, state.currentSeconds)

            state = TimerEngine.tick(state.copy(currentSeconds = 1)).state
            assertTrue(state.isPomodoroWork)
            assertEquals(cycle + 2, state.pomodoroCycle)
        }

        state = TimerEngine.tick(state.copy(currentSeconds = 1)).state
        assertEquals(TimerState.POMODORO_LONG_BREAK, state.currentSeconds)

        state = TimerEngine.tick(state.copy(currentSeconds = 1)).state
        assertEquals(1, state.pomodoroCycle)
        assertTrue(state.isPomodoroWork)
    }

    @Test
    fun tabataFollowsPrepareWorkBreakAndStopsAfterEightCycles() {
        var state = running(TimerEngine.selectMode(TimerState(), TimerMode.TABATA))
        assertEquals(TimerState.TABATA_PREPARE, state.currentSeconds)

        state = TimerEngine.tick(state.copy(currentSeconds = 1)).state
        assertEquals(TabataPhase.WORK, state.tabataPhase)
        assertEquals(TimerState.DEFAULT_TABATA_WORK, state.currentSeconds)

        state = TimerEngine.tick(state.copy(currentSeconds = 1)).state
        assertEquals(TabataPhase.BREAK, state.tabataPhase)

        state = TimerEngine.tick(state.copy(currentSeconds = 1)).state
        assertEquals(TabataPhase.WORK, state.tabataPhase)
        assertEquals(2, state.tabataCycle)

        state = state.copy(tabataCycle = TimerState.TABATA_CYCLES, tabataPhase = TabataPhase.BREAK, currentSeconds = 1)
        state = TimerEngine.tick(state).state
        assertFalse(state.isRunning)
        assertEquals(TabataPhase.PREPARE, state.tabataPhase)
    }

    @Test
    fun tabataBeepsInLastThreeSecondsAndAtPhaseEnd() {
        val base = running(TimerEngine.selectMode(TimerState(), TimerMode.TABATA)).copy(tabataPhase = TabataPhase.WORK)
        assertNull(TimerEngine.tick(base.copy(currentSeconds = 10)).sound)
        assertEquals(TimerSound.COUNTDOWN_PIP, TimerEngine.tick(base.copy(currentSeconds = 3)).sound)
        assertEquals(TimerSound.PHASE_END, TimerEngine.tick(base.copy(currentSeconds = 1)).sound)
    }

    @Test
    fun otherModesNeverBeep() {
        val pomodoro = running(TimerEngine.selectMode(TimerState(), TimerMode.POMODORO)).copy(currentSeconds = 1)
        assertNull(TimerEngine.tick(pomodoro).sound)
    }

    @Test
    fun tabataSettingsAreClampedAndUpdateCurrentPhase() {
        var state = TimerEngine.selectMode(TimerState(), TimerMode.TABATA).copy(tabataPhase = TabataPhase.WORK)
        state = TimerEngine.setTabataWork(state, 0)
        assertEquals(1, state.tabataWorkSeconds)
        assertEquals(1, state.currentSeconds)

        state = TimerEngine.setTabataBreak(state, 99_999)
        assertEquals(TimerState.MAX_TABATA_SECONDS, state.tabataBreakSeconds)
        assertEquals(1, state.currentSeconds)
    }

    @Test
    fun modeCannotChangeWhileRunning() {
        val state = running(TimerEngine.setManualTime(TimerState(), 30))
        assertEquals(TimerMode.MANUAL, TimerEngine.selectMode(state, TimerMode.TABATA).mode)
    }

    @Test
    fun formatsTime() {
        assertEquals("00:05", formatTime(5))
        assertEquals("25:00", formatTime(25 * 60))
        assertEquals("01:01:01", formatTime(3661))
    }
}

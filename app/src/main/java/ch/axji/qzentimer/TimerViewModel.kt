package ch.axji.qzentimer

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Hält den Timer-Zustand, damit er Drehen und Neuaufbau der Oberfläche übersteht.
 * Solange der Timer läuft, hält [TimerService] den Prozess im Vordergrund am Leben.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var tickJob: Job? = null

    fun selectMode(mode: TimerMode) = change { TimerEngine.selectMode(it, mode) }

    fun reset() = change { TimerEngine.reset(it) }

    fun toggleRunning() = change { TimerEngine.toggleRunning(it) }

    fun setManualTime(seconds: Long) = change { TimerEngine.setManualTime(it, seconds) }

    fun changeTabataWork(delta: Long) = change { TimerEngine.setTabataWork(it, it.tabataWorkSeconds + delta) }

    fun changeTabataBreak(delta: Long) = change { TimerEngine.setTabataBreak(it, it.tabataBreakSeconds + delta) }

    private fun change(transform: (TimerState) -> TimerState) {
        _state.update(transform)
        syncRunning()
    }

    /** Startet oder stoppt Sekundentakt und Vordergrunddienst passend zum Zustand. */
    private fun syncRunning() {
        val running = _state.value.isRunning
        if (running && tickJob?.isActive != true) {
            TimerService.start(getApplication())
            tickJob = viewModelScope.launch {
                while (_state.value.isRunning) {
                    delay(1000)
                    val result = TimerEngine.tick(_state.value)
                    _state.value = result.state
                    result.sound?.let(::play)
                }
                TimerService.stop(getApplication())
            }
        } else if (!running) {
            tickJob?.cancel()
            tickJob = null
            TimerService.stop(getApplication())
        }
    }

    private fun play(sound: TimerSound) = when (sound) {
        TimerSound.COUNTDOWN_PIP -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
        TimerSound.PHASE_END -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
    }

    override fun onCleared() {
        tickJob?.cancel()
        TimerService.stop(getApplication())
        toneGenerator.release()
    }
}

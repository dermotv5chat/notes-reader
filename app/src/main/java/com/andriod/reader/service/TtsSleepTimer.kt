package com.andriod.reader.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SleepTimerMode {
    Off,
    FixedMinutes,
    AfterNoteEnd,
}

sealed class LastSleepTimerPreset {
    data class FixedMinutes(val minutes: Int) : LastSleepTimerPreset()
    data object AfterNoteEnd : LastSleepTimerPreset()

    fun displaySubtitle(): String = when (this) {
        is FixedMinutes -> "${minutes} 分钟"
        AfterNoteEnd -> "本篇结束后关闭"
    }

    companion object {
        const val TYPE_FIXED = "fixed"
        const val TYPE_AFTER_NOTE_END = "after_note_end"
        const val DEFAULT_MINUTES = 30

        fun fromStored(type: String?, minutes: Int): LastSleepTimerPreset = when (type) {
            TYPE_AFTER_NOTE_END -> AfterNoteEnd
            else -> FixedMinutes(minutes.coerceIn(1, 90))
        }

        fun storedType(preset: LastSleepTimerPreset): String = when (preset) {
            is FixedMinutes -> TYPE_FIXED
            AfterNoteEnd -> TYPE_AFTER_NOTE_END
        }

        fun storedMinutes(preset: LastSleepTimerPreset): Int? = when (preset) {
            is FixedMinutes -> preset.minutes.coerceIn(1, 90)
            AfterNoteEnd -> null
        }
    }
}

data class TtsSleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.Off,
    val remainingMs: Long? = null,
    val label: String? = null,
) {
    val isAfterNoteEnd: Boolean get() = mode == SleepTimerMode.AfterNoteEnd
    val isFixedActive: Boolean get() = mode == SleepTimerMode.FixedMinutes
    val isActive: Boolean get() = mode != SleepTimerMode.Off
}

internal class TtsSleepTimer(
    context: Context,
    private val onSessionChanged: () -> Unit,
    private val onExpired: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wakeLock = TtsWakeLock(appContext)
    private var mode = SleepTimerMode.Off
    private var deadlineElapsedRealtime = 0L
    private var pausedRemainingMs: Long? = null
    private var tickJob: Job? = null

    fun setMinutes(minutes: Int, onSaved: (Int) -> Unit) {
        reset(notify = false)
        if (minutes <= 0) {
            onSessionChanged()
            return
        }
        onSaved(minutes)
        mode = SleepTimerMode.FixedMinutes
        deadlineElapsedRealtime = SystemClock.elapsedRealtime() + minutes * 60_000L
        wakeLock.acquire()
        scheduleAlarm()
        startTicking()
        onSessionChanged()
    }

    fun setAfterNoteEnd() {
        reset(notify = false)
        mode = SleepTimerMode.AfterNoteEnd
        onSessionChanged()
    }

    fun cancel() {
        reset(notify = true)
    }

    fun onPlaybackPaused() {
        if (mode != SleepTimerMode.FixedMinutes || pausedRemainingMs != null) return
        val remaining = deadlineElapsedRealtime - SystemClock.elapsedRealtime()
        if (remaining <= 0) return
        pausedRemainingMs = remaining
        tickJob?.cancel()
        tickJob = null
        clearScheduledAlarm()
        wakeLock.release()
        onSessionChanged()
    }

    fun onPlaybackResumed() {
        if (mode != SleepTimerMode.FixedMinutes || pausedRemainingMs == null) return
        val remaining = pausedRemainingMs ?: return
        pausedRemainingMs = null
        deadlineElapsedRealtime = SystemClock.elapsedRealtime() + remaining
        wakeLock.acquire()
        scheduleAlarm()
        startTicking()
        onSessionChanged()
    }

    fun onAlarmFired() {
        if (mode != SleepTimerMode.FixedMinutes) return
        val remaining = deadlineElapsedRealtime - SystemClock.elapsedRealtime()
        if (remaining > 1_000L) return
        reset(notify = false)
        onExpired()
        onSessionChanged()
    }

    fun snapshot(): TtsSleepTimerState {
        return when (mode) {
            SleepTimerMode.Off -> TtsSleepTimerState()
            SleepTimerMode.AfterNoteEnd -> TtsSleepTimerState(
                mode = mode,
                label = "本篇结束后关闭",
            )
            SleepTimerMode.FixedMinutes -> {
                val remaining = pausedRemainingMs
                    ?: (deadlineElapsedRealtime - SystemClock.elapsedRealtime())
                if (remaining <= 0) {
                    TtsSleepTimerState()
                } else {
                    val minutes = (remaining + 59_999) / 60_000
                    TtsSleepTimerState(
                        mode = mode,
                        remainingMs = remaining,
                        label = "${minutes}分钟后关闭",
                    )
                }
            }
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(1_000)
                if (mode != SleepTimerMode.FixedMinutes) break
                val remaining = deadlineElapsedRealtime - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    reset(notify = false)
                    onExpired()
                    onSessionChanged()
                    break
                }
                onSessionChanged()
            }
        }
    }

    private fun scheduleAlarm() {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                deadlineElapsedRealtime,
                alarmPendingIntent(),
            )
        } catch (_: SecurityException) {
            // Fall back to in-process ticking when exact alarms are unavailable.
        }
    }

    private fun clearScheduledAlarm() {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(appContext, TtsSleepTimerReceiver::class.java).apply {
            action = TtsSleepTimerReceiver.ACTION_TIMER_EXPIRED
        }
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun reset(notify: Boolean) {
        tickJob?.cancel()
        tickJob = null
        clearScheduledAlarm()
        wakeLock.release()
        mode = SleepTimerMode.Off
        deadlineElapsedRealtime = 0L
        pausedRemainingMs = null
        if (notify) {
            onSessionChanged()
        }
    }

    companion object {
        private const val REQUEST_CODE = 1002
    }
}

class TtsSleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TIMER_EXPIRED) return
        TtsPlaybackManager.onSleepTimerAlarm()
    }

    companion object {
        const val ACTION_TIMER_EXPIRED = "com.andriod.reader.tts.SLEEP_TIMER_EXPIRED"
    }
}

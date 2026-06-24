package com.andriod.reader.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import com.andriod.reader.data.local.AppDiagnosticLog
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TtsPlaybackService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSessionCompat? = null
    private var foregroundPromoted = false
    private var stopSelfJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        TtsNotificationHelper.ensureChannel(this)
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(mediaSessionCallback)
            setMediaButtonReceiver(mediaButtonPendingIntent())
            isActive = true
        }
        ensureForegroundStarted()
        serviceScope.launch {
            TtsPlaybackManager.session.collectLatest { session ->
                updateForeground(session)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        diagnosticLog().i(TAG, "onStartCommand action=${intent?.action}")
        ensureForegroundStarted()
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> TtsPlaybackManager.togglePlayPause()
            ACTION_STOP -> {
                TtsPlaybackManager.stopPlayback()
                stopForegroundIfIdle(stopSelfDelayMs = STOP_SELF_DELAY_EXPLICIT)
            }
            ACTION_ENSURE_STARTED -> {
                val session = TtsPlaybackManager.session.value
                if (session.hasActiveSession) {
                    startForegroundWith(session)
                }
            }
            Intent.ACTION_MEDIA_BUTTON -> {
                if (TtsMediaButtonHandler.handleMediaButtonIntent(intent)) {
                    updateForeground(TtsPlaybackManager.session.value)
                } else {
                    val keyEvent = TtsMediaButtonHandler.keyEventFrom(intent)
                    if (keyEvent != null) {
                        mediaSession?.controller?.dispatchMediaButtonEvent(keyEvent)
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopSelfJob?.cancel()
        stopSelfJob = null
        serviceScope.cancel()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            TtsPlaybackManager.resumePlayback()
        }

        override fun onPause() {
            TtsPlaybackManager.pausePlayback()
        }

        override fun onStop() {
            TtsPlaybackManager.stopPlayback()
            stopForegroundIfIdle(stopSelfDelayMs = STOP_SELF_DELAY_EXPLICIT)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            if (TtsMediaButtonHandler.handleMediaButtonIntent(mediaButtonEvent)) {
                return true
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    private fun ensureForegroundStarted() {
        val session = TtsPlaybackManager.session.value
        diagnosticLog().i(
            TAG,
            "ensureForegroundStarted active=${session.hasActiveSession} promoted=$foregroundPromoted",
        )
        if (session.hasActiveSession) {
            startForegroundWith(session)
        } else {
            startForegroundPlaceholder()
        }
    }

    private fun updateForeground(session: TtsPlaybackSession) {
        updateMetadata(session)
        updatePlaybackState(session)
        if (session.hasActiveSession) {
            cancelDeferredStopSelf()
            startForegroundWith(session)
        } else {
            stopForegroundIfIdle()
        }
    }

    private fun updateMetadata(session: TtsPlaybackSession) {
        if (!session.hasActiveSession) return
        val artist = if (session.segmentTotal > 0) {
            "段落 ${session.segmentIndex + 1} / ${session.segmentTotal}"
        } else {
            "笔记朗读"
        }
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, session.title ?: "语音朗读")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "笔记朗读")
                .putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    TtsNotificationHelper.albumArtBitmap(this),
                )
                .build(),
        )
    }

    private fun updatePlaybackState(session: TtsPlaybackSession) {
        val state = when {
            session.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            session.isPaused -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE,
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
    }

    private fun startForegroundWith(session: TtsPlaybackSession) {
        val token = mediaSession?.sessionToken
        if (token != null) {
            val notification = TtsNotificationHelper.buildNotification(this, session, token)
            promoteToForeground(notification)
        } else {
            startForegroundPlaceholder()
        }
    }

    private fun startForegroundPlaceholder() {
        val notification = TtsNotificationHelper.buildPlaceholderNotification(
            context = this,
            sessionToken = mediaSession?.sessionToken,
        )
        promoteToForeground(notification)
    }

    private fun promoteToForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                TtsNotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(TtsNotificationHelper.NOTIFICATION_ID, notification)
        }
        foregroundPromoted = true
    }

    private fun stopForegroundIfIdle(stopSelfDelayMs: Long = STOP_SELF_DELAY_IDLE) {
        val session = TtsPlaybackManager.session.value
        if (session.hasActiveSession) return
        if (!foregroundPromoted) return
        diagnosticLog().i(
            TAG,
            "stopForegroundIfIdle promoted=$foregroundPromoted delayMs=$stopSelfDelayMs",
        )
        if (foregroundPromoted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        scheduleStopSelf(stopSelfDelayMs)
    }

    private fun scheduleStopSelf(delayMs: Long) {
        stopSelfJob?.cancel()
        stopSelfJob = serviceScope.launch {
            delay(delayMs)
            if (!TtsPlaybackManager.session.value.hasActiveSession) {
                diagnosticLog().i(TAG, "deferred stopSelf after ${delayMs}ms")
                stopSelf()
            }
        }
    }

    private fun cancelDeferredStopSelf() {
        stopSelfJob?.cancel()
        stopSelfJob = null
    }

    private fun diagnosticLog(): AppDiagnosticLog {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            TtsServiceEntryPoint::class.java,
        ).appDiagnosticLog()
    }

    private fun mediaButtonPendingIntent(): PendingIntent {
        val receiver = ComponentName(this, TtsMediaButtonReceiver::class.java)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            component = receiver
        }
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val TAG = "TtsPlaybackService"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val ACTION_PLAY_PAUSE = "com.andriod.reader.tts.PLAY_PAUSE"
        const val ACTION_STOP = "com.andriod.reader.tts.STOP"
        private const val ACTION_ENSURE_STARTED = "com.andriod.reader.tts.ENSURE_STARTED"
        private const val STOP_SELF_DELAY_IDLE = 1_500L
        private const val STOP_SELF_DELAY_EXPLICIT = 300L

        fun ensureStarted(context: Context) {
            val intent = Intent(context, TtsPlaybackService::class.java).apply {
                action = ACTION_ENSURE_STARTED
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun actionPendingIntent(context: Context, action: String): android.app.PendingIntent {
            val intent = Intent(context, TtsPlaybackService::class.java).apply {
                this.action = action
            }
            return android.app.PendingIntent.getService(
                context,
                action.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}

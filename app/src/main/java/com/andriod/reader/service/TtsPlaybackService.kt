package com.andriod.reader.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TtsPlaybackService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSessionCompat? = null

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
        serviceScope.launch {
            TtsPlaybackManager.session.collectLatest { session ->
                updateForeground(session)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> TtsPlaybackManager.togglePlayPause()
            ACTION_STOP -> {
                TtsPlaybackManager.stopPlayback()
                stopForegroundIfIdle()
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
            stopForegroundIfIdle()
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            if (TtsMediaButtonHandler.handleMediaButtonIntent(mediaButtonEvent)) {
                return true
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    private fun updateForeground(session: TtsPlaybackSession) {
        updateMetadata(session)
        updatePlaybackState(session)
        if (session.hasActiveSession) {
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
        val token = mediaSession?.sessionToken ?: return
        val notification = TtsNotificationHelper.buildNotification(this, session, token)
        startForeground(TtsNotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun stopForegroundIfIdle() {
        val session = TtsPlaybackManager.session.value
        if (!session.hasActiveSession) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
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

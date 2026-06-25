package com.andriod.reader

import android.app.Application
import android.os.Build
import android.util.Log
import com.andriod.reader.service.TtsServiceEntryPoint
import com.andriod.reader.ui.reader.MuyuKnockFeedback
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(this, TtsServiceEntryPoint::class.java)
            val log = entryPoint.appDiagnosticLog()
            val ttsBackend = runCatching {
                entryPoint.settingsStore().getTtsSpeechBackend().name
            }.getOrDefault("unknown")
            val edgeVoice = runCatching {
                entryPoint.settingsStore().getEdgeTtsVoiceId()
            }.getOrNull()
            log.logSessionHeader(
                buildString {
                    append("app start ")
                    append("sdk=${Build.VERSION.SDK_INT} ")
                    append("device=${Build.MANUFACTURER} ${Build.MODEL} ")
                    append("ttsBackend=$ttsBackend ")
                    edgeVoice?.let { append("edgeVoice=$it") }
                },
            )
        }.onFailure { error ->
            Log.w(TAG, "Diagnostic session header skipped", error)
        }
        MuyuKnockFeedback.preload(this)
    }

    companion object {
        private const val TAG = "ReaderApplication"
    }
}

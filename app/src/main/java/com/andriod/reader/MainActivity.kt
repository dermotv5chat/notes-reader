package com.andriod.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriod.reader.service.TtsPlaybackService
import com.andriod.reader.ui.ReaderApp
import com.andriod.reader.ui.theme.ReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.andriod.reader.data.remote.SettingsStore

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsStore: SettingsStore

    private var openReaderTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openReaderTarget = intent.getStringExtra(TtsPlaybackService.EXTRA_FILE_NAME)
        setContent {
            val themeMode by settingsStore.appThemeMode.collectAsStateWithLifecycle()
            ReaderTheme(themeMode = themeMode) {
                ReaderApp(
                    openReaderFileName = openReaderTarget,
                    onOpenReaderConsumed = { openReaderTarget = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openReaderTarget = intent.getStringExtra(TtsPlaybackService.EXTRA_FILE_NAME)
    }
}

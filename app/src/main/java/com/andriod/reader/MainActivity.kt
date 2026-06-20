package com.andriod.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.andriod.reader.service.TtsPlaybackService
import com.andriod.reader.ui.ReaderApp
import com.andriod.reader.ui.theme.ReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var openReaderTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openReaderTarget = intent.getStringExtra(TtsPlaybackService.EXTRA_FILE_NAME)
        setContent {
            ReaderTheme {
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

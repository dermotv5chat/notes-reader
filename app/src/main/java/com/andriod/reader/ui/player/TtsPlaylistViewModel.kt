package com.andriod.reader.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.TtsPlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class TtsPlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistManager: TtsPlaylistManager,
) : ViewModel() {
    val snapshot = playlistManager.state

    fun add(fileName: String, title: String) = playlistManager.add(fileName, title)

    fun remove(fileName: String) = playlistManager.remove(fileName)

    fun clear() = playlistManager.clear()

    fun setRepeatMode(mode: TtsQueueRepeatMode) = playlistManager.setRepeatMode(mode)

    fun playItem(fileName: String) = playlistManager.playItem(context, fileName)

    fun contains(fileName: String): Boolean = playlistManager.contains(fileName)
}

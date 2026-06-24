package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class TtsPlaylistSnapshot(
    val items: List<TtsPlaylistItem> = emptyList(),
    val repeatMode: TtsQueueRepeatMode = TtsQueueRepeatMode.OFF,
)

@Singleton
class TtsPlaylistStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val metaDir: java.io.File
        get() = java.io.File(context.filesDir, ".meta").also { it.mkdirs() }

    private val playlistFile: java.io.File
        get() = java.io.File(metaDir, "tts-playlist.json")

    fun read(): TtsPlaylistSnapshot {
        val file = playlistFile
        if (!file.exists()) return TtsPlaylistSnapshot()
        val dto = gson.fromJson(file.readText(), PlaylistDto::class.java) ?: return TtsPlaylistSnapshot()
        return TtsPlaylistSnapshot(
            items = dto.items.mapNotNull { it.toItem() },
            repeatMode = runCatching { TtsQueueRepeatMode.valueOf(dto.repeatMode) }
                .getOrDefault(TtsQueueRepeatMode.OFF),
        )
    }

    fun write(snapshot: TtsPlaylistSnapshot) {
        val dto = PlaylistDto(
            repeatMode = snapshot.repeatMode.name,
            items = snapshot.items.map {
                PlaylistItemDto(
                    fileName = it.fileName,
                    title = it.title,
                    addedAt = it.addedAt.toString(),
                )
            },
        )
        if (snapshot.items.isEmpty() && snapshot.repeatMode == TtsQueueRepeatMode.OFF) {
            if (playlistFile.exists()) playlistFile.delete()
        } else {
            playlistFile.writeText(gson.toJson(dto))
        }
    }

    private data class PlaylistDto(
        val repeatMode: String = TtsQueueRepeatMode.OFF.name,
        val items: List<PlaylistItemDto> = emptyList(),
    )

    private data class PlaylistItemDto(
        val fileName: String,
        val title: String,
        val addedAt: String,
    ) {
        fun toItem(): TtsPlaylistItem? = runCatching {
            TtsPlaylistItem(
                fileName = fileName,
                title = title,
                addedAt = Instant.parse(addedAt),
            )
        }.getOrNull()
    }
}

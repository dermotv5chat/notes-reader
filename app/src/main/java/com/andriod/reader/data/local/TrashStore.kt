package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.SyncFileState
import com.andriod.reader.domain.SyncStatus
import com.andriod.reader.domain.TrashEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val metaDir: File
        get() = File(context.filesDir, ".meta").also { it.mkdirs() }

    private val trashFile: File
        get() = File(metaDir, "trash.json")

    fun listAll(): List<TrashEntry> {
        val file = trashFile
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<Map<String, TrashEntryDto>>() {}.type
        val dtoMap: Map<String, TrashEntryDto> = gson.fromJson(file.readText(), type) ?: emptyMap()
        return dtoMap.values
            .map { it.toEntry() }
            .sortedByDescending { it.deletedAt }
    }

    fun get(id: String): TrashEntry? {
        return listAll().find { it.id == id }
    }

    fun add(entry: TrashEntry) {
        val map = readDtoMap().toMutableMap()
        map[entry.id] = TrashEntryDto.from(entry)
        writeDtoMap(map)
    }

    fun remove(id: String) {
        val map = readDtoMap().toMutableMap()
        map.remove(id)
        writeDtoMap(map)
    }

    private fun readDtoMap(): Map<String, TrashEntryDto> {
        val file = trashFile
        if (!file.exists()) return emptyMap()
        val type = object : TypeToken<Map<String, TrashEntryDto>>() {}.type
        return gson.fromJson(file.readText(), type) ?: emptyMap()
    }

    private fun writeDtoMap(map: Map<String, TrashEntryDto>) {
        trashFile.writeText(gson.toJson(map))
    }

    private data class TrashEntryDto(
        val id: String,
        val originalPath: String,
        val deletedAt: String,
        val githubSha: String? = null,
        val remotePath: String? = null,
        val syncStatus: String = SyncStatus.LOCAL_ONLY.name,
        val pendingDelete: Boolean = false,
    ) {
        fun toEntry(): TrashEntry = TrashEntry(
            id = id,
            originalPath = originalPath,
            deletedAt = Instant.parse(deletedAt),
            syncState = SyncFileState(
                githubSha = githubSha,
                remotePath = remotePath,
                syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }
                    .getOrDefault(SyncStatus.LOCAL_ONLY),
                pendingDelete = pendingDelete,
            ),
        )

        companion object {
            fun from(entry: TrashEntry): TrashEntryDto = TrashEntryDto(
                id = entry.id,
                originalPath = entry.originalPath,
                deletedAt = entry.deletedAt.toString(),
                githubSha = entry.syncState.githubSha,
                remotePath = entry.syncState.remotePath,
                syncStatus = entry.syncState.syncStatus.name,
                pendingDelete = entry.syncState.pendingDelete,
            )
        }
    }
}

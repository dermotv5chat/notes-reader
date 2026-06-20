package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.SyncFileState
import com.andriod.reader.domain.SyncStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val metaDir: File
        get() = File(context.filesDir, ".meta").also { it.mkdirs() }

    private val stateFile: File
        get() = File(metaDir, "sync-state.json")

    fun readAll(): Map<String, SyncFileState> {
        val file = stateFile
        if (!file.exists()) return emptyMap()
        val type = object : TypeToken<Map<String, SyncFileStateDto>>() {}.type
        val dtoMap: Map<String, SyncFileStateDto> = gson.fromJson(file.readText(), type) ?: emptyMap()
        return dtoMap.mapValues { (_, dto) ->
            SyncFileState(
                githubSha = dto.githubSha,
                remotePath = dto.remotePath,
                syncStatus = runCatching { SyncStatus.valueOf(dto.syncStatus) }
                    .getOrDefault(SyncStatus.LOCAL_ONLY),
                pendingDelete = dto.pendingDelete,
            )
        }
    }

    fun writeAll(states: Map<String, SyncFileState>) {
        val dtoMap = states.mapValues { (_, state) ->
            SyncFileStateDto(
                githubSha = state.githubSha,
                remotePath = state.remotePath,
                syncStatus = state.syncStatus.name,
                pendingDelete = state.pendingDelete,
            )
        }
        stateFile.writeText(gson.toJson(dtoMap))
    }

    private data class SyncFileStateDto(
        val githubSha: String? = null,
        val remotePath: String? = null,
        val syncStatus: String = SyncStatus.LOCAL_ONLY.name,
        val pendingDelete: Boolean = false,
    )
}

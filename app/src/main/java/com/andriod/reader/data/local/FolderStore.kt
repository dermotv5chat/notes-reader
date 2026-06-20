package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.data.repository.SyncPathUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val metaDir: File
        get() = File(context.filesDir, ".meta").also { it.mkdirs() }

    private val foldersFile: File
        get() = File(metaDir, "folders.json")

    fun listAll(): Set<String> {
        val file = foldersFile
        if (!file.exists()) return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson<Set<String>>(file.readText(), type)?.map { SyncPathUtils.normalize(it) }?.toSet()
            ?: emptySet()
    }

    fun add(folderPath: String) {
        val normalized = SyncPathUtils.normalize(folderPath)
        if (normalized.isEmpty()) return
        val folders = listAll().toMutableSet()
        folders.add(normalized)
        writeAll(folders)
    }

    fun remove(folderPath: String) {
        val normalized = SyncPathUtils.normalize(folderPath)
        val folders = listAll().toMutableSet()
        folders.remove(normalized)
        writeAll(folders)
    }

    fun renamePrefix(oldPrefix: String, newPrefix: String) {
        val oldNorm = SyncPathUtils.normalize(oldPrefix)
        val newNorm = SyncPathUtils.normalize(newPrefix)
        val folders = listAll().map { path ->
            when {
                path == oldNorm -> newNorm
                path.startsWith("$oldNorm/") -> path.replaceFirst(oldNorm, newNorm)
                else -> path
            }
        }.toSet()
        writeAll(folders)
    }

    private fun writeAll(folders: Set<String>) {
        foldersFile.writeText(gson.toJson(folders.sorted()))
    }
}

package com.andriod.reader.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRegistryStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val metaDir: File
        get() = File(context.filesDir, ".meta").also { it.mkdirs() }

    private val registryFile: File
        get() = File(metaDir, "block-registry.json")

    fun read(fileName: String): FileBlockRegistry {
        val all = readAll()
        return all[fileName] ?: FileBlockRegistry()
    }

    fun write(fileName: String, registry: FileBlockRegistry) {
        val all = readAll().toMutableMap()
        if (registry.order.isEmpty() && registry.entries.isEmpty()) {
            all.remove(fileName)
        } else {
            all[fileName] = registry
        }
        registryFile.writeText(gson.toJson(all))
    }

    private fun readAll(): Map<String, FileBlockRegistry> {
        val file = registryFile
        if (!file.exists()) return emptyMap()
        val type = object : TypeToken<Map<String, FileBlockRegistry>>() {}.type
        return gson.fromJson(file.readText(), type) ?: emptyMap()
    }
}

data class FileBlockRegistry(
    val order: List<String> = emptyList(),
    val entries: Map<String, BlockRegistryEntry> = emptyMap(),
)

data class BlockRegistryEntry(
    val variant: String,
    val textHint: String = "",
    val mode: String? = null,
    val repeatPeriod: String? = null,
    val cadence: String? = null,
    val dailyKind: String? = null,
)

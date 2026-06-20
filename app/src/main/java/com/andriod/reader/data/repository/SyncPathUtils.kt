package com.andriod.reader.data.repository

object SyncPathUtils {
    fun normalize(path: String): String = path.trim().trim('/').replace('\\', '/')

    fun fileBaseName(path: String): String = path.substringAfterLast('/')
}

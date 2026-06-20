package com.andriod.reader.ui

import android.net.Uri

object NavArgs {
    fun decodeFileName(value: String?): String =
        value?.let { Uri.decode(it) } ?: ""
}

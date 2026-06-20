package com.andriod.reader.domain

data class TtsVoiceOption(
    val id: String,
    val label: String,
    val isOnline: Boolean,
)

enum class TtsVoicePreference {
    AUTO,
    PREFER_LOCAL,
    PREFER_ONLINE,
}

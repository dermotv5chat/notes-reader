package com.andriod.reader.service

enum class TtsPlaybackMode {
    None,
    Presynth,
    SegmentOnline,
    SegmentSherpa,
    SegmentSystem,
    ;

    fun displayLabel(): String? = when (this) {
        None -> null
        Presynth -> "整篇预合成"
        SegmentOnline -> "逐段 · 在线"
        SegmentSherpa -> "逐段 · 离线"
        SegmentSystem -> "逐段 · 系统"
    }
}

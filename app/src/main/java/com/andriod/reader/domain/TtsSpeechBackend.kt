package com.andriod.reader.domain

enum class TtsSpeechBackend {
    /** Android TextToSpeech（系统 / Google 引擎） */
    SYSTEM,
    /** Microsoft Edge 在线 neural（需联网，非官方接口） */
    ONLINE_EDGE,
    /** Sherpa-onnx 离线 neural（需下载语音包） */
    OFFLINE_SHERPA,
}

package com.andriod.reader.service.synthesis

import com.andriod.reader.domain.TtsSpeechBackend
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsPreSynthUnavailableMessageTest {
    @Test
    fun unavailableMessageFor_sherpa() {
        assertEquals(
            "请先在设置中下载离线语音包",
            TtsPreSynthPipeline.unavailableMessageFor(TtsSpeechBackend.OFFLINE_SHERPA),
        )
    }

    @Test
    fun unavailableMessageFor_edge() {
        assertEquals(
            "当前无网络，无法生成在线语音",
            TtsPreSynthPipeline.unavailableMessageFor(TtsSpeechBackend.ONLINE_EDGE),
        )
    }
}

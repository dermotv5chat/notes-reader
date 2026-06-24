package com.andriod.reader.service

import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.service.synthesis.TtsPreSynthPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsPresynthBlockedMessageTest {
    @Test
    fun presynthBlockedMessage_systemBackend_returnsNull() {
        assertNull(
            TtsPlaybackManager.presynthBlockedMessage(
                backend = TtsSpeechBackend.SYSTEM,
                networkConnected = false,
                sherpaInstalled = false,
            ),
        )
    }

    @Test
    fun presynthBlockedMessage_edgeWithoutNetwork_returnsMessage() {
        assertEquals(
            TtsPreSynthPipeline.unavailableMessageFor(TtsSpeechBackend.ONLINE_EDGE),
            TtsPlaybackManager.presynthBlockedMessage(
                backend = TtsSpeechBackend.ONLINE_EDGE,
                networkConnected = false,
                sherpaInstalled = false,
            ),
        )
    }

    @Test
    fun presynthBlockedMessage_edgeWithNetwork_returnsNull() {
        assertNull(
            TtsPlaybackManager.presynthBlockedMessage(
                backend = TtsSpeechBackend.ONLINE_EDGE,
                networkConnected = true,
                sherpaInstalled = false,
            ),
        )
    }

    @Test
    fun presynthBlockedMessage_sherpaWithoutModel_returnsMessage() {
        assertEquals(
            TtsPreSynthPipeline.unavailableMessageFor(TtsSpeechBackend.OFFLINE_SHERPA),
            TtsPlaybackManager.presynthBlockedMessage(
                backend = TtsSpeechBackend.OFFLINE_SHERPA,
                networkConnected = true,
                sherpaInstalled = false,
            ),
        )
    }

    @Test
    fun presynthBlockedMessage_sherpaWithModel_returnsNull() {
        assertNull(
            TtsPlaybackManager.presynthBlockedMessage(
                backend = TtsSpeechBackend.OFFLINE_SHERPA,
                networkConnected = false,
                sherpaInstalled = true,
            ),
        )
    }
}

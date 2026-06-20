package com.andriod.reader.service

object TtsPlaybackEndAction {
    fun shouldRestartAfterLastSegment(loopEnabled: Boolean): Boolean = loopEnabled
}

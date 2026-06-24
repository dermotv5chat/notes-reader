package com.andriod.reader.service.synthesis

sealed class PresynthPrepareResult {
    data object Started : PresynthPrepareResult()

    data class Queued(val position: Int) : PresynthPrepareResult()

    data object AlreadyQueued : PresynthPrepareResult()

    data object Unavailable : PresynthPrepareResult()
}

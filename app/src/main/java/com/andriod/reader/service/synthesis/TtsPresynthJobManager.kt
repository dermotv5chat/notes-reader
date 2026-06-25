package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.MarkdownPlainText
import com.andriod.reader.domain.TtsPresynthUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPresynthJobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeline: TtsPreSynthPipeline,
    private val notificationHelper: PresynthNotificationHelper,
) {
    private data class PendingPresynthJob(
        val fileName: String,
        val title: String,
        val plainText: String,
        val forceRegenerate: Boolean,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _jobs = MutableStateFlow<Map<String, PresynthJobState>>(emptyMap())
    val jobs: StateFlow<Map<String, PresynthJobState>> = _jobs.asStateFlow()

    private val queue = ArrayDeque<PendingPresynthJob>()
    private var activeFileName: String? = null
    private var progressCollectJob: Job? = null

    fun prepareBackground(
        fileName: String,
        title: String,
        content: String,
        forceRegenerate: Boolean = false,
    ): PresynthPrepareResult {
        if (!pipeline.usesPresynthBackend()) return PresynthPrepareResult.Unavailable
        val plain = MarkdownPlainText.stripForSpeech(content)
        if (plain.isBlank()) return PresynthPrepareResult.Unavailable

        if (queue.any { it.fileName == fileName }) {
            return PresynthPrepareResult.AlreadyQueued
        }

        if (pipeline.isPrepareInProgress()) {
            if (activeFileName == fileName) {
                return PresynthPrepareResult.Started
            }
            queue.add(
                PendingPresynthJob(
                    fileName = fileName,
                    title = title,
                    plainText = plain,
                    forceRegenerate = forceRegenerate,
                ),
            )
            refreshQueuedJobStates()
            return PresynthPrepareResult.Queued(queue.size)
        }

        startJob(fileName, title, plain, forceRegenerate)
        return PresynthPrepareResult.Started
    }

    fun cancel(fileName: String) {
        val removedFromQueue = queue.removeAll { it.fileName == fileName }
        if (removedFromQueue) {
            refreshQueuedJobStates()
            restoreCachedJobState(fileName)
            return
        }
        if (activeFileName != fileName) return
        pipeline.cancelPrepare()
        activeFileName = null
        progressCollectJob?.cancel()
        progressCollectJob = null
        restoreCachedJobState(fileName)
        startNextQueuedJob()
    }

    fun cancelAll() {
        queue.clear()
        activeFileName?.let { cancel(it) }
        refreshQueuedJobStates()
    }

    fun queuePosition(fileName: String): Int? {
        val index = queue.indexOfFirst { it.fileName == fileName }
        return if (index >= 0) index + 1 else null
    }

    fun jobStateFor(fileName: String, title: String, plainText: String): PresynthJobState {
        queuePosition(fileName)?.let { position ->
            return PresynthJobState(
                fileName = fileName,
                title = title,
                uiState = TtsPresynthUiState.Queued,
                hint = queuedHint(position),
            )
        }
        _jobs.value[fileName]?.takeIf { job ->
            when (job.uiState) {
                TtsPresynthUiState.Queued -> true
                TtsPresynthUiState.Preparing ->
                    activeFileName == fileName && pipeline.isPrepareInProgress()
                else -> false
            }
        }?.let { return it }
        if (activeFileName == fileName && pipeline.isPrepareInProgress()) {
            val progress = pipeline.progress.value
            return PresynthJobState(
                fileName = fileName,
                title = title,
                uiState = progress.state,
                hint = progress.hint,
                chunkProgress = progress.chunkProgress,
                progressFraction = progress.progressFraction,
            )
        }
        val progress = pipeline.uiStateForPlainText(plainText)
        return PresynthJobState(
            fileName = fileName,
            title = title,
            uiState = progress.state,
            hint = progress.hint,
            chunkProgress = progress.chunkProgress,
            progressFraction = progress.progressFraction,
        )
    }

    fun refreshJobFor(fileName: String, title: String, plainText: String) {
        _jobs.value = _jobs.value + (fileName to jobStateFor(fileName, title, plainText))
    }

    private fun startJob(
        fileName: String,
        title: String,
        plainText: String,
        forceRegenerate: Boolean,
    ) {
        val previousActive = activeFileName
        if (previousActive != null && previousActive != fileName) {
            restoreCachedJobState(previousActive)
        }
        activeFileName = fileName
        updateJob(
            fileName = fileName,
            title = title,
            progress = pipeline.progress.value.copy(
                state = TtsPresynthUiState.Preparing,
                hint = "正在生成语音…",
            ),
        )
        startProgressCollection(fileName, title)
        pipeline.prepare(
            plainText = plainText,
            forceRegenerate = forceRegenerate,
            autoPlayWhenReady = false,
            onPrepareFailed = { message ->
                finishJob(fileName, title, success = false, message = message)
            },
            onPrepareCancelled = {
                if (activeFileName == fileName) {
                    activeFileName = null
                    progressCollectJob?.cancel()
                    progressCollectJob = null
                    restoreCachedJobState(fileName)
                    startNextQueuedJob()
                }
            },
        )
    }

    private fun startProgressCollection(fileName: String, title: String) {
        progressCollectJob?.cancel()
        progressCollectJob = scope.launch {
            var wasPreparing = false
            pipeline.progress.collect { progress ->
                if (activeFileName != fileName) return@collect
                if (progress.state == TtsPresynthUiState.Preparing) {
                    wasPreparing = true
                }
                updateJob(fileName, title, progress)
                when (progress.state) {
                    TtsPresynthUiState.Ready -> {
                        if (wasPreparing || activeFileName == fileName) {
                            finishJob(fileName, title, success = true, message = null)
                        }
                    }
                    TtsPresynthUiState.Failed -> finishJob(
                        fileName,
                        title,
                        success = false,
                        message = progress.hint,
                    )
                    else -> Unit
                }
            }
        }
    }

    private fun finishJob(fileName: String, title: String, success: Boolean, message: String?) {
        if (activeFileName == fileName) {
            activeFileName = null
            progressCollectJob?.cancel()
            progressCollectJob = null
        }
        val progress = pipeline.progress.value
        updateJob(fileName, title, progress)
        notificationHelper.notifyIfBackground(
            title = title,
            success = success,
            message = message ?: progress.hint,
        )
        startNextQueuedJob()
    }

    private fun startNextQueuedJob() {
        val next = queue.removeFirstOrNull() ?: run {
            refreshQueuedJobStates()
            return
        }
        refreshQueuedJobStates()
        startJob(next.fileName, next.title, next.plainText, next.forceRegenerate)
    }

    private fun refreshQueuedJobStates() {
        val updated = _jobs.value.toMutableMap()
        val queuedFileNames = queue.map { it.fileName }.toSet()
        updated.keys.toList().forEach { key ->
            val job = updated[key] ?: return@forEach
            if (job.uiState == TtsPresynthUiState.Queued && key !in queuedFileNames) {
                restoreCachedJobState(key)?.let { restored ->
                    updated[key] = restored
                } ?: updated.remove(key)
            }
        }
        queue.forEachIndexed { index, job ->
            updated[job.fileName] = PresynthJobState(
                fileName = job.fileName,
                title = job.title,
                uiState = TtsPresynthUiState.Queued,
                hint = queuedHint(index + 1),
            )
        }
        _jobs.value = updated
    }

    private fun restoreCachedJobState(fileName: String): PresynthJobState? {
        val note = noteRepositoryPlainText(fileName)
        if (note == null) {
            _jobs.value = _jobs.value - fileName
            return null
        }
        val (title, plain) = note
        val progress = pipeline.uiStateForPlainText(plain)
        val restored = PresynthJobState(
            fileName = fileName,
            title = title,
            uiState = progress.state,
            hint = progress.hint,
            chunkProgress = progress.chunkProgress,
            progressFraction = progress.progressFraction,
        )
        _jobs.value = _jobs.value + (fileName to restored)
        return restored
    }

    private fun queuedHint(position: Int): String = "排队中（第 $position 位）"

    private fun updateJob(fileName: String, title: String, progress: TtsPreSynthProgress) {
        _jobs.value = _jobs.value + (
            fileName to PresynthJobState(
                fileName = fileName,
                title = title,
                uiState = progress.state,
                hint = progress.hint,
                chunkProgress = progress.chunkProgress,
                progressFraction = progress.progressFraction,
            )
            )
    }

    private fun noteRepositoryPlainText(fileName: String): Pair<String, String>? {
        return runCatching {
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.andriod.reader.service.TtsServiceEntryPoint::class.java,
            )
            val note = entryPoint.noteRepository().getNote(fileName) ?: return null
            val plain = MarkdownPlainText.stripForSpeech(note.content)
            note.title to plain
        }.getOrNull()
    }
}

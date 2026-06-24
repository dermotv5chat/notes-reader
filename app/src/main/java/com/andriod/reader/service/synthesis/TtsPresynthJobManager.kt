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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _jobs = MutableStateFlow<Map<String, PresynthJobState>>(emptyMap())
    val jobs: StateFlow<Map<String, PresynthJobState>> = _jobs.asStateFlow()

    private var activeFileName: String? = null
    private var activeTitle: String? = null
    private var progressCollectJob: Job? = null

    fun prepareBackground(
        fileName: String,
        title: String,
        content: String,
        forceRegenerate: Boolean = false,
    ): Boolean {
        if (!pipeline.usesPresynthBackend()) return false
        val plain = MarkdownPlainText.stripForSpeech(content)
        if (plain.isBlank()) return false
        if (activeFileName != null && activeFileName != fileName && pipeline.progress.value.state == TtsPresynthUiState.Preparing) {
            return false
        }
        activeFileName = fileName
        activeTitle = title
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
            plainText = plain,
            forceRegenerate = forceRegenerate,
            autoPlayWhenReady = false,
            onPrepareFailed = { message ->
                finishJob(fileName, title, success = false, message = message)
            },
        )
        return true
    }

    fun cancel(fileName: String) {
        if (activeFileName != fileName) return
        pipeline.cancelPrepare()
        activeFileName = null
        activeTitle = null
        progressCollectJob?.cancel()
        progressCollectJob = null
        val note = noteRepositoryPlainText(fileName)
        if (note != null) {
            _jobs.value = _jobs.value + (fileName to jobStateFor(fileName, note.first, note.second))
        } else {
            _jobs.value = _jobs.value - fileName
        }
    }

    fun cancelAll() {
        activeFileName?.let { cancel(it) }
    }

    fun jobStateFor(fileName: String, title: String, plainText: String): PresynthJobState {
        _jobs.value[fileName]?.takeIf { it.uiState == TtsPresynthUiState.Preparing }?.let { return it }
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
            activeTitle = null
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
    }

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

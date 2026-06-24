package com.andriod.reader.ui.reader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.data.repository.BlockPracticeDisplayMeta
import com.andriod.reader.data.repository.PracticeRepository
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.RepeatPeriod
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.TtsPresynthUiState
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.LastSleepTimerPreset
import com.andriod.reader.service.SleepTimerPresetPolicy
import com.andriod.reader.service.SleepTimerMode
import com.andriod.reader.service.TtsHelper
import com.andriod.reader.service.TtsVoiceQuality
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.service.TtsPlaybackSession
import com.andriod.reader.service.TtsPlaylistManager
import com.andriod.reader.service.edge.NetworkAvailability
import com.andriod.reader.service.synthesis.SherpaDownloadPhase
import com.andriod.reader.service.synthesis.SherpaDownloadProgress
import com.andriod.reader.service.synthesis.SherpaModelDownloadCoordinator
import com.andriod.reader.ui.NavArgs
import com.andriod.reader.ui.Routes
import com.andriod.reader.util.NotificationPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.Instant
import javax.inject.Inject

data class ReaderUiState(
    val note: Note? = null,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val keepScreenOn: Boolean = false,
    val isPlaying: Boolean = false,
    val segmentIndex: Int = 0,
    val segmentTotal: Int = 0,
    val voiceOptions: List<TtsVoiceOption> = emptyList(),
    val selectedVoiceId: String? = null,
    val voicePreference: TtsVoicePreference = TtsVoicePreference.AUTO,
    val voicePickerExpanded: Boolean = false,
    val speechBackend: TtsSpeechBackend = TtsSpeechBackend.SYSTEM,
    val ttsQualityHint: String? = null,
    val isTtsReady: Boolean = false,
    val isTtsInitializing: Boolean = false,
    val ttsError: String? = null,
    val queueRepeatMode: TtsQueueRepeatMode = TtsQueueRepeatMode.OFF,
    val queueCount: Int = 0,
    val noteInQueue: Boolean = false,
    val canSelectRepeatAll: Boolean = false,
    val ttsSettingsVisible: Boolean = false,
    val sleepTimerVisible: Boolean = false,
    val sleepTimerSliderMinutes: Float = 0f,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.Off,
    val sleepTimerRemainingMs: Long? = null,
    val sleepTimerLabel: String? = null,
    val lastSleepTimerPresetSubtitle: String = "30 分钟",
    val estimatedNoteRemainingMinutes: Int = 0,
    val canScheduleAfterNoteEnd: Boolean = false,
    val backgroundNoteTitle: String? = null,
    val notificationPermissionDenied: Boolean = false,
    val presynthState: TtsPresynthUiState = TtsPresynthUiState.Hidden,
    val presynthHint: String? = null,
    val presynthProgress: String? = null,
    val presynthCharCount: Int = 0,
    val presynthButtonEnabled: Boolean = true,
    val presynthSnackbar: String? = null,
    val sherpaModelInstalled: Boolean = false,
    val isDownloadingSherpaModel: Boolean = false,
    val sherpaDownloadHint: String? = null,
    val sherpaDownloadProgress: Float? = null,
    val sherpaDownloadPhase: SherpaDownloadPhase = SherpaDownloadPhase.Idle,
    val sherpaDownloadBytesLabel: String? = null,
    val sherpaDownloadSnackbar: String? = null,
    val blocks: List<NoteBlock> = emptyList(),
    val todayPractice: Map<String, PracticeDayEntry> = emptyMap(),
    val practiceMeta: Map<String, BlockPracticeDisplayMeta> = emptyMap(),
    val practiceSheet: PracticeSheetState? = null,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val practiceRepository: PracticeRepository,
    private val settingsStore: SettingsStore,
    private val playlistManager: TtsPlaylistManager,
    private val diagnosticLog: AppDiagnosticLog,
    private val sherpaDownloadCoordinator: SherpaModelDownloadCoordinator,
) : ViewModel() {
    private val fileName: String = NavArgs.decodeFileName(savedStateHandle.get<String>("fileName"))

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var controller: com.andriod.reader.service.TtsController? = null
    private var lastHostContext: Context? = null
    private var pendingStartAfterPermission = false
    private var lastPresynthState: TtsPresynthUiState = TtsPresynthUiState.Hidden
    private var presynthCollectJob: Job? = null

    companion object {
        private const val NOTIFICATION_PERMISSION_MESSAGE =
            "需要通知权限才能在后台显示朗读控制"
    }

    init {
        viewModelScope.launch {
            TtsPlaybackManager.session.collect { session ->
                syncFromSession(session)
            }
        }
        viewModelScope.launch {
            playlistManager.state.collect { snapshot ->
                val fileName = _uiState.value.note?.fileName
                _uiState.update {
                    it.copy(
                        queueRepeatMode = snapshot.repeatMode,
                        queueCount = snapshot.items.size,
                        noteInQueue = fileName != null && snapshot.items.any { item -> item.fileName == fileName },
                        canSelectRepeatAll = snapshot.items.isNotEmpty(),
                    )
                }
            }
        }
    }

    private fun syncPresynthProgress(progress: com.andriod.reader.service.synthesis.TtsPreSynthProgress) {
        val backend = _uiState.value.speechBackend
        val canPrepare = TtsPlaybackManager.canPreparePresynth()
        val buttonEnabled = when (backend) {
            TtsSpeechBackend.ONLINE_EDGE -> canPrepare
            TtsSpeechBackend.OFFLINE_SHERPA -> TtsPlaybackManager.sherpaModelInstalled()
            TtsSpeechBackend.SYSTEM -> false
        }
        val hint = when {
            backend == TtsSpeechBackend.ONLINE_EDGE && !canPrepare &&
                progress.state != TtsPresynthUiState.Preparing ->
                "当前无网络，无法生成；可改用离线高质量或系统朗读"
            backend == TtsSpeechBackend.OFFLINE_SHERPA && !TtsPlaybackManager.sherpaModelInstalled() &&
                !sherpaDownloadCoordinator.isInstalled() &&
                progress.state != TtsPresynthUiState.Ready ->
                "请先下载离线语音包（朗读设置）"
            else -> progress.hint
        }
        val snackbar = if (
            progress.state == TtsPresynthUiState.Ready &&
            lastPresynthState == TtsPresynthUiState.Preparing
        ) {
            "语音已生成"
        } else {
            null
        }
        lastPresynthState = progress.state
        _uiState.update {
            it.copy(
                presynthState = progress.state,
                presynthHint = hint,
                presynthProgress = progress.chunkProgress,
                presynthCharCount = progress.charCount,
                presynthButtonEnabled = buttonEnabled || progress.state == TtsPresynthUiState.Preparing,
                presynthSnackbar = snackbar ?: it.presynthSnackbar,
            )
        }
    }

    fun clearPresynthSnackbar() {
        _uiState.update { it.copy(presynthSnackbar = null) }
    }

    private fun startPresynthProgressCollection() {
        presynthCollectJob?.cancel()
        presynthCollectJob = viewModelScope.launch {
            TtsPlaybackManager.presynthProgress()?.collect { progress ->
                syncPresynthProgress(progress)
            }
        }
    }

    private fun readVoicePreference(): TtsVoicePreference {
        return runCatching {
            TtsVoicePreference.valueOf(settingsStore.getVoicePreference())
        }.getOrDefault(TtsVoicePreference.AUTO)
    }

    private fun syncFromSession(session: TtsPlaybackSession) {
        val currentFileName = _uiState.value.note?.fileName
        val isDifferentNote = session.fileName != null &&
            currentFileName != null &&
            session.fileName != currentFileName
        val current = _uiState.value
        val estimatedMinutes = if (session.sleepTimerMode == SleepTimerMode.AfterNoteEnd) {
            TtsPlaybackManager.estimateNoteRemainingMinutes(
                noteContent = current.note?.content,
                speechRate = current.speechRate,
            )
        } else {
            current.estimatedNoteRemainingMinutes
        }
        _uiState.update {
            it.copy(
                segmentIndex = session.segmentIndex,
                segmentTotal = session.segmentTotal,
                isPlaying = session.isPlaying,
                backgroundNoteTitle = if (isDifferentNote) session.title else null,
                sleepTimerMode = session.sleepTimerMode,
                sleepTimerRemainingMs = session.sleepTimerRemainingMs,
                sleepTimerLabel = session.sleepTimerLabel,
                estimatedNoteRemainingMinutes = estimatedMinutes,
            )
        }
    }

    private fun attachUiCallbacks() {
        TtsPlaybackManager.attachUiCallbacks(
            onSegmentChanged = { index, total ->
                _uiState.update { it.copy(segmentIndex = index, segmentTotal = total) }
            },
            onPlaybackStateChanged = { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            },
            onSpeakError = { message ->
                diagnosticLog.e("Reader", "ttsError: $message")
                _uiState.update { it.copy(ttsError = message, isPlaying = false) }
            },
        )
    }

    fun initTts(hostContext: Context) {
        lastHostContext = hostContext
        diagnosticLog.d("Reader", "initTts file=$fileName")
        viewModelScope.launch {
            _uiState.update { it.copy(isTtsInitializing = true, ttsError = null) }
            attachUiCallbacks()
            val existing = TtsPlaybackManager.getOrNull()
            val tts = if (existing != null && existing.isReady()) {
                existing
            } else {
                val ctrl = TtsPlaybackManager.getOrCreate(hostContext)
                val ready = ctrl.awaitReady(hostContext)
                if (!ready) {
                    val engines = TtsHelper.listInstalledEngines(hostContext)
                    val defaultEngine = TtsHelper.defaultEnginePackage(hostContext)
                    val tried = ctrl.attemptedEngineLabels()
                    diagnosticLog.e("Reader", "initTts failed engines=$tried")
                    _uiState.update {
                        it.copy(
                            isTtsInitializing = false,
                            isTtsReady = false,
                            ttsError = buildString {
                                append("无法启动语音引擎。")
                                if (tried.isNotEmpty()) {
                                    append("已尝试：${tried.joinToString()}。")
                                }
                                if (!defaultEngine.isNullOrBlank()) {
                                    append("系统默认：$defaultEngine。")
                                }
                                if (engines.isNotEmpty()) {
                                    append("已安装：${engines.joinToString()}。")
                                }
                                append("请到 设置 → 更多设置 → 无障碍 → 文字转语音输出，确认默认引擎能试听中文。")
                            },
                        )
                    }
                    return@launch
                }
                ctrl
            }
            tts.setSpeechRate(_uiState.value.speechRate)
            tts.setPitch(_uiState.value.speechPitch)
            playlistManager.syncLoopToController()
            tts.applyVoicePreference(_uiState.value.voicePreference)
            _uiState.value.selectedVoiceId?.let { tts.applySelectedVoice(it) }
            controller = tts
            refreshVoiceOptions(tts)
            syncFromSession(TtsPlaybackManager.session.value)
            _uiState.value.note?.content?.let { content ->
                TtsPlaybackManager.refreshPresynthForNote(content)
            }
            startPresynthProgressCollection()
            diagnosticLog.i("Reader", "initTts ready backend=${tts.speechBackend()}")
            _uiState.update {
                it.copy(
                    isTtsInitializing = false,
                    isTtsReady = true,
                    ttsError = null,
                )
            }
        }
    }

    private fun refreshVoiceOptions(tts: com.andriod.reader.service.TtsController) {
        val options = tts.listVoiceOptions()
        val backend = tts.speechBackend()
        val activeId = when (backend) {
            TtsSpeechBackend.ONLINE_EDGE -> settingsStore.getEdgeTtsVoiceId()
            TtsSpeechBackend.OFFLINE_SHERPA -> "sherpa-vits-zh"
            TtsSpeechBackend.SYSTEM ->
                tts.diagnostics().voiceName?.takeIf { id -> options.any { it.id == id } }
                    ?: _uiState.value.selectedVoiceId
        }
        val diag = tts.diagnostics()
        val tier = tts.voiceQualityTier()
        val hint = if (TtsVoiceQuality.needsQualityHint(tier)) {
            TtsVoiceQuality.qualityGuide(tier, diag.googleTtsInstalled)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                voiceOptions = options,
                selectedVoiceId = activeId,
                speechBackend = backend,
                ttsQualityHint = hint,
                sherpaModelInstalled = TtsPlaybackManager.sherpaModelInstalled() ||
                    sherpaDownloadCoordinator.isInstalled(),
            )
        }
    }

    fun downloadSherpaModel() {
        if (_uiState.value.isDownloadingSherpaModel) return
        if (!NetworkAvailability.isConnected(context)) {
            _uiState.update { it.copy(sherpaDownloadSnackbar = "无网络，无法下载") }
            return
        }
        diagnosticLog.i("Reader", "sherpa model download started")
        _uiState.update {
            it.copy(
                isDownloadingSherpaModel = true,
                sherpaDownloadPhase = SherpaDownloadPhase.Downloading,
                sherpaDownloadProgress = null,
                sherpaDownloadBytesLabel = null,
                sherpaDownloadHint = "正在下载离线语音包…",
                sherpaDownloadSnackbar = "开始下载离线语音包（约 50 MB）…",
            )
        }
        viewModelScope.launch {
            val result = sherpaDownloadCoordinator.download { progress ->
                updateSherpaDownloadProgressUi(progress)
            }
            result.fold(
                onSuccess = {
                    diagnosticLog.i("Reader", "sherpa model download success")
                },
                onFailure = { error ->
                    diagnosticLog.e("Reader", "sherpa model download failed: ${error.message}", error)
                },
            )
            _uiState.value.note?.content?.let { TtsPlaybackManager.refreshPresynthForNote(it) }
            _uiState.update {
                it.copy(
                    isDownloadingSherpaModel = false,
                    sherpaModelInstalled = sherpaDownloadCoordinator.isInstalled(),
                    sherpaDownloadPhase = SherpaDownloadPhase.Idle,
                    sherpaDownloadProgress = null,
                    sherpaDownloadBytesLabel = null,
                    sherpaDownloadHint = result.fold(
                        onSuccess = { "离线语音包已就绪" },
                        onFailure = { error -> error.message ?: "下载失败" },
                    ),
                    sherpaDownloadSnackbar = result.fold(
                        onSuccess = { "离线语音包已下载" },
                        onFailure = { error -> "下载失败：${error.message ?: "未知错误"}" },
                    ),
                )
            }
        }
    }

    private fun updateSherpaDownloadProgressUi(progress: SherpaDownloadProgress) {
        val snapshot = sherpaDownloadCoordinator.uiSnapshot(progress)
        _uiState.update {
            it.copy(
                sherpaDownloadPhase = snapshot.phase,
                sherpaDownloadProgress = snapshot.progress,
                sherpaDownloadBytesLabel = snapshot.bytesLabel,
                sherpaDownloadHint = snapshot.hint,
            )
        }
    }

    fun clearSherpaDownloadSnackbar() {
        _uiState.update { it.copy(sherpaDownloadSnackbar = null) }
    }

    fun onSpeechRateChange(rate: Float) {
        settingsStore.saveDefaultSpeechRate(rate)
        controller?.setSpeechRate(rate)
        _uiState.value.note?.content?.let { TtsPlaybackManager.refreshPresynthForNote(it) }
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun onSpeechPitchChange(pitch: Float) {
        settingsStore.saveDefaultSpeechPitch(pitch)
        controller?.setPitch(pitch)
        _uiState.value.note?.content?.let { TtsPlaybackManager.refreshPresynthForNote(it) }
        _uiState.update { it.copy(speechPitch = pitch) }
    }

    fun openTtsSettings() {
        _uiState.update { it.copy(ttsSettingsVisible = true) }
    }

    fun closeTtsSettings() {
        _uiState.update { it.copy(ttsSettingsVisible = false, voicePickerExpanded = false) }
    }

    fun openSleepTimer() {
        val note = _uiState.value.note
        val sliderMinutes = TtsPlaybackManager.initialSleepTimerSliderMinutes(context).toFloat()
        _uiState.update {
            it.copy(
                sleepTimerVisible = true,
                sleepTimerSliderMinutes = sliderMinutes,
                estimatedNoteRemainingMinutes = TtsPlaybackManager.estimateNoteRemainingMinutes(
                    noteContent = note?.content,
                    speechRate = it.speechRate,
                ),
                canScheduleAfterNoteEnd = TtsPlaybackManager.noteHasReadableContent(note?.content),
                lastSleepTimerPresetSubtitle = settingsStore.getLastSleepTimerPreset().displaySubtitle(),
            )
        }
    }

    fun closeSleepTimer() {
        val rounded = _uiState.value.sleepTimerSliderMinutes.roundToInt().coerceIn(0, 90)
        if (SleepTimerPresetPolicy.shouldPersistOnSheetClose(rounded)) {
            settingsStore.saveLastSleepTimerPreset(LastSleepTimerPreset.FixedMinutes(rounded))
        }
        _uiState.update { it.copy(sleepTimerVisible = false) }
    }

    fun onSleepTimerSliderFinished(minutes: Float) {
        val rounded = minutes.roundToInt().coerceIn(0, 90)
        viewModelScope.launch {
            _uiState.update { it.copy(sleepTimerSliderMinutes = rounded.toFloat()) }
            TtsPlaybackManager.setSleepTimerMinutes(context, rounded)
        }
    }

    fun applySleepTimerAfterNoteEnd() {
        TtsPlaybackManager.setSleepTimerAfterNoteEnd(context)
        _uiState.update {
            it.copy(
                sleepTimerVisible = false,
                sleepTimerSliderMinutes = 0f,
                lastSleepTimerPresetSubtitle = settingsStore.getLastSleepTimerPreset().displaySubtitle(),
            )
        }
    }

    fun applyLastSleepTimerPreset() {
        val preset = TtsPlaybackManager.applyLastSleepTimerPreset(context)
        _uiState.update {
            it.copy(
                sleepTimerVisible = false,
                sleepTimerSliderMinutes = when (preset) {
                    is LastSleepTimerPreset.FixedMinutes -> preset.minutes.toFloat()
                    LastSleepTimerPreset.AfterNoteEnd -> it.sleepTimerSliderMinutes
                },
                lastSleepTimerPresetSubtitle = preset.displaySubtitle(),
            )
        }
    }

    fun cycleRepeatMode() {
        playlistManager.cycleRepeatMode()
    }

    fun setQueueRepeatMode(mode: TtsQueueRepeatMode) {
        playlistManager.setRepeatMode(mode)
    }

    fun addCurrentNoteToQueue() {
        val note = _uiState.value.note ?: return
        playlistManager.add(note.fileName, note.title)
    }

    fun removeFromQueue(fileName: String) {
        playlistManager.remove(fileName)
    }

    fun clearQueue() {
        playlistManager.clear()
    }

    fun playQueueItem(fileName: String) {
        val host = lastHostContext ?: return
        playlistManager.playItem(host, fileName)
    }

    fun onVoicePickerExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(voicePickerExpanded = expanded) }
    }

    fun onVoiceSelected(voiceId: String) {
        settingsStore.saveSelectedVoiceId(voiceId)
        if (_uiState.value.speechBackend == TtsSpeechBackend.ONLINE_EDGE) {
            settingsStore.saveEdgeTtsVoiceId(voiceId)
        }
        controller?.applySelectedVoice(voiceId)
        _uiState.value.note?.content?.let { TtsPlaybackManager.refreshPresynthForNote(it) }
        _uiState.update {
            it.copy(selectedVoiceId = voiceId, voicePickerExpanded = false)
        }
    }

    fun onVoicePreferenceChange(preference: TtsVoicePreference) {
        settingsStore.saveVoicePreference(preference.name)
        controller?.applyVoicePreference(preference)
        controller?.let { refreshVoiceOptions(it) }
        _uiState.update { it.copy(voicePreference = preference) }
    }

    fun onSpeechBackendChange(backend: TtsSpeechBackend) {
        viewModelScope.launch {
            diagnosticLog.i("Reader", "speechBackendChange -> $backend")
            settingsStore.saveTtsSpeechBackend(backend)
            controller?.applySpeechBackend(backend)
            val wasPlaying = TtsPlaybackManager.session.value.hasActiveSession
            TtsPlaybackManager.reinitialize(context)
            attachUiCallbacks()
            val tts = TtsPlaybackManager.awaitReady(context)
            controller = tts
            tts.setSpeechRate(_uiState.value.speechRate)
            tts.setPitch(_uiState.value.speechPitch)
            refreshVoiceOptions(tts)
            _uiState.value.note?.content?.let { TtsPlaybackManager.refreshPresynthForNote(it) }
            startPresynthProgressCollection()
            _uiState.update {
                it.copy(
                    speechBackend = backend,
                    ttsError = if (wasPlaying) "已切换朗读引擎（已停止当前朗读）" else it.ttsError,
                )
            }
        }
    }

    fun refreshNotificationPermissionState() {
        if (!NotificationPermission.hasPermission(context)) return
        _uiState.update {
            it.copy(
                notificationPermissionDenied = false,
                ttsError = if (it.ttsError == NOTIFICATION_PERMISSION_MESSAGE) null else it.ttsError,
            )
        }
    }

    fun openNotificationSettings() {
        NotificationPermission.openAppNotificationSettings(context)
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update {
                it.copy(
                    notificationPermissionDenied = false,
                    ttsError = if (it.ttsError == NOTIFICATION_PERMISSION_MESSAGE) null else it.ttsError,
                )
            }
            if (pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                executePendingStartPlayback(withForegroundService = true)
            }
            return
        }
        _uiState.update {
            it.copy(
                notificationPermissionDenied = true,
                ttsError = NOTIFICATION_PERMISSION_MESSAGE,
            )
        }
        if (pendingStartAfterPermission) {
            pendingStartAfterPermission = false
            executePendingStartPlayback(withForegroundService = false)
        }
    }

    private fun needsNotificationPermissionForNewPlayback(): Boolean {
        return NotificationPermission.isRequired() &&
            !NotificationPermission.hasPermission(context)
    }

    private fun executePendingStartPlayback(withForegroundService: Boolean) {
        val host = lastHostContext ?: return
        val note = _uiState.value.note ?: return
        if (note.content.isBlank()) return
        TtsPlaybackManager.startPlayback(
            context = host,
            fileName = note.fileName,
            title = note.title,
            content = note.content,
            withForegroundService = withForegroundService,
        )
    }

    fun togglePlayPause() {
        val host = lastHostContext
        val state = _uiState.value
        diagnosticLog.d(
            "Reader",
            "togglePlayPause ready=${state.isTtsReady} playing=${state.isPlaying} file=${state.note?.fileName}",
        )
        if (host == null || controller == null || !state.isTtsReady) {
            _uiState.update { it.copy(ttsError = it.ttsError ?: "语音引擎尚未就绪") }
            host?.let { initTts(it) }
            return
        }
        val session = TtsPlaybackManager.session.value
        val note = state.note
        if (note == null) {
            _uiState.update { it.copy(ttsError = "笔记不存在，无法朗读") }
            return
        }
        _uiState.update { it.copy(ttsError = null) }

        if (session.hasActiveSession && session.fileName != note.fileName) {
            if (session.isPlaying) {
                controller?.pause()
            } else {
                controller?.resume()
            }
            return
        }

        if (state.isPlaying) {
            controller?.pause()
        } else if (state.presynthState == TtsPresynthUiState.Preparing) {
            _uiState.update { it.copy(ttsError = "正在生成语音，请稍候…") }
        } else if (!session.hasActiveSession || session.fileName != note.fileName) {
            if (note.content.isBlank()) {
                _uiState.update { it.copy(ttsError = "笔记内容为空") }
                return
            }
            TtsPlaybackManager.startPlayback(host, note.fileName, note.title, note.content)
        } else {
            controller?.resume()
        }
    }

    fun onPlayPauseClicked(requestNotificationPermission: () -> Unit) {
        val state = _uiState.value
        val session = TtsPlaybackManager.session.value
        val note = state.note
        val startingNewPlayback = !state.isPlaying &&
            (!session.hasActiveSession || session.fileName != note?.fileName)

        if (startingNewPlayback && needsNotificationPermissionForNewPlayback()) {
            pendingStartAfterPermission = true
            requestNotificationPermission()
            return
        }
        togglePlayPause()
    }

    fun stop() = TtsPlaybackManager.stopPlayback()

    fun nextSegment() = controller?.nextSegment()

    fun detachTtsUi() {
        TtsPlaybackManager.detachUiCallbacks()
        controller = null
    }

    fun prepareForEdit(): Boolean {
        val note = _uiState.value.note ?: return false
        val session = TtsPlaybackManager.session.value
        if (session.hasActiveSession && session.fileName == note.fileName) {
            TtsPlaybackManager.stopPlayback()
        }
        return true
    }

    fun onPresynthClick() {
        val note = _uiState.value.note ?: return
        val state = _uiState.value.presynthState
        when (state) {
            TtsPresynthUiState.Preparing -> TtsPlaybackManager.cancelPresynth()
            TtsPresynthUiState.Ready -> {
                TtsPlaybackManager.preparePresynth(note.content, forceRegenerate = true)
            }
            TtsPresynthUiState.Stale,
            TtsPresynthUiState.Failed,
            TtsPresynthUiState.NotPrepared,
            -> TtsPlaybackManager.preparePresynth(note.content, forceRegenerate = state != TtsPresynthUiState.NotPrepared)
            TtsPresynthUiState.Hidden -> Unit
        }
    }

    fun onPresynthCancel() {
        TtsPlaybackManager.cancelPresynth()
    }

    fun refreshNote() {
        val refreshed = noteRepository.getNote(fileName) ?: return
        val blocks = practiceRepository.parseBlocks(refreshed.content, refreshed.fileName)
        TtsPlaybackManager.refreshPresynthForNote(refreshed.content)
        _uiState.update { current ->
            current.copy(
                note = refreshed,
                blocks = blocks,
                todayPractice = practiceRepository.getPeriodEntriesForBlocks(refreshed.fileName, blocks),
                practiceMeta = practiceRepository.getDisplayMetaForBlocks(refreshed.fileName, blocks),
            )
        }
    }

    fun onTrackableBlockClick(block: NoteBlock) {
        if (!block.trackable) return
        val note = _uiState.value.note ?: return
        val callout = block as? NoteBlock.Callout
        val history = practiceRepository.getBlockHistory(note.fileName, block.id)
        val hasPeriodEntry = callout?.let {
            practiceRepository.hasAnyEntryInPeriod(
                note.fileName,
                block.id,
                it.mode,
                it.repeatPeriod,
            )
        } ?: false
        _uiState.update {
            it.copy(
                practiceSheet = PracticeSheetState(
                    blockId = block.id,
                    blockLabel = block.displayLabel(),
                    mode = callout?.mode ?: PracticeMode.REPEATLY,
                    repeatPeriod = callout?.repeatPeriod ?: RepeatPeriod.DAY,
                    hasPeriodEntry = hasPeriodEntry,
                    history = history,
                ),
            )
        }
    }

    fun dismissPracticeSheet() {
        _uiState.update { it.copy(practiceSheet = null) }
    }

    fun savePractice(event: PracticeEvent, note: String) {
        val sheet = _uiState.value.practiceSheet ?: return
        val file = _uiState.value.note?.fileName ?: return
        practiceRepository.appendEntry(
            fileName = file,
            blockId = sheet.blockId,
            event = event,
            note = note,
        )
        _uiState.update {
            it.copy(
                practiceSheet = null,
                todayPractice = practiceRepository.getPeriodEntriesForBlocks(file, it.blocks),
                practiceMeta = practiceRepository.getDisplayMetaForBlocks(file, it.blocks),
            )
        }
    }

    fun clearPracticeToday() {
        val sheet = _uiState.value.practiceSheet ?: return
        val file = _uiState.value.note?.fileName ?: return
        practiceRepository.clearPeriodEntry(
            fileName = file,
            blockId = sheet.blockId,
            mode = sheet.mode,
            repeatPeriod = sheet.repeatPeriod,
        )
        _uiState.update {
            it.copy(
                practiceSheet = null,
                todayPractice = practiceRepository.getPeriodEntriesForBlocks(file, it.blocks),
                practiceMeta = practiceRepository.getDisplayMetaForBlocks(file, it.blocks),
            )
        }
    }

    fun updatePracticeEntryNote(recordedAt: Instant, note: String) {
        val sheet = _uiState.value.practiceSheet ?: return
        val file = _uiState.value.note?.fileName ?: return
        if (!practiceRepository.updateEntryNote(file, sheet.blockId, recordedAt, note)) return
        val history = practiceRepository.getBlockHistory(file, sheet.blockId)
        _uiState.update {
            it.copy(
                practiceSheet = sheet.copy(history = history),
                todayPractice = practiceRepository.getPeriodEntriesForBlocks(file, it.blocks),
                practiceMeta = practiceRepository.getDisplayMetaForBlocks(file, it.blocks),
            )
        }
    }

    fun practiceCalendarRouteArgs(): String? {
        val sheet = _uiState.value.practiceSheet ?: return null
        val file = _uiState.value.note?.fileName ?: return null
        return Routes.practiceCalendar(
            fileName = file,
            blockId = sheet.blockId,
            blockLabel = sheet.blockLabel,
            mode = sheet.mode,
            repeatPeriod = sheet.repeatPeriod,
        )
    }

    private fun loadInitialState(): ReaderUiState {
        val note = noteRepository.getNote(fileName)
        val blocks = note?.let { practiceRepository.parseBlocks(it.content, it.fileName) }.orEmpty()
        val todayPractice = note?.let {
            practiceRepository.getPeriodEntriesForBlocks(it.fileName, blocks)
        }.orEmpty()
        val practiceMeta = note?.let {
            practiceRepository.getDisplayMetaForBlocks(it.fileName, blocks)
        }.orEmpty()
        return ReaderUiState(
            note = note,
            blocks = blocks,
            todayPractice = todayPractice,
            practiceMeta = practiceMeta,
            speechRate = settingsStore.getDefaultSpeechRate(),
            speechPitch = settingsStore.getDefaultSpeechPitch(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
            selectedVoiceId = settingsStore.getSelectedVoiceId(),
            voicePreference = readVoicePreference(),
            speechBackend = settingsStore.getTtsSpeechBackend(),
            queueRepeatMode = playlistManager.state.value.repeatMode,
            queueCount = playlistManager.state.value.items.size,
            noteInQueue = note?.let { n ->
                playlistManager.state.value.items.any { it.fileName == n.fileName }
            } ?: false,
            canSelectRepeatAll = playlistManager.state.value.items.isNotEmpty(),
            lastSleepTimerPresetSubtitle = settingsStore.getLastSleepTimerPreset().displaySubtitle(),
            sherpaModelInstalled = sherpaDownloadCoordinator.isInstalled(),
        )
    }
}

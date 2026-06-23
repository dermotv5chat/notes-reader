package com.andriod.reader.ui.reader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.data.repository.PracticeRepository
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.service.LastSleepTimerPreset
import com.andriod.reader.service.SleepTimerPresetPolicy
import com.andriod.reader.service.SleepTimerMode
import com.andriod.reader.service.TtsHelper
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.service.TtsPlaybackSession
import com.andriod.reader.ui.NavArgs
import com.andriod.reader.util.NotificationPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
    val isTtsReady: Boolean = false,
    val isTtsInitializing: Boolean = false,
    val ttsError: String? = null,
    val loopEnabled: Boolean = false,
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
    val blocks: List<NoteBlock> = emptyList(),
    val todayPractice: Map<String, PracticeDayEntry> = emptyMap(),
    val practiceSheet: PracticeSheetState? = null,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val practiceRepository: PracticeRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val fileName: String = NavArgs.decodeFileName(savedStateHandle.get<String>("fileName"))

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var controller: com.andriod.reader.service.TtsController? = null
    private var lastHostContext: Context? = null
    private var pendingStartAfterPermission = false

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
                _uiState.update { it.copy(ttsError = message, isPlaying = false) }
            },
        )
    }

    fun initTts(hostContext: Context) {
        lastHostContext = hostContext
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
            tts.setLoopEnabled(_uiState.value.loopEnabled)
            tts.applyVoicePreference(_uiState.value.voicePreference)
            _uiState.value.selectedVoiceId?.let { tts.applySelectedVoice(it) }
            controller = tts
            refreshVoiceOptions(tts)
            syncFromSession(TtsPlaybackManager.session.value)
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
        val activeId = tts.diagnostics().voiceName?.takeIf { id -> options.any { it.id == id } }
            ?: _uiState.value.selectedVoiceId
        _uiState.update {
            it.copy(voiceOptions = options, selectedVoiceId = activeId)
        }
    }

    fun onSpeechRateChange(rate: Float) {
        settingsStore.saveDefaultSpeechRate(rate)
        controller?.setSpeechRate(rate)
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun onSpeechPitchChange(pitch: Float) {
        settingsStore.saveDefaultSpeechPitch(pitch)
        controller?.setPitch(pitch)
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

    fun toggleLoop() {
        val enabled = !_uiState.value.loopEnabled
        settingsStore.saveLoopPlayback(enabled)
        controller?.setLoopEnabled(enabled)
        _uiState.update { it.copy(loopEnabled = enabled) }
    }

    fun onVoicePickerExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(voicePickerExpanded = expanded) }
    }

    fun onVoiceSelected(voiceId: String) {
        settingsStore.saveSelectedVoiceId(voiceId)
        controller?.applySelectedVoice(voiceId)
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

    fun refreshNote() {
        val refreshed = noteRepository.getNote(fileName) ?: return
        _uiState.update { current ->
            current.copy(
                note = refreshed,
                blocks = practiceRepository.parseBlocks(refreshed.content, refreshed.fileName),
                todayPractice = practiceRepository.getTodayEntriesForNote(refreshed.fileName),
            )
        }
    }

    fun onTrackableBlockClick(block: NoteBlock) {
        if (!block.trackable) return
        val note = _uiState.value.note ?: return
        val history = practiceRepository.getBlockHistory(note.fileName, block.id)
        val hasTodayEntry = practiceRepository.hasAnyEntryOnDate(note.fileName, block.id)
        _uiState.update {
            it.copy(
                practiceSheet = PracticeSheetState(
                    blockId = block.id,
                    blockLabel = block.displayLabel(),
                    hasTodayEntry = hasTodayEntry,
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
                todayPractice = practiceRepository.getTodayEntriesForNote(file),
            )
        }
    }

    fun clearPracticeToday() {
        val sheet = _uiState.value.practiceSheet ?: return
        val file = _uiState.value.note?.fileName ?: return
        practiceRepository.clearTodayEntry(file, sheet.blockId)
        _uiState.update {
            it.copy(
                practiceSheet = null,
                todayPractice = practiceRepository.getTodayEntriesForNote(file),
            )
        }
    }

    private fun loadInitialState(): ReaderUiState {
        val note = noteRepository.getNote(fileName)
        val blocks = note?.let { practiceRepository.parseBlocks(it.content, it.fileName) }.orEmpty()
        val todayPractice = note?.let { practiceRepository.getTodayEntriesForNote(it.fileName) }.orEmpty()
        return ReaderUiState(
            note = note,
            blocks = blocks,
            todayPractice = todayPractice,
            speechRate = settingsStore.getDefaultSpeechRate(),
            speechPitch = settingsStore.getDefaultSpeechPitch(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
            selectedVoiceId = settingsStore.getSelectedVoiceId(),
            voicePreference = readVoicePreference(),
            loopEnabled = settingsStore.isLoopPlaybackEnabled(),
            lastSleepTimerPresetSubtitle = settingsStore.getLastSleepTimerPreset().displaySubtitle(),
        )
    }
}

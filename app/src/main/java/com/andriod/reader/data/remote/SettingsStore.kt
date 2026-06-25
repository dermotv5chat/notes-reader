package com.andriod.reader.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.andriod.reader.domain.GitHubSettings
import com.andriod.reader.domain.MuyuSoundPreset
import com.andriod.reader.service.LastSleepTimerPreset
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.service.synthesis.SherpaModelCatalog
import com.andriod.reader.ui.theme.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "reader_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _appThemeMode = MutableStateFlow(readAppThemeMode())
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun getGitHubSettings(): GitHubSettings = GitHubSettings(
        owner = prefs.getString(KEY_OWNER, "dermotv5chat") ?: "dermotv5chat",
        repo = prefs.getString(KEY_REPO, "notes") ?: "notes",
    )

    fun saveGitHubSettings(settings: GitHubSettings) {
        prefs.edit()
            .putString(KEY_OWNER, settings.owner)
            .putString(KEY_REPO, settings.repo)
            .apply()
    }

    fun getDefaultSpeechRate(): Float = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)

    fun saveDefaultSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }

    fun getDefaultSpeechPitch(): Float = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)

    fun saveDefaultSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
    }

    fun getSelectedVoiceId(): String? = prefs.getString(KEY_SELECTED_VOICE, null)?.takeIf { it.isNotBlank() }

    fun saveSelectedVoiceId(voiceId: String?) {
        prefs.edit().putString(KEY_SELECTED_VOICE, voiceId?.trim()).apply()
    }

    fun getVoicePreference(): String = prefs.getString(KEY_VOICE_PREFERENCE, "AUTO") ?: "AUTO"

    fun saveVoicePreference(preference: String) {
        prefs.edit().putString(KEY_VOICE_PREFERENCE, preference).apply()
    }

    fun isKeepScreenOn(): Boolean = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun getAppThemeMode(): AppThemeMode = readAppThemeMode()

    fun getTtsSpeechBackend(): TtsSpeechBackend = runCatching {
        TtsSpeechBackend.valueOf(
            prefs.getString(KEY_TTS_SPEECH_BACKEND, TtsSpeechBackend.SYSTEM.name)
                ?: TtsSpeechBackend.SYSTEM.name,
        )
    }.getOrDefault(TtsSpeechBackend.SYSTEM)

    fun saveTtsSpeechBackend(backend: TtsSpeechBackend) {
        prefs.edit().putString(KEY_TTS_SPEECH_BACKEND, backend.name).apply()
    }

    fun getEdgeTtsVoiceId(): String =
        prefs.getString(KEY_EDGE_TTS_VOICE, DEFAULT_EDGE_TTS_VOICE) ?: DEFAULT_EDGE_TTS_VOICE

    fun saveEdgeTtsVoiceId(voiceId: String) {
        prefs.edit().putString(KEY_EDGE_TTS_VOICE, voiceId.trim()).apply()
    }

    fun getSherpaModelPackId(): String =
        prefs.getString(KEY_SHERPA_MODEL_PACK, SherpaModelCatalog.MELO_ID)
            ?: SherpaModelCatalog.MELO_ID

    fun saveSherpaModelPackId(packId: String) {
        prefs.edit().putString(KEY_SHERPA_MODEL_PACK, packId.trim()).apply()
    }

    fun getSherpaSpeakerId(): Int = prefs.getInt(KEY_SHERPA_SPEAKER_ID, 0)

    fun saveSherpaSpeakerId(speakerId: Int) {
        prefs.edit().putInt(KEY_SHERPA_SPEAKER_ID, speakerId.coerceAtLeast(0)).apply()
    }

    fun saveAppThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_APP_THEME_MODE, mode.name).apply()
        _appThemeMode.value = mode
    }

    private fun readAppThemeMode(): AppThemeMode =
        AppThemeMode.fromStored(prefs.getString(KEY_APP_THEME_MODE, AppThemeMode.SYSTEM.name))

    fun isLoopPlaybackEnabled(): Boolean = prefs.getBoolean(KEY_LOOP_PLAYBACK, false)

    fun saveLoopPlayback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOOP_PLAYBACK, enabled).apply()
    }

    fun getLastSleepTimerPreset(): LastSleepTimerPreset {
        return LastSleepTimerPreset.fromStored(
            type = prefs.getString(KEY_LAST_SLEEP_TIMER_TYPE, LastSleepTimerPreset.TYPE_FIXED),
            minutes = prefs.getInt(
                KEY_LAST_SLEEP_TIMER_MINUTES,
                LastSleepTimerPreset.DEFAULT_MINUTES,
            ),
        )
    }

    fun isMuyuSoundEnabled(): Boolean =
        prefs.getBoolean(KEY_MUYU_SOUND_ENABLED, DEFAULT_MUYU_SOUND_ENABLED)

    fun setMuyuSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUYU_SOUND_ENABLED, enabled).apply()
    }

    fun isMuyuVibrationEnabled(): Boolean =
        prefs.getBoolean(KEY_MUYU_VIBRATION_ENABLED, DEFAULT_MUYU_VIBRATION_ENABLED)

    fun setMuyuVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUYU_VIBRATION_ENABLED, enabled).apply()
    }

    fun getMuyuSoundPreset(): MuyuSoundPreset =
        MuyuSoundPreset.fromStored(prefs.getString(KEY_MUYU_SOUND_PRESET, null))

    fun setMuyuSoundPreset(preset: MuyuSoundPreset) {
        prefs.edit().putString(KEY_MUYU_SOUND_PRESET, preset.storageKey).apply()
    }

    fun saveLastSleepTimerPreset(preset: LastSleepTimerPreset) {
        val editor = prefs.edit().putString(
            KEY_LAST_SLEEP_TIMER_TYPE,
            LastSleepTimerPreset.storedType(preset),
        )
        LastSleepTimerPreset.storedMinutes(preset)?.let { minutes ->
            editor.putInt(KEY_LAST_SLEEP_TIMER_MINUTES, minutes)
        }
        editor.apply()
    }

    companion object {
        private const val KEY_TOKEN = "github_token"
        private const val KEY_OWNER = "github_owner"
        private const val KEY_REPO = "github_repo"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_SELECTED_VOICE = "selected_voice_id"
        private const val KEY_VOICE_PREFERENCE = "voice_preference"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_LOOP_PLAYBACK = "loop_playback"
        private const val KEY_LAST_SLEEP_TIMER_TYPE = "last_sleep_timer_type"
        private const val KEY_LAST_SLEEP_TIMER_MINUTES = "last_sleep_timer_minutes"
        private const val KEY_TTS_SPEECH_BACKEND = "tts_speech_backend"
        private const val KEY_EDGE_TTS_VOICE = "edge_tts_voice"
        private const val KEY_SHERPA_MODEL_PACK = "sherpa_model_pack_id"
        private const val KEY_SHERPA_SPEAKER_ID = "sherpa_speaker_id"
        private const val KEY_MUYU_SOUND_ENABLED = "muyu_sound_enabled"
        private const val KEY_MUYU_VIBRATION_ENABLED = "muyu_vibration_enabled"
        private const val KEY_MUYU_SOUND_PRESET = "muyu_sound_preset"
        const val DEFAULT_EDGE_TTS_VOICE = "zh-CN-XiaoxiaoNeural"
        internal const val DEFAULT_MUYU_SOUND_ENABLED = true
        internal const val DEFAULT_MUYU_VIBRATION_ENABLED = true
    }
}

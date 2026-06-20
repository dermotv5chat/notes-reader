package com.andriod.reader.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.andriod.reader.domain.GitHubSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

    fun isLoopPlaybackEnabled(): Boolean = prefs.getBoolean(KEY_LOOP_PLAYBACK, false)

    fun saveLoopPlayback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOOP_PLAYBACK, enabled).apply()
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
        private const val KEY_LOOP_PLAYBACK = "loop_playback"
    }
}

package com.andriod.reader.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

object TtsHelper {
    const val GOOGLE_TTS_ENGINE = "com.google.android.tts"

    data class TtsDiagnostics(
        val enginePackage: String,
        val engineLabel: String,
        val voiceName: String?,
        val voiceLocale: String?,
        val voiceQuality: Int?,
        val chineseVoiceCount: Int,
        val isGoogleEngine: Boolean,
        val googleTtsInstalled: Boolean,
        val isLanguageFallback: Boolean,
        val isOnlineVoice: Boolean,
        val recommendation: String,
    )

    data class VoiceSetup(
        val voice: Voice?,
        val chineseVoiceCount: Int,
        val languageFallback: Boolean,
    )

    fun isGoogleTtsInstalled(context: Context): Boolean =
        listInstalledEngines(context).contains(GOOGLE_TTS_ENGINE)

    fun listInstalledEngines(context: Context): List<String> {
        val pm = context.packageManager
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        return pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo -> resolveInfo.serviceInfo?.packageName }
            .distinct()
            .sorted()
    }

    fun defaultEnginePackage(context: Context): String? =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
            ?.takeIf { it.isNotBlank() }

    /**
     * null = let Android pick the system default engine (most reliable on MIUI / vendor ROMs).
     */
    fun engineTryOrder(context: Context): List<String?> {
        val installed = listInstalledEngines(context)
        val defaultEngine = defaultEnginePackage(context)
        val order = linkedSetOf<String?>()

        order.add(null)
        if (!defaultEngine.isNullOrBlank()) {
            order.add(defaultEngine)
        }
        installed
            .filter { it != defaultEngine && it != GOOGLE_TTS_ENGINE }
            .forEach { order.add(it) }
        if (GOOGLE_TTS_ENGINE in installed) {
            order.add(GOOGLE_TTS_ENGINE)
        }
        return order.toList()
    }

    fun engineLabel(enginePackage: String?): String =
        when {
            enginePackage.isNullOrBlank() -> "系统默认"
            else -> enginePackage
        }

    fun createTextToSpeech(
        context: Context,
        listener: TextToSpeech.OnInitListener,
        enginePackage: String?,
    ): TextToSpeech {
        return if (enginePackage != null) {
            TextToSpeech(context, listener, enginePackage)
        } else {
            TextToSpeech(context, listener)
        }
    }

    fun setupChineseVoice(
        engine: TextToSpeech,
        preferredVoiceName: String? = null,
        preference: TtsVoicePreference = TtsVoicePreference.AUTO,
    ): VoiceSetup {
        val chineseVoices = listChineseVoices(engine)
        val validPreferred = preferredVoiceName?.takeIf { name ->
            chineseVoices.any { it.name == name }
        }
        val selected = resolveVoice(chineseVoices, validPreferred, preference)
        if (selected != null && engine.setVoice(selected) == TextToSpeech.SUCCESS) {
            return VoiceSetup(voice = selected, chineseVoiceCount = chineseVoices.size, languageFallback = false)
        }

        val langResult = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
        val languageOk = langResult >= TextToSpeech.LANG_AVAILABLE

        return VoiceSetup(
            voice = engine.voice?.takeIf { isChineseVoice(it) },
            chineseVoiceCount = chineseVoices.size,
            languageFallback = languageOk && selected == null,
        )
    }

    fun listChineseVoices(engine: TextToSpeech): List<Voice> {
        val voices = engine.voices ?: emptySet()
        return voices.filter { isChineseVoice(it) }
    }

    fun toVoiceOptions(voices: List<Voice>, preference: TtsVoicePreference): List<TtsVoiceOption> {
        return voices
            .sortedWith(compareByDescending<Voice> { sortScore(it, preference) }.thenBy { it.name })
            .map { voice ->
                TtsVoiceOption(
                    id = voice.name,
                    label = voiceLabel(voice),
                    isOnline = voice.isNetworkConnectionRequired,
                )
            }
    }

    fun resolveVoice(
        voices: List<Voice>,
        preferredVoiceName: String?,
        preference: TtsVoicePreference,
    ): Voice? {
        if (preferredVoiceName != null) {
            voices.find { it.name == preferredVoiceName }?.let { return it }
        }
        return voices.maxByOrNull { sortScore(it, preference) }
    }

    fun isChineseVoice(voice: Voice): Boolean {
        val lang = voice.locale.language.lowercase(Locale.US)
        val tag = voice.locale.toLanguageTag().lowercase(Locale.US)
        val name = voice.name.lowercase(Locale.US)
        return lang == "zh" ||
            lang == "cmn" ||
            lang == "yue" ||
            tag.startsWith("zh") ||
            tag.contains("cmn") ||
            name.contains("zh-cn") ||
            name.contains("zh_cn") ||
            name.contains("cmn-cn") ||
            name.contains("cmn_cn")
    }

    fun getDiagnostics(
        context: Context,
        engine: TextToSpeech?,
        activeEnginePackage: String? = null,
        preferredVoiceName: String? = null,
        preference: TtsVoicePreference = TtsVoicePreference.AUTO,
    ): TtsDiagnostics {
        val googleInstalled = isGoogleTtsInstalled(context)
        if (engine == null) {
            return TtsDiagnostics(
                enginePackage = "未初始化",
                engineLabel = "未初始化",
                voiceName = null,
                voiceLocale = null,
                voiceQuality = null,
                chineseVoiceCount = 0,
                isGoogleEngine = false,
                googleTtsInstalled = googleInstalled,
                isLanguageFallback = false,
                isOnlineVoice = false,
                recommendation = recommendation(googleInstalled, false, VoiceSetup(null, 0, false), preference),
            )
        }

        val setup = setupChineseVoice(engine, preferredVoiceName, preference)
        val currentVoice = setup.voice ?: engine.voice?.takeIf { isChineseVoice(it) }
        val enginePackage = engine.defaultEngine ?: activeEnginePackage ?: "未知"
        val isGoogle = enginePackage == GOOGLE_TTS_ENGINE

        return TtsDiagnostics(
            enginePackage = enginePackage,
            engineLabel = engineLabel(context, enginePackage),
            voiceName = currentVoice?.name ?: if (setup.languageFallback) "中文（语言模式）" else null,
            voiceLocale = currentVoice?.locale?.toLanguageTag(),
            voiceQuality = currentVoice?.quality,
            chineseVoiceCount = setup.chineseVoiceCount,
            isGoogleEngine = isGoogle,
            googleTtsInstalled = googleInstalled,
            isLanguageFallback = setup.languageFallback && currentVoice == null,
            isOnlineVoice = currentVoice?.isNetworkConnectionRequired == true,
            recommendation = recommendation(googleInstalled, isGoogle, setup, preference),
        )
    }

    suspend fun awaitEngineReady(
        initDeferred: CompletableDeferred<Boolean>,
        timeoutMs: Long = 5000,
    ): Boolean = withTimeoutOrNull(timeoutMs) {
        initDeferred.await()
    } ?: false

    fun openTtsSettings(context: Context) {
        val intent = Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun openGoogleTtsInPlayStore(context: Context) {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$GOOGLE_TTS_ENGINE"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (marketIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(marketIntent)
        } else {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_TTS_ENGINE"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }

    fun voiceLabel(voice: Voice): String {
        val type = when {
            voice.isNetworkConnectionRequired -> "在线"
            voice.name.contains("local", ignoreCase = true) -> "离线"
            else -> "本地"
        }
        return "$type · ${voice.name}"
    }

    private fun sortScore(voice: Voice, preference: TtsVoicePreference): Int {
        var score = baseVoiceScore(voice)
        when (preference) {
            TtsVoicePreference.PREFER_LOCAL -> {
                if (!voice.isNetworkConnectionRequired) score += 1_500
                if (voice.name.contains("local", ignoreCase = true)) score += 500
            }
            TtsVoicePreference.PREFER_ONLINE -> {
                if (voice.isNetworkConnectionRequired) score += 1_500
            }
            TtsVoicePreference.AUTO -> {
                if (!voice.isNetworkConnectionRequired) score += 600
            }
        }
        return score
    }

    private fun baseVoiceScore(voice: Voice): Int {
        var score = voice.quality
        val tag = voice.locale.toLanguageTag().lowercase(Locale.US)
        val name = voice.name.lowercase(Locale.US)
        if (tag.contains("zh-cn") || tag.contains("cmn-cn") || tag == "zh-cn") score += 1_000
        if (isChineseVoice(voice)) score += 300
        when {
            name.contains("neural") -> score += 800
            name.contains("wavenet") -> score += 700
            name.contains("network") || name.contains("online") -> score += 400
            name.contains("local") -> score += 300
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && voice.quality >= Voice.QUALITY_VERY_HIGH) {
            score += 400
        } else if (voice.quality >= Voice.QUALITY_HIGH) {
            score += 200
        }
        return score
    }

    private fun engineLabel(context: Context, packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private fun recommendation(
        googleInstalled: Boolean,
        isGoogleEngine: Boolean,
        setup: VoiceSetup,
        preference: TtsVoicePreference,
    ): String {
        return when {
            setup.chineseVoiceCount > 0 && setup.voice != null ->
                buildString {
                    append("已识别 ${setup.chineseVoiceCount} 个中文语音，可在阅读页选择。")
                    if (setup.voice.isNetworkConnectionRequired) {
                        append("当前为在线语音，需联网。")
                    }
                }
            setup.chineseVoiceCount > 0 ->
                "已识别 ${setup.chineseVoiceCount} 个中文语音，请在阅读页手动选择。"
            setup.languageFallback ->
                "将使用系统默认中文语音朗读。"
            googleInstalled && isGoogleEngine ->
                "使用 Google 引擎。若效果不好，可在系统设置换成本机自带引擎。"
            else ->
                "使用系统自带语音引擎。若无法朗读，请到系统「文字转语音输出」试听并确认中文可用。"
        }
    }
}

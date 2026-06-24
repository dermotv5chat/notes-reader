package com.andriod.reader.service.synthesis

data class SherpaModelPack(
    val id: String,
    val displayName: String,
    val genderLabel: String?,
    val archiveUrl: String,
    val dirName: String,
    val archiveFileName: String,
    val modelFileName: String = "model.onnx",
    val tokensFileName: String = "tokens.txt",
    val lexiconFileName: String = "lexicon.txt",
    val ruleFsts: List<String> = emptyList(),
    val ruleFars: String = "",
    val speakerCount: Int,
    val defaultSid: Int = 0,
    val estimatedSizeMb: Int,
    val speakerLabels: List<String>,
) {
    fun dropdownLabel(installed: Boolean): String = buildString {
        append(displayName)
        genderLabel?.let { append(" · $it") }
        append(if (installed) " · 已下载" else " · 约 ${estimatedSizeMb} MB")
    }

    fun speakerLabel(sid: Int): String =
        speakerLabels.getOrElse(sid) { "音色 ${sid + 1}" }
}

object SherpaModelCatalog {
    const val MELO_ID = "melo-zh-en"
    const val ZH_LL_ID = "zh-ll"
    const val FANCHEN_WNJ_ID = "fanchen-wnj"

    private const val RELEASE_BASE =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    val all: List<SherpaModelPack> = listOf(
        SherpaModelPack(
            id = MELO_ID,
            displayName = "Melo 中文",
            genderLabel = "女声",
            archiveUrl = "$RELEASE_BASE/vits-melo-tts-zh_en.tar.bz2",
            dirName = "vits-melo-tts-zh_en",
            archiveFileName = "sherpa-melo.tar.bz2",
            ruleFsts = listOf("date.fst", "number.fst"),
            speakerCount = 1,
            defaultSid = 0,
            estimatedSizeMb = 50,
            speakerLabels = listOf("女声"),
        ),
        SherpaModelPack(
            id = ZH_LL_ID,
            displayName = "中文 LL",
            genderLabel = "5 音色",
            archiveUrl = "$RELEASE_BASE/sherpa-onnx-vits-zh-ll.tar.bz2",
            dirName = "sherpa-onnx-vits-zh-ll",
            archiveFileName = "sherpa-zh-ll.tar.bz2",
            ruleFsts = listOf("phone.fst", "number.fst"),
            speakerCount = 5,
            defaultSid = 0,
            estimatedSizeMb = 113,
            speakerLabels = listOf("音色 1", "音色 2", "音色 3", "音色 4", "音色 5"),
        ),
        SherpaModelPack(
            id = FANCHEN_WNJ_ID,
            displayName = "Fanchen 中文",
            genderLabel = "男声",
            archiveUrl = "$RELEASE_BASE/vits-zh-hf-fanchen-wnj.tar.bz2",
            dirName = "vits-zh-hf-fanchen-wnj",
            archiveFileName = "sherpa-fanchen-wnj.tar.bz2",
            modelFileName = "vits-zh-hf-fanchen-wnj.onnx",
            ruleFsts = listOf("number.fst"),
            speakerCount = 1,
            defaultSid = 0,
            estimatedSizeMb = 115,
            speakerLabels = listOf("男声"),
        ),
    )

    fun packById(id: String): SherpaModelPack? = all.find { it.id == id }

    fun defaultPack(): SherpaModelPack = all.first()
}

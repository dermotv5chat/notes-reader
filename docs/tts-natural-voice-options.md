# TTS 自然度方案对比与选型

本文档记录笔记 App 朗读自然度升级的调研结论与已落地实现，供后续 Phase 2 离线 neural 参考。

## 现状（系统 TTS）

App 默认使用 Android `TextToSpeech` API（[`TtsController.kt`](../app/src/main/java/com/andriod/reader/service/TtsController.kt)）。听感偏机械常见原因：

1. 默认厂商离线拼接音，非 neural
2. 未安装 / 未下载 Google 中文 neural 语音包
3. 在线 neural 需联网，弱网下降级

## 已落地（Phase 0 + Phase 1 + 整篇预合成）

| 能力 | 说明 |
|------|------|
| **质量分级提示** | [`TtsVoiceQuality.kt`](../app/src/main/java/com/andriod/reader/service/TtsVoiceQuality.kt) 评估当前音色，设置/阅读页展示改进建议 |
| **AUTO 选音增强** | [`TtsHelper.kt`](../app/src/main/java/com/andriod/reader/service/TtsHelper.kt) 优先 neural / 在线 Voice |
| **在线高质量（Edge）** | 设置/阅读页可选「在线高质量」，经 [`OnlineEdgeSpeechBackend`](../app/src/main/java/com/andriod/reader/service/OnlineEdgeSpeechBackend.kt) 整篇预合成 + ExoPlayer 播放；无网回退系统 TTS |
| **离线高质量（Sherpa）** | 设置中下载 VITS 模型，[`OfflineSherpaSpeechBackend`](../app/src/main/java/com/andriod/reader/service/OfflineSherpaSpeechBackend.kt) 与 Edge 共用 [`TtsPreSynthPipeline`](../app/src/main/java/com/andriod/reader/service/synthesis/TtsPreSynthPipeline.kt) |
| **整篇预合成** | 阅读页「生成语音」按钮；≤4000 字一次合成，超长按段落块降级（不按逗号切）；磁盘缓存于 `cache/tts-presynth/` |
| **共享朗读设置 UI** | [`TtsVoiceSettingsSection.kt`](../app/src/main/java/com/andriod/reader/ui/tts/TtsVoiceSettingsSection.kt) |

### 整篇预合成策略

- **Edge / Sherpa**：用户点「生成语音」或播放时若未就绪则自动生成；就绪后 ExoPlayer 播整文件（超长时为播放列表连播）
- **系统 TTS**：仍逐段 `TextToSpeech.speak`，无预合成 UI
- **缓存键**：`SHA256(正文 + 引擎 + 音色 + 语速 + 音调)`；改文或改设置自然 miss

### 用户使用建议

1. **改动最小**：系统 TTS → 安装 Google TTS → 下载中文 neural → 阅读页偏好「在线」
2. **更好听且可联网**：设置 → 朗读引擎 → **在线高质量** → 阅读页 **生成语音**
3. **无网 / 隧道**：设置 → **离线高质量** → 下载语音包 → 阅读页 **生成语音**
4. **开车 / 弱网回退**：系统 TTS 或接受 Edge 无网时播放失败回退系统分段朗读

## 方案对比摘要

| 方案 | 自然度 | 离线 | 集成状态 |
|------|--------|------|----------|
| 系统 + Google neural | 中～高 | 是 | 已支持 |
| Edge TTS 在线 | 很高 | 否 | **已支持（可选）** |
| Piper / Sherpa-onnx | 中高 | 是 | 见 [离线 neural 评估](./tts-offline-neural-evaluation.md) |
| 云 API 官方 | 很高 | 否 | 未集成（需用户 Key 或后端） |

## 相关文档

- [TTS 引擎初始化失败排查](./tts-engine-init-failure.md)
- [离线 neural 评估（Piper / Sherpa-onnx）](./tts-offline-neural-evaluation.md)

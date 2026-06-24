# TTS 自然度方案对比与选型

本文档记录笔记 App 朗读自然度升级的调研结论与已落地实现，供后续 Phase 2 离线 neural 参考。

## 现状（系统 TTS）

App 默认使用 Android `TextToSpeech` API（[`TtsController.kt`](../app/src/main/java/com/andriod/reader/service/TtsController.kt)）。听感偏机械常见原因：

1. 默认厂商离线拼接音，非 neural
2. 未安装 / 未下载 Google 中文 neural 语音包
3. 在线 neural 需联网，弱网下降级

## 已落地（Phase 0 + Phase 1）

| 能力 | 说明 |
|------|------|
| **质量分级提示** | [`TtsVoiceQuality.kt`](../app/src/main/java/com/andriod/reader/service/TtsVoiceQuality.kt) 评估当前音色，设置/阅读页展示改进建议 |
| **AUTO 选音增强** | [`TtsHelper.kt`](../app/src/main/java/com/andriod/reader/service/TtsHelper.kt) 优先 neural / 在线 Voice |
| **在线高质量（Edge）** | 设置/阅读页可选「在线高质量」，经 [`OnlineEdgeSpeechBackend`](../app/src/main/java/com/andriod/reader/service/OnlineEdgeSpeechBackend.kt) + ExoPlayer 播放；无网回退系统 TTS |
| **共享朗读设置 UI** | [`TtsVoiceSettingsSection.kt`](../app/src/main/java/com/andriod/reader/ui/tts/TtsVoiceSettingsSection.kt) |

### 用户使用建议

1. **改动最小**：系统 TTS → 安装 Google TTS → 下载中文 neural → 阅读页偏好「在线」
2. **更好听且可联网**：设置 → 朗读引擎 → **在线高质量**
3. **开车 / 隧道**：保持「系统 TTS」或接受 Edge 无网时自动回退

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

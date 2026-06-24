# 离线 Neural TTS（Sherpa-onnx 已集成）

Phase 2 结论：**已集成 Sherpa-onnx** 作为「离线高质量」引擎，与 Edge 在线共用整篇预合成管线 [`TtsPreSynthPipeline`](../app/src/main/java/com/andriod/reader/service/synthesis/TtsPreSynthPipeline.kt)。

## 评估维度（Piper vs Sherpa-onnx）

| 维度 | Piper TTS | Sherpa-onnx |
|------|-----------|-------------|
| 许可 | MIT（核心） | Apache-2.0 等 |
| Android 集成 | C++ + JNI，社区 Android 绑定 | 官方 [Android TTS 示例](https://github.com/k2-fsa/sherpa-onnx) |
| 中文模型 | 有限，需实测选模 | 社区 VITS / Melo 等 ONNX 模型较多 |
| 单模型体积 | 约 20～80 MB | 类似，视模型而定 |
| 合成延迟（中端机估） | 短段文本通常 &lt; 1s | 相近；整篇预合成约 2～8s（视字数） |
| 自然度（相对系统拼接） | 明显提升 | 明显提升 |
| 自然度（相对 Edge 在线） | 略逊 | 略逊 |
| 维护成本 | JNI + ABI + 模型分发 | 略高（ONNX 运行时 + 模型） |

## 与本 App 架构的契合

当前朗读管线（2025 整篇预合成）：

- **系统模式**：`TextToSpeech.speak` + 逗号/句号分段 [`TtsSegmentSplitter`](../app/src/main/java/com/andriod/reader/service/TtsSegmentSplitter.kt)
- **Edge / Sherpa 模式**：[`TtsParagraphSplitter`](../app/src/main/java/com/andriod/reader/service/synthesis/TtsParagraphSplitter.kt) 整篇或段落块 → 合成 MP3/WAV → [`ExoPlayerSpeechPlayer`](../app/src/main/java/com/andriod/reader/service/edge/ExoPlayerSpeechPlayer.kt) 单文件或播放列表

实现要点：

- 模型：`vits-melo-tts-zh_en`，应用内下载至 `filesDir/tts-models/`，见 [`SherpaModelManager`](../app/src/main/java/com/andriod/reader/service/synthesis/SherpaModelManager.kt)
- 合成：[`SherpaFullTextSynthesizer`](../app/src/main/java/com/andriod/reader/service/synthesis/SherpaFullTextSynthesizer.kt) / [`OfflineSherpaSpeechBackend`](../app/src/main/java/com/andriod/reader/service/OfflineSherpaSpeechBackend.kt)
- 依赖：`com.github.k2-fsa:sherpa-onnx`（JitPack）

## 推荐结论

1. **默认推荐**：短笔记用户选 Edge 或 Sherpa + **生成语音**，听感优于逐段系统 TTS。
2. **无网场景**：设置 → **离线高质量** → 下载语音包 → 阅读页预合成。
3. **不要** 再并行集成 Piper；Sherpa 已覆盖离线 neural 路径。

## 验证清单

- [x] Sherpa Android 库 + 中文 VITS 模型应用内下载
- [x] 与 Edge 共用预合成缓存与 ExoPlayer 播放
- [ ] 真机对比：厂商 TTS / Google neural / Sherpa / Edge（需人工试听）
- [ ] 长文（5000+ 字）分块生成与连播稳定性

## 参考链接

- Piper: https://github.com/rhasspy/piper
- Sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
- 方案总览: [tts-natural-voice-options.md](./tts-natural-voice-options.md)

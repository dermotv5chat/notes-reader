# 离线 Neural TTS 评估（Piper vs Sherpa-onnx）

Phase 2 调研结论：**暂不集成**，优先使用已落地的「系统 TTS + Google neural」与「Edge 在线高质量」。若未来要做纯离线 neural，建议在二者中 **二选一**。

## 评估维度

| 维度 | Piper TTS | Sherpa-onnx |
|------|-----------|-------------|
| 许可 | MIT（核心） | Apache-2.0 等 |
| Android 集成 | C++ + JNI，社区 Android 绑定 | 官方 [Android TTS 示例](https://github.com/k2-fsa/sherpa-onnx) |
| 中文模型 | 有限，需实测选模 | 社区 VITS / Melo 等 ONNX 模型较多 |
| 单模型体积 | 约 20～80 MB | 类似，视模型而定 |
| 合成延迟（中端机估） | 短段文本通常 &lt; 1s | 相近 |
| 自然度（相对系统拼接） | 明显提升 | 明显提升 |
| 自然度（相对 Edge 在线） | 略逊 | 略逊 |
| 维护成本 | JNI + ABI + 模型分发 | 略高（ONNX 运行时 + 模型） |

## 与本 App 架构的契合

当前朗读管线：

- **系统模式**：`TextToSpeech.speak` + 分段 [`UtteranceProgressListener`](../app/src/main/java/com/andriod/reader/service/TtsController.kt)
- **在线模式**：合成 MP3 → [`ExoPlayerSpeechPlayer`](../app/src/main/java/com/andriod/reader/service/edge/ExoPlayerSpeechPlayer.kt) → 段完成回调

Piper / Sherpa 与 Edge 类似，需 **合成 → 本地播放**，不能复用 `TextToSpeech`。集成工作量预估 **1～3 周**（含中文模型试听、APK 体积与按需下载策略）。

## 推荐结论

1. **短期**：不集成 Piper/Sherpa；用 Phase 0/1 已交付能力即可覆盖多数用户。
2. **若必须离线 neural**：优先 **Sherpa-onnx**（官方 Android Demo、模型生态更活跃）；在独立分支跑通 Demo + 1 个中文 VITS 模型试听后再决定。
3. **不要** 同时维护 Piper 与 Sherpa 两套 native 栈。

## 试听与验证清单（未来若启动集成）

- [ ] 克隆 `k2-fsa/sherpa-onnx` Android TTS 示例，替换为中文 VITS 模型
- [ ] 在目标真机（如小米）测：冷启动合成延迟、长文 20 段连续播放 CPU/内存
- [ ] 对比同一段落：厂商 TTS / Google neural / Sherpa 模型 / Edge 在线
- [ ] 评估 APK 增量与用户「应用内下载模型」流程

## 参考链接

- Piper: https://github.com/rhasspy/piper
- Sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
- 方案总览: [tts-natural-voice-options.md](./tts-natural-voice-options.md)

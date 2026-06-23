# 熄屏继续朗读

## 现象

前台服务与通知栏已就绪，但 **屏幕熄灭后朗读停止**，亮屏后又继续（或表现为暂停后自动恢复）。

## 原因

1. **未持有 WakeLock**：熄屏后 CPU 休眠，TTS 合成/播放在段与段之间或句中被中断。
2. **音频焦点类型不当**：曾使用 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`，长时间朗读易被系统收回焦点。
3. **TTS 绑定 Activity 上下文**：部分 ROM 在 Activity 进入后台时对 TTS 更 aggressive；应使用 `applicationContext`。

Manifest 中已有 `WAKE_LOCK` 权限，但需在播放期间主动 acquire/release。

## 实现要点

| 项 | 做法 |
|----|------|
| WakeLock | [`TtsWakeLock.kt`](../app/src/main/java/com/andriod/reader/service/TtsWakeLock.kt)：`PARTIAL_WAKE_LOCK`，`start`/`resume` 时 acquire，`pause`/`stop` 时 release |
| 音频焦点 | `AUDIOFOCUS_GAIN` + `OnAudioFocusChangeListener`；短暂丢失后自动 `resume` |
| 上下文 | [`TtsController`](../app/src/main/java/com/andriod/reader/service/TtsController.kt) / [`TtsPlaybackManager`](../app/src/main/java/com/andriod/reader/service/TtsPlaybackManager.kt) 统一 `applicationContext` |
| TTS 音频 | `TextToSpeech.setAudioAttributes(USAGE_MEDIA, CONTENT_TYPE_SPEECH)` |

前台服务 [`TtsPlaybackService`](../app/src/main/java/com/andriod/reader/service/TtsPlaybackService.kt)（`foregroundServiceType=mediaPlayback`）与通知栏仍保留，与 WakeLock 互补。

## 系统省电设置（小米 / HyperOS 等建议必做）

代码侧 WakeLock 与前台服务解决「熄屏 CPU 休眠、音频焦点」问题；**部分厂商 ROM 仍会限制后台**，需手动放行：

**设置 → 应用 → 笔记朗读 → 省电 → 无限制**

（或「不受省电策略限制」「允许后台活动」等同类选项。）

真机验证：开启朗读后熄屏，应能持续播放；通知栏 / 锁屏控制仍可用。若仅改代码仍偶发停播，完成上述设置后通常即可稳定。

## 若仍被系统杀进程

除省电策略外，可检查：

- 是否允许 **自启动** / **后台弹出界面**（厂商菜单名称不一）
- 开发阶段 Debug 包与 Release 签名不同，重装后系统可能重置上述权限，需重新设置

## 定时关闭

阅读页 **定时关闭**（[`TtsSleepTimer.kt`](../app/src/main/java/com/andriod/reader/service/TtsSleepTimer.kt)）在后台 / 熄屏仍按系统时钟倒计时（暂停播放时倒计时继续），到点或本篇播完后自动 `stop`。依赖上述 WakeLock 与前台服务；省电「无限制」同样建议开启。

## 相关文档

- [tts-notification-permission.md](tts-notification-permission.md)
- [tts-bluetooth-media-controls.md](tts-bluetooth-media-controls.md)
- [tts-engine-init-failure.md](tts-engine-init-failure.md)

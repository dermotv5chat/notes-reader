# 蓝牙 / 车机播放控制（AVRCP）

## 现象

手机通过 **蓝牙** 连接 **车机** 或 **耳机** 朗读笔记时：

- 其他音乐 App 可在车机/耳机上 **播放 / 暂停**
- 本 App 朗读中，车机方向盘或耳机上的 **播放 / 暂停无效**（或行为错乱）
- 手机通知栏、锁屏上的播放按钮可能正常，但 **外设媒体键** 无响应

## 根因

本 App 已使用 [`TtsPlaybackService`](../app/src/main/java/com/andriod/reader/service/TtsPlaybackService.kt) 前台服务 + `MediaSessionCompat` + 通知栏 `MediaStyle`，与音乐 App 同类。问题出在 **媒体键事件解析与回调**，而非「没做 MediaSession」。

### 1. 错误解析 `KeyEvent`（主要 bug）

车机 / 耳机经 **蓝牙 AVRCP** 发来的按键，Android 会包装为：

- `Intent.ACTION_MEDIA_BUTTON`
- `Intent.EXTRA_KEY_EVENT` → **`KeyEvent` 对象**（Parcelable）

旧实现用 `getIntExtra(Intent.EXTRA_KEY_EVENT, …)` 读取，**永远拿不到有效 keyCode**，按键被丢弃，车机/耳机控不住。

### 2. `onPlay` / `onPause` 误用 `togglePlayPause()`

AVRCP 发送的是 **明确的播放或暂停** 指令。用 toggle 会在「已在播放却收到 PLAY」等情况下 **反向操作**，表现为偶发无效或乱切状态。

### 3. 缺少 MediaButton 转发（部分设备）

部分 ROM / 车机在 App 处于后台时，只把按键发给 `MediaSession.setMediaButtonReceiver` 注册的 **BroadcastReceiver**，若未注册则 Session 回调收不到事件。

## 与 Android Auto 的区别

| 连接方式 | 本修复是否覆盖 | 说明 |
|----------|----------------|------|
| **手机蓝牙 → 车机**（显示手机媒体信息） | ✅ | 走 AVRCP，与耳机相同 |
| **蓝牙耳机 / 真无线** | ✅ | 单键多为 `PLAY_PAUSE`，双键为 `PLAY` / `PAUSE` |
| **有线耳机线控** | 多数 ✅ | 系统汇总为媒体键，通常同样生效 |
| **Android Auto 专用界面 / CarPlay** | ❌ 未单独集成 | 需另做 Android Auto Media 或 CarPlay SDK，不在本次范围 |

## 本 App 的修复

| 项 | 做法 |
|----|------|
| 解析媒体键 | [`TtsMediaButtonHandler`](../app/src/main/java/com/andriod/reader/service/TtsMediaButtonHandler.kt)：从 `EXTRA_KEY_EVENT` 取 `KeyEvent`，仅处理 `ACTION_DOWN` |
| 明确播放/暂停 | [`TtsPlaybackManager`](../app/src/main/java/com/andriod/reader/service/TtsPlaybackManager.kt) 增加 `pausePlayback()` / `resumePlayback()`；Session 回调 `onPlay` / `onPause` 分别调用 |
| 播放/暂停单键 | `KEYCODE_MEDIA_PLAY_PAUSE` 仍用 `togglePlayPause()`（耳机常见） |
| MediaSession 标志 | `FLAG_HANDLES_MEDIA_BUTTONS` + `FLAG_HANDLES_TRANSPORT_CONTROLS` |
| 蓝牙转发 | [`TtsMediaButtonReceiver`](../app/src/main/java/com/andriod/reader/service/TtsMediaButtonReceiver.kt) 接收 `MEDIA_BUTTON`，启动 Service 处理 |
| Session 绑定 | `MediaSessionCompat.setMediaButtonReceiver(PendingIntent → TtsMediaButtonReceiver)` |
| 音频类型 | TTS 已设 `AudioAttributes.USAGE_MEDIA`（与音乐同级，便于系统路由 AVRCP） |

按键到行为的映射：

| 按键 | 行为 |
|------|------|
| `MEDIA_PLAY` | 继续朗读 |
| `MEDIA_PAUSE` | 暂停朗读 |
| `MEDIA_PLAY_PAUSE` | 切换播放/暂停 |
| `MEDIA_STOP` | 停止朗读 |

## 前提条件

外设控制与通知栏控制 **共用同一套 Session**，需满足：

1. **正在朗读**（有活跃 session + 前台服务）
2. **Android 13+** 已允许通知（见 [tts-notification-permission.md](tts-notification-permission.md)）；否则无前台通知，Session 可能未正确激活
3. 蓝牙已连接，系统把音频路由到车机/耳机

## 验证清单

- [ ] 手机连 **蓝牙耳机**，朗读中按耳机 **播放/暂停** 有效
- [ ] 手机连 **车机蓝牙**，方向盘或车机屏幕 **播放/暂停** 有效
- [ ] 车机/锁屏显示 **笔记标题**（AVRCP 元数据正常）
- [ ] 暂停后再按播放 **继续当前段落**，而非从头或反向 toggle
- [ ] 手机通知栏按钮仍正常（与外设一致）

## 若仍无效

1. 确认已安装 **含本修复** 的版本（`TtsMediaButtonHandler` 存在）
2. 确认朗读时通知栏有 **播放卡片**（通知权限、省电无限制见 [tts-screen-off-playback.md](tts-screen-off-playback.md)）
3. 记录：手机型号、Android 版本、车机是 **纯蓝牙媒体** 还是 **Android Auto**，以及车机是否 **根本不显示** 本 App 标题（元数据未通 vs 按键未通）

## 相关文档

- [tts-notification-permission.md](tts-notification-permission.md) — 通知权限与前台服务
- [tts-screen-off-playback.md](tts-screen-off-playback.md) — 熄屏 / 后台播放
- [tts-engine-init-failure.md](tts-engine-init-failure.md) — TTS 引擎初始化

## 测试

逻辑单测：[`TtsMediaButtonHandlerTest`](../app/src/test/java/com/andriod/reader/service/TtsMediaButtonHandlerTest.kt)（`KeyEvent` 解析、忽略 `ACTION_UP`）。

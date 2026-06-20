# TTS 通知权限（HyperOS / Android 13+）

## 现象

在小米 HyperOS 3.0（Android 16）等设备上：

- 系统 **设置 → 应用设置 → 本 App → 通知管理** 显示 **「不允许」**
- 开关 **灰色，无法手动打开**
- 朗读时通知栏 **没有** 播放状态卡片

## 根因

从 **Android 13（API 33）** 起，应用发送通知（含前台服务 `startForeground`）必须：

1. 在 `AndroidManifest.xml` 声明 `POST_NOTIFICATIONS`
2. 在运行时向用户申请该权限

若 Manifest 未声明、也从未弹出系统授权框，HyperOS 会认为该 App **不使用通知**，设置页中通知项会呈灰色不可开启。

这与 [`TtsPlaybackService`](../app/src/main/java/com/andriod/reader/service/TtsPlaybackService.kt) 是否实现无关；缺权限时前台服务通知无法显示。

## 本 App 的修复

- Manifest 已声明 `android.permission.POST_NOTIFICATIONS`
- 阅读页 **首次点播放** 时弹出系统「允许发送通知？」对话框
- 拒绝后：底栏提示 + **「去设置」** 跳转应用通知设置
- 拒绝时仍可在 **阅读页内** 朗读；但离开页面后无通知栏控制（不启动前台服务）
- 通知渠道「语音朗读」重要性为 `IMPORTANCE_DEFAULT`，便于在状态栏显示

相关代码：

- [`NotificationPermission.kt`](../app/src/main/java/com/andriod/reader/util/NotificationPermission.kt)
- [`ReaderViewModel.onPlayPauseClicked()`](../app/src/main/java/com/andriod/reader/ui/reader/ReaderViewModel.kt)
- [`ReaderScreen`](../app/src/main/java/com/andriod/reader/ui/reader/ReaderScreen.kt) 中的 `RequestPermission` launcher

## 用户操作指引

### 首次使用（推荐）

1. 打开笔记，点击 **播放**
2. 在系统弹窗中选择 **允许**
3. 返回列表或切到其他 App，下拉通知栏应能看到朗读通知

### 若曾点「拒绝」或设置里仍为灰色

1. **卸载旧版** 后安装含 `POST_NOTIFICATIONS` 的新版（Manifest 变更后建议重装一次）
2. 再次打开笔记并点播放，应出现授权弹窗
3. 若仍无弹窗：设置 → 应用设置 → 本 App → 通知管理 → **允许通知**
4. 也可在阅读页底栏点 **「去设置」** 直接跳转

### 若授权后仍无通知

再检查 HyperOS 是否限制后台（省电、自启动等）；多数情况下授权后即可正常显示。

## 验证清单

- [ ] 点播放弹出「允许发送通知？」
- [ ] 允许后设置里通知为「允许」，可单独开关「语音朗读」渠道
- [ ] 后台朗读时通知栏有标题、段落进度、播放/暂停/停止
- [ ] 拒绝后阅读页内仍可朗读，但无后台通知

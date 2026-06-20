# TTS 语音引擎初始化失败排查记录

## 现象

在小米真机上，阅读页点击「朗读」无反应，或提示 **「无法启动语音引擎」**。

同时用户确认：系统 **设置 → 更多设置 → 无障碍 → 文字转语音输出** 中，默认引擎试听中文 **正常**。

即：**系统 TTS 可用，但 App 内初始化失败**。

## 环境

- 设备：小米 / MIUI 真机（测试设备 `34f0671b`）
- App：Kotlin + Compose + Hilt，TTS 封装在 [`TtsController.kt`](../app/src/main/java/com/andriod/reader/service/TtsController.kt)
- 初始化入口：阅读页 [`ReaderViewModel.initTts()`](../app/src/main/java/com/andriod/reader/ui/reader/ReaderViewModel.kt)、设置页 [`SettingsViewModel.refreshTtsInfo()`](../app/src/main/java/com/andriod/reader/ui/settings/SettingsViewModel.kt)
- 引擎枚举与中文语音：[`TtsHelper.kt`](../app/src/main/java/com/andriod/reader/service/TtsHelper.kt)

## 排查过程

### 1. 排除「没反应」是路由问题

子目录笔记路径含 `/`，早期用路径参数传 `fileName` 会导致导航失败。已改为 query 参数 + `Uri.encode`，阅读页能正常打开。排除后，问题集中在 TTS 初始化。

### 2. 增加用户可见的错误与状态

在 `ReaderUiState` 增加 `isTtsReady`、`isTtsInitializing`、`ttsError`。初始化失败时展示诊断文字，例如：

- 已尝试的引擎列表
- 系统默认引擎包名（`Settings.Secure.TTS_DEFAULT_SYNTH`）
- 已安装的 TTS 引擎包名

便于在真机上直接反馈，而不依赖 `adb logcat`。

### 3. 多引擎逐个重试 + 超时

`TtsController.awaitReady()` 按顺序尝试多个引擎，每个引擎单独等待 `onInit`，超时后切换下一个：

| 顺序 | 引擎 | 说明 |
|------|------|------|
| 1 | `null`（不显式指定包名） | 由系统选择默认引擎，在 MIUI 上最稳妥 |
| 2 | 系统默认包名 | 从 `TTS_DEFAULT_SYNTH` 读取 |
| 3 | 其他已安装引擎 | 排除 Google |
| 4 | `com.google.android.tts` | 放最后，避免卡住阻塞 |

单引擎超时 8 秒；系统默认（`null`）放宽到 15 秒。

### 4. 对比系统设置与 App 行为

系统试听正常，说明：

- 手机已安装可用 TTS 引擎
- 中文语音资源存在
- 权限与系统服务无硬性阻断

因此重点转向 **App 侧初始化方式**，而非让用户重装 Google TTS。

### 5. Context 类型验证（关键线索）

早期在 `ViewModel` 里用 `@ApplicationContext` 构造 `TextToSpeech`。在部分厂商 ROM（尤其 MIUI）上，`ApplicationContext` 初始化 TTS 可能失败或回调异常，而 **Activity Context** 正常。

修复：阅读页、设置页均通过 `LocalContext.current`（Activity）调用 `initTts` / `refreshTtsInfo`，不再在 ViewModel `init` 里用 Application Context 预初始化。

### 6. 定位竞态：迟到的 `onInit`（根因）

多引擎重试时，若引擎 A 初始化较慢：

1. App 等待 8s 超时，`shutdown` 引擎 A
2. 开始尝试引擎 B
3. 引擎 A 的 `onInit` **迟到到达**，误完成引擎 B 的 `CompletableDeferred`
4. 状态错乱，最终所有引擎均判失败

小米自带 TTS 冷启动偏慢，极易触发此竞态。用户侧表现为：系统设置里引擎早已可用，App 却始终「无法启动」。

**验证思路**：失败提示中「已尝试」列表完整，但无一成功；且问题在慢引擎 + 短超时设备上稳定复现。

### 7. 次要因素：语音配置不应阻塞就绪

原逻辑在 `onInit(SUCCESS)` 后同步调用 `setupChineseVoice()`（访问 `engine.voices`、设置中文 Voice）。部分引擎在 `onInit` 瞬间访问 voices 可能抛异常，导致 **引擎其实已就绪却被判失败**。

修复：先 `completeInit(true)` 标记引擎就绪，中文语音配置放到 `runCatching` 中异步完成，失败也不影响朗读。

### 8. Android 11+ 包可见性

`PackageManager.queryIntentServices(TTS_SERVICE)` 在 targetSdk ≥ 30 时，若 Manifest 未声明 `<queries>`，可能枚举不到已安装引擎，影响诊断信息与回退顺序。

已在 [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) 增加：

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
```

## 根因总结

| 优先级 | 原因 | 后果 |
|--------|------|------|
| **主因** | 多引擎重试时，旧引擎迟到的 `onInit` 污染新引擎的初始化状态（竞态） | 全部引擎尝试均失败 |
| **次因** | 使用 `ApplicationContext` 初始化 TTS（MIUI 不兼容） | `onInit` 不回调或失败 |
| **次因** | `setupChineseVoice` 异常导致整次初始化失败 | 引擎可用但被误判 |
| **辅助** | 缺少 TTS `<queries>` | 引擎列表不完整，影响诊断与顺序 |
| **加重** | 小米 TTS 冷启动慢 + 8s 超时偏短 | 更容易触发竞态与误判 |

## 最终方案

### 1. 使用 Activity Context 初始化

阅读页 [`ReaderScreen.kt`](../app/src/main/java/com/andriod/reader/ui/reader/ReaderScreen.kt)：

```kotlin
LaunchedEffect(context) {
    viewModel.initTts(context)
}
```

设置页 [`SettingsScreen.kt`](../app/src/main/java/com/andriod/reader/ui/settings/SettingsScreen.kt) 同样在界面显示后传入 `LocalContext.current`，不在 ViewModel `init` 里预初始化。

### 2. 为每次引擎尝试分配 attemptId，忽略过期回调

```kotlin
awaitingInitAttemptId++
val attemptId = awaitingInitAttemptId

tts = TtsHelper.createTextToSpeech(hostContext, { status ->
    if (attemptId != awaitingInitAttemptId) return@createTextToSpeech
    onInit(status)
}, enginePackage)
```

超时切换下一引擎时递增 `awaitingInitAttemptId`，迟到的旧回调被丢弃。

### 3. 引擎就绪与语音配置解耦

`onInit(SUCCESS)` 后先注册 `UtteranceProgressListener` 并 `completeInit(true)`；`setupChineseVoice` 放入 `runCatching`，不阻塞就绪。

### 4. 引擎尝试顺序

[`TtsHelper.engineTryOrder()`](../app/src/main/java/com/andriod/reader/service/TtsHelper.kt)：`null`（系统默认）优先，其次系统默认包名，再次其他厂商引擎，最后 Google TTS。

### 5. 超时策略

- 系统默认（`enginePackage == null`）：15 秒
- 显式指定包名：8 秒

## 失败时的诊断信息

初始化失败时，阅读页 `ttsError` 会拼接：

1. `无法启动语音引擎。`
2. `已尝试：系统默认, com.xiaomi...., ...`（如有）
3. `系统默认：com.xxx`（如能读取）
4. `已安装：com.a, com.b, ...`（枚举结果）
5. 引导用户检查系统「文字转语音输出」

收集用户反馈时，请让对方 **原文复制整段错误提示**，便于判断是超时、竞态还是 Context 问题。

## 相关文件

| 文件 | 职责 |
|------|------|
| [`TtsController.kt`](../app/src/main/java/com/andriod/reader/service/TtsController.kt) | 引擎重试、`awaitReady`、朗读控制、attemptId 防竞态 |
| [`TtsHelper.kt`](../app/src/main/java/com/andriod/reader/service/TtsHelper.kt) | 引擎顺序、中文 Voice、诊断信息 |
| [`ReaderViewModel.kt`](../app/src/main/java/com/andriod/reader/ui/reader/ReaderViewModel.kt) | 阅读页 TTS 状态与 `initTts(hostContext)` |
| [`ReaderScreen.kt`](../app/src/main/java/com/andriod/reader/ui/reader/ReaderScreen.kt) | `LaunchedEffect` 触发初始化 |
| [`SettingsViewModel.kt`](../app/src/main/java/com/andriod/reader/ui/settings/SettingsViewModel.kt) | 设置页试听 / 刷新 TTS |
| [`SettingsScreen.kt`](../app/src/main/java/com/andriod/reader/ui/settings/SettingsScreen.kt) | 传入 Activity Context |
| [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | TTS `<queries>` 声明 |
| [`TtsHelperEngineOrderTest.kt`](../app/src/test/java/com/andriod/reader/service/TtsHelperEngineOrderTest.kt) | 引擎顺序单元测试 |

## 实践记录（2026-06）

- **小米真机**：系统 TTS 试听中文正常，App 报「无法启动语音引擎」。
- **修复前**：`ApplicationContext` 初始化 + 多引擎重试无 attemptId 防竞态 + 语音配置阻塞就绪。
- **修复后**：Activity Context + attemptId 忽略迟到 `onInit` + 就绪与语音配置解耦 + 系统默认 15s 超时。
- **结果**：用户确认朗读功能恢复正常（「可以了」）。

## 后续若再出现类似问题

1. 先让用户复制 App 内完整 `ttsError` 文案。
2. 确认系统「文字转语音输出」默认引擎试听是否正常。
3. 看「已尝试」是否包含 `系统默认` 且是否排在第一位。
4. 换机时重点怀疑：**厂商 ROM + Context 类型 + 慢引擎竞态**，而非笔记内容或 Markdown 解析。
5. 开发调试可用 `adb logcat | findstr -i tts` 观察 `onInit` 回调时序（可选）。

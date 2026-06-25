# 笔记朗读 (Andriod Reader)

本地 Markdown 笔记 + GitHub 手动同步 + TTS 语音朗读的 Android App。

**Agent 上手指南：** [AGENTS.md](AGENTS.md)（架构、模块地图、近期变更、必守规则与文档索引）

## 功能

- 离线新建、编辑、删除笔记（本地 `notes/*.md`）
- iOS 风格编辑：自动保存、格式工具栏、撤销/重做
- 阅读页 Markdown 渲染；朗读前自动剥离格式符号
- 手动上传/下载到 GitHub 仓库 [dermotv5chat/notes](https://github.com/dermotv5chat/notes)；上传冲突（409/404）排查见 [docs/github-sync-upload-conflict.md](docs/github-sync-upload-conflict.md)
- TTS 朗读笔记：语音选择、语速调节、分段播放；**播放列表** Tab 可查看队列并一键播放（多篇连播、单曲/列表循环）；自然度方案见 [docs/tts-natural-voice-options.md](docs/tts-natural-voice-options.md)
- **行为准则（P1）**：Callout 块可点击，记录今日遵守或违背；普通待办仅展示；详见 [docs/principles-guide.md](docs/principles-guide.md)

详见 [CHANGELOG.md](CHANGELOG.md)。安装到手机见 [docs/install-to-device.md](docs/install-to-device.md)。

## 开发环境

- Android Studio（推荐）或命令行 Gradle
- JDK 17（Android Studio 自带 JBR 即可）
- Android SDK API 35

## 构建

### `assembleDebug` 与 `install2device.ps1` 的区别

| | `.\gradlew assembleDebug` | `.\install2device.ps1` |
|---|---|---|
| **作用** | 只编译、打包 | 编译 + 安装到已连接手机 |
| **产出** | `app/build/outputs/apk/debug/app-debug.apk` | 手机上已安装的新版 App |
| **需要 USB 连接设备** | 否 | 是（`adb devices` 显示 `device`） |
| **自动配置 Java / SDK** | 否 | 是 |
| **自动配置 Gradle 缓存** | 否 | 是（见下方说明） |

关系：`install2device.ps1` **内部会调用** `assembleDebug`，再执行 `adb install -r`。  
只想确认能否编译、或只要 APK 文件 → 用 `gradlew assembleDebug`；改完要在真机上看效果 → 用 `install2device.ps1`。

**Gradle 缓存：** Wrapper 会把 Gradle 下载到 `%USERPROFILE%\.gradle\wrapper\dists\`，正常情况下**只下载一次**。若在 Cursor 终端等沙箱环境里直接跑 `gradlew` 可能走临时目录并重复下载；`install2device.ps1` 已默认设置 `GRADLE_USER_HOME` 指向本机缓存。直接跑 `gradlew` 时可手动指定：

```powershell
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
```

### 仅构建（不安装）

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
cd E:\workspace\andriod-reader
.\gradlew.bat assembleDebug
```

若 Gradle 下载卡住，检查 `%USERPROFILE%\.gradle\wrapper\dists\gradle-8.9-bin\` 下是否有未下完的 `.part` / `.lck`；完整包应位于对应 hash 目录的 `gradle-8.9-bin.zip`（约 136 MB）。

## 测试

### `testDebugUnitTest` 与 `runAndroidTest.ps1` 的区别

| | `.\gradlew testDebugUnitTest` | `.\runAndroidTest.ps1` |
|---|---|---|
| **作用** | 跑 JVM 单元测试 | 同上（默认），并自动配置 Java / Gradle 缓存 |
| **Compose UI 测试** | 是（Robolectric，无需连手机） | 是 |
| **需要 USB 连接设备** | 否 | 否（默认）；加 `-Instrumented` 时才需要 |
| **典型耗时** | 数十秒 | 数十秒 |

关系：`runAndroidTest.ps1` **默认调用** `testDebugUnitTest`，包含定时关闭等 Compose UI 测试（`ReaderSleepTimerSheetTest`），在电脑上即可跑完，不必装测试 APK 到手机。

**若直接跑 `gradlew` 报 `JAVA_HOME is not set`**，请先设置 Java 或使用脚本（脚本会自动指向 Android Studio JBR）：

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
```

### 方式一：一键脚本（推荐）

**跑全部单元测试：**

```powershell
cd E:\workspace\andriod-reader
.\runAndroidTest.ps1
```

**只跑指定测试类：**

```powershell
.\runAndroidTest.ps1 -TestClass com.andriod.reader.ui.reader.ReaderSleepTimerSheetTest
```

脚本会自动配置 `JAVA_HOME`、`GRADLE_USER_HOME`；默认走 Robolectric，无需连接手机。

### 方式二：命令行 gradle

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
cd E:\workspace\andriod-reader
.\gradlew.bat testDebugUnitTest
```

### 真机仪器测试（可选，较慢）

仅在需要验证真机行为时使用；小米/HyperOS 可能需开启 **USB 调试（通过 USB 安装）**，且 `connectedDebugAndroidTest` 可能长时间停在 `0/N completed`：

```powershell
.\runAndroidTest.ps1 -Instrumented -TestClass com.andriod.reader.ui.reader.ReaderSleepTimerSheetTest
```

## Git 提交

### Cursor 命令（推荐）

在 Agent 输入框输入：

```text
/commit
/commit -p
/commit fix: 新建笔记放入当前文件夹
/commit -p fix: 新建笔记放入当前文件夹
```

会加载 `.cursor/commands/commit.md`：Agent 会查看 diff、撰写 commit message，并执行 `commit.ps1`；带 `-p` 时还会 push 到当前分支对应远端。

### 脚本（手动或 Agent 调用）

**SSH 在 WSL**：`commit.ps1` 在 Windows 上完成 `git add` + `commit`，`-p` 时仅 push 走 WSL（`commit.sh --push-only`）。也可直接在 WSL 运行 `./commit.sh` 做全流程。

```powershell
cd E:\workspace\andriod-reader
.\commit.ps1 "fix: 新建笔记放入当前文件夹"
.\commit.ps1 -p "fix: 新建笔记放入当前文件夹"
# 可选：全流程在 WSL（含 commit 作者信息）
.\commit.ps1 -Wsl -p "fix: 新建笔记放入当前文件夹"
```

```bash
# WSL 内
cd /mnt/e/workspace/andriod-reader
./commit.sh -p "fix: 新建笔记放入当前文件夹"
```

脚本会自动 `git add`（排除 `app/build/`、`.gradle/`、`local.properties` 等），然后提交；`-p` 时 push 到当前分支对应远端。

构建产物路径：

| 版本 | 命令 | APK 路径 |
|------|------|----------|
| Debug | `assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `assembleRelease` | `app/build/outputs/apk/release/app-release.apk` |

## 安装到手机

### 前提

- 手机 Android 8.0+（项目 `minSdk = 26`）
- 开启 **开发者选项 → USB 调试**（设置里连点「版本号」7 次可打开开发者选项）
- 电脑已安装 [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)（Android Studio 自带）

### 方式一：Android Studio（推荐）

1. USB 连接手机，手机上允许调试授权
2. Android Studio 打开本项目，顶部设备列表选中你的手机
3. 点击 **Run ▶**（或 `Shift+F10`）
4. Studio 会自动编译 Debug 版并安装

### 方式二：一键脚本（推荐）

**PowerShell（推荐，最快）：**

```powershell
cd E:\workspace\andriod-reader
.\install2device.ps1
```

脚本会自动配置 `JAVA_HOME`、`ANDROID_HOME`、`GRADLE_USER_HOME`，执行 `assembleDebug`，并对所有已连接设备 `adb install -r`。等价于「构建」+「方式三」里的 adb 安装步骤。

Git Bash / WSL 可用 `./install2device.sh`，但首次往往要下载 Gradle，明显更慢。详见 [docs/install-to-device.md](docs/install-to-device.md)。

### 方式三：命令行 adb

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd E:\workspace\andriod-reader

# 1. 构建
.\gradlew.bat assembleDebug

# 2. 确认手机已连接
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices

# 3. 安装（-r 表示覆盖安装，保留数据）
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

`adb devices` 应显示 `device`（不是 `unauthorized` 或 `offline`）。

### 方式四：拷贝 APK 到手机

1. 将 `app-debug.apk` 传到手机（微信、网盘、数据线均可）
2. 在文件管理器中点击安装
3. 若系统提示「未知来源」，在设置中允许该来源安装应用

## 在 Windows 上查看 Android 日志

常用以下几种方式（排查 TTS、同步等问题时配合 **设置 → 存储与诊断** 里导出的诊断日志一起看）。

### 1. Android Studio（最省事）

1. 手机打开 **USB 调试**，用数据线连电脑
2. 打开 Android Studio → 打开 `andriod-reader` 工程
3. 底部点 **Logcat**（或 `View` → `Tool Windows` → `Logcat`）
4. 设备选你的手机，进程选 `com.andriod.reader`
5. 搜索框可过滤，例如：
   - `ReaderDiag`（App 诊断日志 tag）
   - `package:com.andriod.reader`

### 2. 命令行 adb（不开 Studio 也行）

先确认 adb 可用（Android SDK 的 `platform-tools` 在 PATH 里）：

```powershell
adb devices
```

有设备后：

```powershell
# 只看本 App
adb logcat --pid=$(adb shell pidof -s com.andriod.reader)

# 或按 tag 过滤
adb logcat -s ReaderDiag

# 清空旧日志再复现问题
adb logcat -c
adb logcat -s ReaderDiag
```

若 `pidof` 不支持，可先启动 App，再：

```powershell
adb logcat | Select-String "andriod.reader"
```

### 3. 无线调试（Android 11+，可选）

```powershell
adb pair <IP:配对端口>
adb connect <IP:连接端口>
adb devices
```

之后同样用 Studio Logcat 或 `adb logcat`。

**小提示**

- 第一次连接手机会弹出「允许 USB 调试」，要点允许
- 没设备时：换线、换 USB 口、装/更新 [Google USB Driver](https://developer.android.com/studio/run/win-usb)
- 复现 TTS 等问题：先 `adb logcat -c` 清空，再操作 App，日志更干净
- App 内可在 **设置 → 存储与诊断** 导出诊断 txt；开发调试仍建议配合 **Logcat + `ReaderDiag` tag**

## Debug 版 vs Release 版

| | Debug | Release |
|--|-------|---------|
| **用途** | 日常开发、自己试用 | 正式分发、长期使用 |
| **构建命令** | `assembleDebug` | `assembleRelease` |
| **可调试** | 是（可连 Android Studio 断点） | 否 |
| **体积** | 略大 | 略小（当前未开启混淆，差距不大） |
| **签名** | 自动用 Android 调试证书 | 需正式签名证书 |
| **能否覆盖安装** | 与 Release 签名不同，**不能**互相覆盖 | 同左 |

**当前项目状态：**

- 开发阶段一直用的是 **Debug 版**（`app-debug.apk`），功能够用，适合自己装手机日常用
- 项目已配置 `release` 构建类型，但**尚未配置正式签名密钥**；本地可执行 `assembleRelease` 打出包，若要公开发布或上架，还需在 `app/build.gradle.kts` 中配置 `signingConfigs`（keystore）

对自己用：继续 `assembleDebug` + `adb install -r` 即可。

Debug / Release 差异、性能说明，以及 **为何不要在 Debug 上开 minify**，见 [docs/debug-vs-release-builds.md](docs/debug-vs-release-builds.md)。

## 首次使用

1. 用 Android Studio 打开本项目
2. 安装到手机或模拟器
3. 进入 **设置** 页，填入 GitHub PAT（`repo` 权限）
4. 仓库默认：`dermotv5chat` / `notes`，笔记目录 `notes`
5. 在列表页点 **下载** 拉取远程笔记，或 **新建** 后点 **上传**

## 安全说明

- `pat.txt` 仅用于本地开发，已加入 `.gitignore`，**切勿提交**
- PAT 保存在手机加密存储中，不会写入代码

## 电脑端协作

电脑上继续用 SSH 管理同一仓库：

```bash
git clone git@github.com:dermotv5chat/notes.git
```

手机与电脑通过 GitHub 同步；同时修改同一文件时，App 会提示冲突处理。上传 409/404 等问题见 [docs/github-sync-upload-conflict.md](docs/github-sync-upload-conflict.md)。

## 语音包建议

安装 **Google 文字转语音**，在系统设置中下载中文「神经网络/增强」语音包，朗读更自然。

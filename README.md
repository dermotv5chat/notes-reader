# 笔记朗读 (Andriod Reader)

本地 Markdown 笔记 + GitHub 手动同步 + TTS 语音朗读的 Android App。

## 功能

- 离线新建、编辑、删除笔记（本地 `notes/*.md`）
- iOS 风格编辑：自动保存、格式工具栏、撤销/重做
- 阅读页 Markdown 渲染；朗读前自动剥离格式符号
- 手动上传/下载到 GitHub 仓库 [dermotv5chat/notes](https://github.com/dermotv5chat/notes)
- TTS 朗读笔记：语音选择、语速调节、分段播放

详见 [CHANGELOG.md](CHANGELOG.md)。安装到手机见 [docs/install-to-device.md](docs/install-to-device.md)。

## 开发环境

- Android Studio（推荐）或命令行 Gradle
- JDK 17（Android Studio 自带 JBR 即可）
- Android SDK API 35

## 构建

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd E:\workspace\andriod-reader
.\gradlew.bat assembleDebug
```

若 Gradle 下载慢，可先用腾讯云镜像手动下载 `gradle-8.9-bin.zip`，或使用本地 Gradle：

```powershell
& "$env:TEMP\gradle-8.9\bin\gradle.bat" assembleDebug
```

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

手机与电脑通过 GitHub 同步；同时修改同一文件时，App 会提示冲突处理。

## 语音包建议

安装 **Google 文字转语音**，在系统设置中下载中文「神经网络/增强」语音包，朗读更自然。

# 安装 App 到手机

本文记录将「笔记朗读」Debug 包装到真机的实践方式。**日常推荐 PowerShell**，比 WSL / Git Bash 脚本更快、更稳。

## 推荐方式：PowerShell 一键脚本

```powershell
cd E:\workspace\andriod-reader
.\install2device.ps1
```

脚本 [`install2device.ps1`](../install2device.ps1) 会依次：

1. 自动设置 `JAVA_HOME`（Android Studio JBR）
2. 从 `local.properties` 或默认路径读取 `ANDROID_HOME`
3. 构建 Debug APK（优先用 `%TEMP%\gradle-8.9`，避免重复下载 Gradle）
4. `adb install -r` 安装到所有已连接设备

### 为什么 PowerShell 更快

| 对比项 | PowerShell `install2device.ps1` | WSL `./install2device.sh` |
|--------|--------------------------------|---------------------------|
| 运行环境 | Windows 原生 | WSL → 再调 `cmd.exe` |
| Gradle | 常用 `%TEMP%\gradle-8.9` 缓存 | 易走 `gradlew.bat` 首次下载（约 130MB，无进度条） |
| 路径 / JDK | 直接 Windows 路径 | 需 `/mnt/c/...` 转换 |
| 典型耗时 | 已缓存时构建数秒～1 分钟 + 安装十几秒 | 首次可能 10～15 分钟以上 |

**结论：在 Windows 上开发装机，用 PowerShell，不要用 WSL 跑安装脚本。**

## 前提条件

- 手机 Android 8.0+（`minSdk = 26`）
- USB 数据线连接电脑
- 手机开启 **开发者选项 → USB 调试**
- 电脑已装 Android Studio（含 SDK Platform-Tools）

首次连接时在手机上点 **允许 USB 调试**。

## 如何判断当前终端

| 提示符 | 环境 | 用什么 |
|--------|------|--------|
| `PS E:\workspace\...>` | **PowerShell**（推荐） | `.\install2device.ps1` |
| `/mnt/e/workspace/...$` | WSL | 建议换 PowerShell |
| `MINGW64 ... $` | Git Bash | `./install2device.sh` |

PowerShell 里可确认：

```powershell
$PSVersionTable.PSVersion   # 有版本号即为 PowerShell
```

## 手动安装（分步）

与一键脚本等价，便于排查问题。

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd E:\workspace\andriod-reader

# 构建（有缓存时很快）
& "$env:TEMP\gradle-8.9\bin\gradle.bat" assembleDebug
# 若无缓存：.\gradlew.bat assembleDebug

# 确认设备
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices

# 安装（-r 覆盖安装，保留应用数据）
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

### 仅安装、不重新编译

APK 已存在时最快：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r E:\workspace\andriod-reader\app\build\outputs\apk\debug\app-debug.apk
```

## `adb` 找不到

PowerShell 里直接输入 `adb` 若报错「无法识别」，说明 **未加入 PATH**，不是没安装。

用完整路径即可（Android Studio 默认位置）：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

可选：将 `%LOCALAPPDATA%\Android\Sdk\platform-tools` 加入用户 PATH，重启终端后可直接用 `adb`。

## 机型差异（Pixel / 小米等）

- **编译速度**：与手机型号无关，只取决于电脑环境与是否已有 Gradle 缓存。
- **安装阶段**：不同厂商可能有额外限制。

### 小米 / 红米

若安装失败：

```
INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
```

请在手机打开：

**设置 → 开发者选项 → USB 安装**（或「通过 USB 安装应用」）

### 多台设备

`install2device.ps1` 会对 `adb devices` 中所有 `device` 状态设备逐个安装。

## Debug 与 Release

| 类型 | 构建命令 | 输出路径 |
|------|----------|----------|
| Debug（日常） | `assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `assembleRelease` | `app/build/outputs/apk/release/app-release.apk` |

当前一键脚本只构建并安装 **Debug**。Release 需配置签名后再用。

## 其他安装方式

- **Android Studio**：选中设备 → Run ▶，适合调试断点。
- **拷贝 APK**：将 `app-debug.apk` 传到手机，文件管理器点击安装（需允许未知来源）。

## 相关文件

- [`install2device.ps1`](../install2device.ps1) — Windows 推荐
- [`install2device.sh`](../install2device.sh) — Git Bash / WSL（次选）
- [`local.properties.example`](../local.properties.example) — SDK 路径模板（`local.properties` 由 Android Studio 生成，已 gitignore）

## 实践记录（2026-06）

- **Pixel**：在 PowerShell 下用缓存 Gradle + `adb install -r`，构建与安装均很快。
- **小米**：编译流程相同；若安装被拒，开启「USB 安装」即可。
- **WSL 脚本**：首次易卡在 `Downloading gradle-8.9-bin.zip`（无进度条），体验明显慢于 PowerShell；不推荐日常装机构建。

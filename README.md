# 笔记朗读 (Andriod Reader)

本地 Markdown 笔记 + GitHub 手动同步 + TTS 语音朗读的 Android App。

## 功能

- 离线新建、编辑、删除笔记（本地 `notes/*.md`）
- 手动上传/下载到 GitHub 仓库 [dermotv5chat/notes](https://github.com/dermotv5chat/notes)
- TTS 朗读笔记，支持语速调节、分段播放

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

若 Gradle 下载慢，可先用腾讯云镜像手动下载 `gradle-8.9-bin.zip` 再运行 wrapper。

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

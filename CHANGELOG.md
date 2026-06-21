# Changelog

本项目的所有重要变更记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-06-20

一期（MVP）功能完成。

### Added

#### 笔记与存储
- 本地 Markdown 笔记 CRUD，文件存储于 `notes/*.md`
- YAML front matter 元数据（`id`、`title`、`updatedAt`）
- 笔记列表：搜索、新建、编辑、删除
- 同步状态记录（`.meta/sync-state.json`）

#### 编辑体验（iOS 风格）
- 无边框全屏编辑：大标题 + 正文、占位符、浅暖色背景
- 600ms 防抖自动保存；返回时立即保存
- 空笔记直接返回不创建文件
- 键盘上方格式工具栏：加粗、斜体、删除线、无序/有序列表、待办、小标题
- 撤销 / 重做（最多 50 步，工具栏末尾）
- 格式操作插入 Markdown 符号，存储格式不变

#### 阅读与朗读
- 阅读页 Markdown View 渲染（加粗、斜体、删除线、标题、列表、待办）
- TTS 语音朗读：播放 / 暂停 / 停止、下一段、语速调节
- 朗读前 Markdown 净化：不读格式符号；`~~删除线~~` 内容整段跳过
- 强制优先 Google TTS 引擎，自动匹配中文语音
- 朗读页语音选择：自动 / 离线 / 在线 + 下拉选具体音色
- 朗读时可选保持屏幕常亮

#### GitHub 同步
- PAT 保存在加密存储（`EncryptedSharedPreferences`）
- 手动上传 / 下载（GitHub Contents API）
- 支持配置仓库 owner、repo、笔记目录
- 连接测试与更明确的 404 / 权限错误提示
- 下载路径兼容仓库根目录与 `notes/` 子目录
- 远程冲突检测与用户选择（保留本地 / 保留远程 / 另存副本）

#### 设置
- GitHub 仓库与 PAT 配置
- TTS 引擎诊断（是否安装 Google TTS、可用中文语音数量）
- 默认语速、音调、屏幕常亮

#### 工程与文档
- Kotlin + Jetpack Compose + Material 3 + Hilt 项目骨架
- 单元测试：`MarkdownParser`、`MarkdownEditorActions`、`MarkdownPlainText`、`MarkdownDisplay`、`EditorUndoHistory`
- 技术文档：[`docs/editor-keyboard-toolbar-layout.md`](docs/editor-keyboard-toolbar-layout.md)、[`docs/install-to-device.md`](docs/install-to-device.md)

### Changed
- 编辑完成返回列表，不再自动跳转朗读页
- 阅读页由纯文本显示改为 Markdown 渲染视图
- TTS 朗读文本由原始 Markdown 改为净化后的纯文本

### Fixed
- Gradle 下载超时：支持本地 Gradle 8.9 与镜像构建
- GitHub 同步 404：改进 PAT 权限与仓库路径错误提示
- 下载显示 0 条笔记：修正 404 吞掉与路径回退逻辑
- TTS 中文语音检测：修复异步初始化时序与 `cmn-cn` 区域匹配
- 机械感朗读：默认倾向离线语音，朗读页可选具体音色
- 编辑页键盘弹起文本被顶没：修正 `imePadding` 重复应用
- 格式工具栏与键盘之间缝隙：改用 `adjustNothing`、精确 IME 偏移、去除导航栏 bottom inset 叠加

### Security
- `pat.txt` 加入 `.gitignore`，PAT 不写入代码仓库

---

## 二期候选（未实现）

详见 [docs/二期功能讨论.md](docs/二期功能讨论.md)（Notion 式内容拖拽成本、列表拖文件夹对比、排期建议）。

- 编辑页 Markdown 预览切换
- 富文本所见即所得编辑
- 图片 / 附件插入
- 自动同步或后台同步
- Release 签名与 Play 发布

[1.0.0]: https://github.com/dermotv5chat/andriod-reader/releases/tag/v1.0.0

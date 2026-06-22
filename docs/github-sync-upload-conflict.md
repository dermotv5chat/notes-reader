# GitHub 上传冲突（409 / 404）排查与修复记录

## 现象

在笔记列表页点击 **上传** 时，可能出现以下错误：

### 1. 原始 JSON（409）

Snackbar 显示 GitHub 返回的 JSON，例如：

```json
{
  "message": "content/rules.md does not match 4d0304449efb763a5c8b55260563aa9c6e0b2774",
  "status": "409"
}
```

列表中对应笔记带有 **待上传** 云图标（`PENDING` 状态）。

### 2. 误导性的「无法访问仓库」（404）

Snackbar 显示：

> 无法访问 dermotv5chat/notes。  
> 请确认：1. 仓库名和 Owner 正确 …

用户侧容易误以为 **PAT 或仓库配置错了**，但设置页「测试连接」可能仍然正常，且同一仓库此前已成功同步过。

## 背景：同步状态如何工作

App 使用 GitHub **Contents API** 手动上传/下载，本地每篇笔记在 `.meta/sync-state.json` 中记录：

| 字段 | 含义 |
|------|------|
| `githubSha` | 上次同步时远程文件的 blob SHA |
| `remotePath` | 远程路径（如 `content/rules.md`） |
| `syncStatus` | `LOCAL_ONLY` / `PENDING` / `SYNCED` |

编辑已同步笔记后，`saveNote()` 会把状态设为 `PENDING`，但 **保留旧的 `githubSha`**，等待上传时用该 SHA 做乐观锁更新。

上传流程见 [`SyncRepository.uploadPending()`](../app/src/main/java/com/andriod/reader/data/repository/SyncRepository.kt)。

## 根因分析

### 409：SHA 与远程不一致

GitHub 更新文件时要求提供当前 blob 的 `sha`。若本地 `sync-state.json` 中的 SHA **落后于** GitHub 上的实际版本（例如在电脑上 `git push` 过、或在 GitHub 网页编辑过），PUT 会返回 **409 Conflict**。

修复前：409 直接失败，并把 `errorBody` 原样显示给用户；整次上传在第一个失败文件处中断，已成功上传的文件也不会写入 sync state。

### 404：文件级错误被当成仓库级错误

404 在 GitHub API 中有多种含义：

| 场景 | 实际含义 |
|------|----------|
| 私有仓库 + Token 无权限 | 仓库不可访问（GitHub 故意返回 404 而非 403） |
| `getContents(path)` 路径不存在 | **单个文件** 在远程不存在 |
| 本地 `remotePath` 过期 | sync-state 里仍是旧路径（如 `notes/x.md`），文件已移到 `content/x.md` |
| 远程文件已删、本地仍有旧 SHA | PUT / GET 均可能 404 |

修复前：上传冲突处理在 409 后会 `GET` 远程文件取新 SHA。若 GET 因路径错误或文件已删返回 404，异常冒泡到顶层，`parseHttpError()` 统一显示「无法访问 owner/repo」，**掩盖真实原因**。

### 422：未带 SHA 更新已存在文件

本地状态为 `LOCAL_ONLY` 或 `githubSha` 为空，但远程同路径已有文件时，PUT 不带 `sha` 会返回 422。修复前未单独处理。

## 修复方案（2026-06）

### 1. 409 冲突自动恢复

[`uploadNoteFile()`](../app/src/main/java/com/andriod/reader/data/repository/SyncRepository.kt) 捕获 409 后：

1. 拉取远程最新内容与 SHA（见下方路径回退）
2. 比较本地与远程 front matter 中的 `updatedAt`
3. **本地不比远程旧** → 用新 SHA 自动重试 PUT（用户点上传即表示要以本地为准）
4. **远程更新** → 弹出与下载相同的「同步冲突」对话框（保留本地 / 保留远程 / 另存副本）

策略见 [`SyncUploadPolicy`](../app/src/main/java/com/andriod/reader/data/repository/SyncUploadPolicy.kt)。

### 2. 远程路径回退

[`SyncUploadPaths.candidateRemotePaths()`](../app/src/main/java/com/andriod/reader/data/repository/SyncUploadPaths.kt) 依次尝试：

1. `sync-state.json` 中的 `remotePath`
2. 当前本地相对路径（如 `content/rules.md`）

避免 rename / 移动文件夹后仍向旧路径 GET 导致 404。

### 3. 404 / 422：远程文件不存在时重建

若所有候选路径 GET 均为 404，视为远程文件已不存在，**不带 SHA 重新 PUT 创建**。

若 422 且远程文件存在，则进入与 409 相同的冲突比较流程。

### 4. 逐文件保存进度

每成功上传或删除一篇，立即 `writeAll()` sync state。一篇失败不会丢失同批次中已成功的结果。

### 5. 删除时的 404 / 409

远程文件已被删除时，DELETE 返回 404 → 视为成功。409 → 拉取新 SHA 后重试删除。

### 6. 错误提示

- 409 / 422：用户可读中文，不再显示原始 JSON
- 单文件失败：`「content/rules.md」上传失败：…` 带文件名前缀
- 404：补充说明「私有仓库无 Token 权限时 GitHub 也会返回 404」

上传冲突 UI 与下载共用 [`NoteListViewModel.awaitConflictResolution()`](../app/src/main/java/com/andriod/reader/ui/list/NoteListViewModel.kt) 与列表页 [`AlertDialog`](../app/src/main/java/com/andriod/reader/ui/list/NoteListScreen.kt)。

## 用户侧处理建议

| 情况 | 建议操作 |
|------|----------|
| 刚在电脑上 push 过，手机要覆盖远程 | 直接点 **上传**；本地较新时会自动用新 SHA 成功 |
| 远程也有新修改 | 上传时出现冲突对话框，选 **保留本地** / **保留远程** / **另存副本** |
| 仍报 404 且提示仓库不可访问 | 设置页 **测试连接**；检查 PAT 是否过期、是否授权 `dermotv5chat/notes`、Contents 读写权限 |
| 长期混乱 | 先 **下载** 对齐远程，再编辑，再 **上传** |

## 相关文件

| 文件 | 职责 |
|------|------|
| [`SyncRepository.kt`](../app/src/main/java/com/andriod/reader/data/repository/SyncRepository.kt) | 上传/下载、409/404/422 处理、冲突 resolver |
| [`SyncUploadPolicy.kt`](../app/src/main/java/com/andriod/reader/data/repository/SyncUploadPolicy.kt) | 本地 vs 远程时间戳：自动上传 or 询问用户 |
| [`SyncUploadPaths.kt`](../app/src/main/java/com/andriod/reader/data/repository/SyncUploadPaths.kt) | 远程路径候选列表 |
| [`SyncStateStore.kt`](../app/src/main/java/com/andriod/reader/data/local/SyncStateStore.kt) | `.meta/sync-state.json` 读写 |
| [`NoteFileStore.saveNote()`](../app/src/main/java/com/andriod/reader/data/local/NoteFileStore.kt) | 编辑后设 `PENDING`、保留 `githubSha` |
| [`NoteListViewModel.kt`](../app/src/main/java/com/andriod/reader/ui/list/NoteListViewModel.kt) | 上传/下载入口、冲突对话框 deferred |
| [`SyncUploadPolicyTest.kt`](../app/src/test/java/com/andriod/reader/data/repository/SyncUploadPolicyTest.kt) | 时间戳策略单元测试 |
| [`SyncUploadPathsTest.kt`](../app/src/test/java/com/andriod/reader/data/repository/SyncUploadPathsTest.kt) | 路径回退单元测试 |

## 实践记录（2026-06）

- **复现**：`content/rules.md` 待上传；上传先报 409 JSON，修复 409 逻辑后又报「无法访问 dermotv5chat/notes」404。
- **根因**：409 后 GET 远程文件失败（路径或文件状态），404 被误判为仓库不可访问。
- **修复后**：用户确认上传已恢复正常。

## 后续若再出现类似问题

1. 记录 Snackbar **完整文案**（是否带 `「路径」上传失败` 前缀）。
2. 区分 **409**（版本冲突）与 **404**（路径/权限/文件不存在）。
3. 在设置页执行 **测试连接**，确认 Token 与仓库名。
4. 开发调试可对照 `.meta/sync-state.json` 中该文件的 `githubSha`、`remotePath` 与 GitHub 网页上文件是否一致。

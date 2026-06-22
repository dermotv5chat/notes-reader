# /commit — 暂存、提交，可选 push（默认后台）

按用户要求完成 git 提交。用户可能在命令后追加参数，例如：`/commit -p` 或 `/commit fix: 文件夹笔记路径 -p`。

## 解析用户输入

1. 若用户消息含 `-p` 或 `-Push`（大小写不敏感）→ **需要 push**
2. 若用户消息含 **`-sync`**、**`-wait`**、**「等提交完」**、**「同步执行」** → **前台等待** commit 完成（默认不等待，见下）
3. 去掉 `/commit`、`-p`、`-Push`、`-sync`、`-wait` 后的剩余文字 → 若不为空，作为**用户指定的 commit message**；否则由你根据 diff 撰写

## 主 Agent 做什么（快，不阻塞对话）

在项目根目录 **并行**执行（仅只读，主 Agent 自己跑）：

- `git status`
- `git diff`（含 staged / unstaged）
- `git log -5 --oneline`

根据变更撰写 **1–2 句** commit message（说明 why，遵循仓库近期风格，如 `chore:`、`fix:`、`feat:`）。有用户指定文案则用用户的。

若无变更 → 回复「Nothing to commit」，**不要**启动后台任务。

**禁止**提交：`.env`、`pat.txt`、`local.properties`、密钥、`app/build/` 等构建产物（`commit.sh` 已排除，勿用 `git add -f` 强行加入）。

## 默认：后台 subagent 执行 commit（推荐）

**目的：** push 走 WSL 可能数秒～十几秒，不要在主对话里同步等待，以便用户继续讨论其它功能。

1. 用 **Task 工具** 启动 `subagent_type: shell`，并设 **`run_in_background: true`**
2. Task 描述示例：`git commit push background`
3. Task prompt 须包含完整指令，例如：

   ```
   Full Repository Path: E:\workspace\andriod-reader
   Run exactly one command (PowerShell):
   cd E:\workspace\andriod-reader; .\commit.ps1 -p "此处为 commit message"
   (omit -p if user did not request push)
   Return: exit code, commit hash from git log -1 if success, push result, branch name, or error output.
   ```

4. **主 Agent 立即回复用户**（不要 Await、不要等 subagent）：
   - 拟用的 commit message
   - 将提交的文件摘要（来自 diff）
   - 一句说明：**「提交已在后台进行，完成后会通知；你可以继续聊其它功能。」**
5. Subagent 完成后，系统会推送通知；若失败，在后续回合简短告知用户并给出错误摘要

**不要**在主流程中同步运行 `.\commit.ps1` 或 `git push`，除非用户明确要求 `-sync` / `-wait`。

## 可选：前台同步执行

仅当用户带 `-sync`、`-wait` 或明确要求「提交完再告诉我」时：

```powershell
cd E:\workspace\andriod-reader
.\commit.ps1 "此处为 commit message"
.\commit.ps1 -p "此处为 commit message"
```

**本机 SSH 只在 WSL 配置**，不要直接用 PowerShell 的 `git push`。`-p` 时由 `commit.ps1` 在 WSL 执行 push，无需再单独 `git push`。

WSL 内手动全流程（用户自己在终端跑，非 Agent 默认路径）：

```bash
cd /mnt/e/workspace/andriod-reader
./commit.sh -p "此处为 commit message"
```

## 约束

- 不要 `git push --force` 到 main/master
- 不要改 git config
- 不要用 `--no-verify`，除非用户明确要求
- 无变更则不空提交、不启后台任务
- 只有用户带了 `-p`/`-Push` 才 push；默认只 commit

## 完成后回复

| 模式 | 主 Agent 当轮回复 | 后续 |
|------|-------------------|------|
| **后台（默认）** | message + 文件摘要 +「后台进行中」 | subagent 通知里补 hash / push 结果 |
| **前台 `-sync`** | hash、分支、是否 push、提交了哪些文件 | — |

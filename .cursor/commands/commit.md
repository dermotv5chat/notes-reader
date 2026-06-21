# /commit — 暂存、提交，可选 push

按用户要求完成 git 提交。用户可能在命令后追加参数，例如：`/commit -p` 或 `/commit fix: 文件夹笔记路径 -p`。

## 解析用户输入

1. 若用户消息含 `-p` 或 `-Push`（大小写不敏感）→ **需要 push**
2. 去掉 `/commit`、`-p`、`-Push` 后的剩余文字 → 若不为空，作为**用户指定的 commit message**；否则由你根据 diff 撰写

## 执行步骤（必须亲自跑命令，不要只描述）

在项目根目录并行执行：

- `git status`
- `git diff`（含 staged / unstaged）
- `git log -5 --oneline`

根据变更撰写 **1–2 句** commit message（说明 why，遵循仓库近期风格，如 `chore:`、`fix:`、`feat:`）。有用户指定文案则用用户的。

**禁止**提交：`.env`、`pat.txt`、`local.properties`、密钥、`app/build/` 等构建产物（`commit.sh` 已排除，勿用 `git add -f` 强行加入）。

**本机 SSH 只在 WSL 配置**，不要直接用 PowerShell 的 `git push`。在项目根目录执行：

```powershell
# 推荐：PowerShell 入口，内部自动走 WSL
.\commit.ps1 "此处为 commit message"
.\commit.ps1 -p "此处为 commit message"
```

或在 WSL 内：

```bash
cd /mnt/e/workspace/andriod-reader   # 按实际盘符调整
./commit.sh "此处为 commit message"
./commit.sh -p "此处为 commit message"
```

根据上文「是否需要 push」选择是否加 `-p`。

## 约束

- 不要 `git push --force` 到 main/master
- 不要改 git config
- 不要用 `--no-verify`，除非用户明确要求
- 无变更则说明「Nothing to commit」，不要空提交
- 只有用户带了 `-p`/`-Push` 才 push；默认只 commit

## 完成后回复

简要说明：提交了哪些文件、commit hash、是否已 push、当前分支名。

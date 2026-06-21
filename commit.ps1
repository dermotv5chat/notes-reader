# Stage project changes, commit, and optionally push.
# Git push uses WSL when available (SSH is configured there, not in Windows PowerShell).
# Usage:
#   .\commit.ps1 "chore: update readme"
#   .\commit.ps1 -p "fix: create notes inside current folder"

param(
    [Alias("p")]
    [switch]$Push,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MessageParts
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
Set-Location $Root

$Message = ($MessageParts -join " ").Trim()
if (-not $Message) {
    throw @"
Commit message required.

Examples:
  .\commit.ps1 "chore: update readme"
  .\commit.ps1 -p "fix: folder note path"
"@
}

function ConvertTo-WslPath([string]$WindowsPath) {
    $full = (Resolve-Path $WindowsPath).Path
    if ($full -match '^([A-Za-z]):\\(.*)$') {
        $drive = $matches[1].ToLower()
        $rest = $matches[2] -replace '\\', '/'
        return "/mnt/$drive/$rest"
    }
    throw "Cannot convert path to WSL: $WindowsPath"
}

function Invoke-WslCommit {
    param(
        [bool]$DoPush,
        [string]$CommitMessage
    )
    if (-not (Get-Command wsl -ErrorAction SilentlyContinue)) {
        throw "WSL not found. Run ./commit.sh inside WSL, or install WSL for push via SSH."
    }
    $wslRoot = ConvertTo-WslPath $Root
    $escaped = $CommitMessage -replace "'", "'\\''"
    $pushFlag = if ($DoPush) { "-p" } else { "" }
    $cmd = "cd '$wslRoot' && chmod +x ./commit.sh 2>/dev/null; ./commit.sh $pushFlag '$escaped'"
    Write-Host "==> Using WSL (SSH): wsl bash -lc ..."
    wsl bash -lc $cmd
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

# Prefer WSL: same repo, SSH keys, and git identity as the user's normal workflow.
if (Get-Command wsl -ErrorAction SilentlyContinue) {
    Invoke-WslCommit -DoPush:$Push -CommitMessage $Message
    exit 0
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git not found in PATH and WSL is unavailable."
}

git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Not a git repository: $Root"
}

Write-Host "==> Staging changes (excluding build outputs)..."
git add -A -- . `
    ":(exclude)app/build" `
    ":(exclude)build" `
    ":(exclude).gradle" `
    ":(exclude)local.properties" `
    ":(exclude)pat.txt" `
    ":(exclude).env"

$staged = @(git diff --cached --name-only)
if ($staged.Count -eq 0) {
    Write-Host "Nothing to commit."
    exit 0
}

Write-Host "==> Staged files:"
$staged | ForEach-Object { Write-Host "    $_" }

Write-Host "==> Commit: $Message"
git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if ($Push) {
    Write-Warning "Push skipped: WSL not available. Configure WSL SSH or run ./commit.sh -p in WSL."
    exit 1
}

Write-Host "==> Done. Use -p to push via WSL: .\commit.ps1 -p `"$Message`""

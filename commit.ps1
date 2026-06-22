# Stage project changes, commit, and optionally push.
# Commit uses Windows git; push uses WSL (SSH keys are configured there).
# Usage:
#   .\commit.ps1 "chore: update readme"
#   .\commit.ps1 -p "fix: create notes inside current folder"
#   .\commit.ps1 -Wsl -p "full pipeline in WSL (optional)"

param(
    [Alias("p")]
    [switch]$Push,
    [switch]$Wsl,
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

function Write-CommitMessageFile([string]$Text) {
    $path = Join-Path $env:TEMP ("commit-msg-{0}.txt" -f [guid]::NewGuid().ToString("n"))
    [System.IO.File]::WriteAllText($path, $Text, [System.Text.UTF8Encoding]::new($false))
    return $path
}

function Invoke-StageChanges {
    Write-Host "==> Staging changes (excluding build outputs)..."
    git add -A -- . `
        ":(exclude)app/build" `
        ":(exclude)build" `
        ":(exclude).gradle" `
        ":(exclude)local.properties" `
        ":(exclude)pat.txt" `
        ":(exclude).env"
    if ($LASTEXITCODE -gt 1) {
        exit $LASTEXITCODE
    }
}

function Invoke-WslPush {
    if (-not (Get-Command wsl -ErrorAction SilentlyContinue)) {
        throw "WSL not found. Install WSL or run: wsl ./commit.sh --push-only"
    }
    $wslRoot = ConvertTo-WslPath $Root
    Write-Host "==> Push via WSL (SSH)..."
    wsl.exe bash -lc "cd '$wslRoot' && chmod +x ./commit.sh 2>/dev/null; ./commit.sh --push-only"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Invoke-WslCommit {
    param(
        [bool]$DoPush,
        [string]$CommitMessage
    )
    if (-not (Get-Command wsl -ErrorAction SilentlyContinue)) {
        throw "WSL not found. Run ./commit.sh inside WSL, or install WSL for push via SSH."
    }

    $msgFile = Write-CommitMessageFile $CommitMessage
    try {
        $wslRoot = ConvertTo-WslPath $Root
        $wslMsgFile = ConvertTo-WslPath $msgFile
        $pushFlag = if ($DoPush) { "-p" } else { "" }
        Write-Host "==> Using WSL for stage/commit$(if ($DoPush) { '/push' })..."
        wsl.exe bash -lc "cd '$wslRoot' && chmod +x ./commit.sh 2>/dev/null; COMMIT_MESSAGE_FILE='$wslMsgFile' ./commit.sh $pushFlag"
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    finally {
        Remove-Item $msgFile -Force -ErrorAction SilentlyContinue
    }
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git not found in PATH."
}

git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Not a git repository: $Root"
}

if ($Wsl) {
    Invoke-WslCommit -DoPush:$Push -CommitMessage $Message
    exit 0
}

Invoke-StageChanges

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
    Invoke-WslPush
}
else {
    Write-Host "==> Done. Use -p to push via WSL: .\commit.ps1 -p `"$Message`""
}

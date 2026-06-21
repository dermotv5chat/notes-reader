# Stage project changes, commit, and optionally push.
# Usage:
#   .\commit.ps1 "chore: update readme"
#   .\commit.ps1 -p "fix: create notes inside current folder"
#   .\commit.ps1 -Push "feat: add sleep timer"

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

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git not found in PATH."
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
    $branch = git rev-parse --abbrev-ref HEAD
    Write-Host "==> Push branch: $branch"
    git rev-parse --abbrev-ref "@{u}" *> $null
    if ($LASTEXITCODE -ne 0) {
        git push -u origin $branch
    } else {
        git push
    }
    exit $LASTEXITCODE
}

Write-Host "==> Done. Use -p to push: .\commit.ps1 -p `"$Message`""

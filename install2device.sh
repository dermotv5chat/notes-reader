#!/usr/bin/env bash
# Build debug APK and install to all connected Android devices.
#
# Usage:
#   ./install2device.sh          # Git Bash / WSL
#   DEBUG=1 ./install2device.sh
#
# PowerShell (recommended on Windows): .\install2device.ps1

set -euo pipefail
[[ "${DEBUG:-}" == "1" ]] && set -x

USE_WINDOWS_GRADLE=0
WINDOWS_JAVA_HOME=""

log() {
  printf '[install2device] %s\n' "$*" >&2
}

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
GRADLE_ARGS=(assembleDebug --console=plain)

log "started ($(date '+%H:%M:%S')), project=$ROOT"

is_wsl() {
  grep -qi microsoft /proc/version 2>/dev/null
}

if is_wsl; then
  log "environment: WSL"
else
  log "environment: bash"
fi

normalize_path() {
  local path="$1"
  while [[ "$path" == *//* ]]; do
    path="${path//\/\//\/}"
  done
  echo "$path"
}

decode_properties_path() {
  local raw="$1"
  raw="${raw//$'\r'/}"
  # Java properties: \: -> :, \\ -> \
  raw="$(printf '%s' "$raw" | sed 's/\\:/:/g; s/\\\\/\\/g')"
  printf '%s' "$raw"
}

win_path_to_unix() {
  local path="$1"
  path="$(decode_properties_path "$path")"
  path="${path//\\//}"
  path="$(normalize_path "$path")"

  if [[ "$path" =~ ^/mnt/([a-z])/(.*)$ ]]; then
    echo "/${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
    return
  fi
  if [[ "$path" =~ ^/([A-Za-z])/(.*)$ ]]; then
    echo "/${BASH_REMATCH[1],,}/${BASH_REMATCH[2]}"
    return
  fi
  if [[ "$path" =~ ^([A-Za-z]):/(.*)$ ]]; then
    if is_wsl; then
      echo "/mnt/${BASH_REMATCH[1],,}/${BASH_REMATCH[2]}"
    else
      echo "/${BASH_REMATCH[1],,}/${BASH_REMATCH[2]}"
    fi
    return
  fi
  echo "$path"
}

to_windows_path() {
  local path="$1"
  if is_wsl && command -v wslpath >/dev/null 2>&1; then
    wslpath -w "$path" | tr -d '\r'
    return
  fi
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -w "$path" | tr -d '\r'
    return
  fi
  echo "$path"
}

read_windows_env() {
  command -v cmd.exe >/dev/null 2>&1 || return 0
  if [[ -z "${LOCALAPPDATA:-}" ]]; then
    log "reading LOCALAPPDATA from Windows..."
    LOCALAPPDATA="$(cmd.exe //c "echo %LOCALAPPDATA%" 2>/dev/null | tr -d '\r' || true)"
    export LOCALAPPDATA
  fi
  if [[ -z "${USERPROFILE:-}" ]]; then
    log "reading USERPROFILE from Windows..."
    USERPROFILE="$(cmd.exe //c "echo %USERPROFILE%" 2>/dev/null | tr -d '\r' || true)"
    export USERPROFILE
  fi
}

read_sdk_from_local_properties() {
  local props="$ROOT/local.properties"
  [[ -f "$props" ]] || return 1

  log "reading SDK path from local.properties..."
  local raw unix_path
  raw="$(grep '^sdk.dir=' "$props" | head -1 | cut -d= -f2-)"
  unix_path="$(normalize_path "$(win_path_to_unix "$raw")")"

  if [[ -d "$unix_path" ]]; then
    export ANDROID_HOME="$unix_path"
    return 0
  fi
  log "local.properties sdk.dir not found on disk: $unix_path"
  return 1
}

resolve_linux_java_home() {
  local candidates=(
    "/usr/lib/jvm/java-17-openjdk-amd64"
    "/usr/lib/jvm/java-17-openjdk"
    "/usr/lib/jvm/default-java"
  )
  local dir
  for dir in "${candidates[@]}"; do
    if [[ -d "$dir" && -x "$dir/bin/java" ]]; then
      export JAVA_HOME="$dir"
      return 0
    fi
  done
  if command -v java >/dev/null 2>&1; then
    local java_real
    java_real="$(readlink -f "$(command -v java)" 2>/dev/null || true)"
    if [[ -n "$java_real" ]]; then
      export JAVA_HOME="$(normalize_path "$(dirname "$(dirname "$java_real")")")"
      [[ -d "$JAVA_HOME" ]] && return 0
    fi
  fi
  return 1
}

resolve_windows_jbr_unix() {
  local candidates=()
  if is_wsl; then
    candidates+=("/mnt/c/Program Files/Android/Android Studio/jbr")
  else
    candidates+=("/c/Program Files/Android/Android Studio/jbr")
  fi
  candidates+=("/mnt/c/Program Files/Android/Android Studio/jbr")
  if [[ -n "${PROGRAMFILES:-}" ]]; then
    candidates+=("$(win_path_to_unix "$PROGRAMFILES/Android/Android Studio/jbr")")
  fi

  local dir
  for dir in "${candidates[@]}"; do
    dir="$(normalize_path "$dir")"
    if [[ -d "$dir" ]]; then
      export JAVA_HOME="$dir"
      WINDOWS_JAVA_HOME="$(to_windows_path "$dir")"
      return 0
    fi
  done
  return 1
}

resolve_java_home() {
  log "resolving JAVA_HOME..."

  if [[ -n "${JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$(normalize_path "$(win_path_to_unix "$JAVA_HOME")")"
    if [[ -d "$JAVA_HOME" ]]; then
      WINDOWS_JAVA_HOME="$(to_windows_path "$JAVA_HOME")"
      return
    fi
    log "JAVA_HOME is set but directory missing: $JAVA_HOME"
  fi

  if is_wsl; then
    if resolve_linux_java_home; then
      log "using Linux JDK in WSL: $JAVA_HOME"
      USE_WINDOWS_GRADLE=0
      return
    fi
    log "no Linux JDK in WSL; will build via Windows Gradle + Android Studio JBR"
    USE_WINDOWS_GRADLE=1
  fi

  if resolve_windows_jbr_unix; then
    log "using Android Studio JBR: $JAVA_HOME"
    return
  fi

  echo "error: JAVA_HOME not found." >&2
  if is_wsl; then
    echo "WSL fix: sudo apt install openjdk-17-jdk" >&2
    echo "Or use PowerShell: .\\install2device.ps1" >&2
  fi
  exit 1
}

resolve_android_home() {
  log "resolving ANDROID_HOME..."

  if [[ -n "${ANDROID_HOME:-}" ]]; then
    export ANDROID_HOME="$(normalize_path "$(win_path_to_unix "$ANDROID_HOME")")"
    [[ -d "$ANDROID_HOME" ]] && return
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    export ANDROID_HOME="$(normalize_path "$(win_path_to_unix "$ANDROID_SDK_ROOT")")"
    [[ -d "$ANDROID_HOME" ]] && return
  fi
  if read_sdk_from_local_properties; then
    return
  fi

  read_windows_env

  local win_user="${USERNAME:-${USER:-}}"
  local candidates=()
  if [[ -n "${LOCALAPPDATA:-}" ]]; then
    candidates+=("$(win_path_to_unix "$LOCALAPPDATA/Android/Sdk")")
  fi
  if [[ -n "${USERPROFILE:-}" ]]; then
    candidates+=("$(win_path_to_unix "$USERPROFILE/AppData/Local/Android/Sdk")")
  fi
  if [[ -n "$win_user" ]]; then
    candidates+=(
      "/mnt/c/Users/$win_user/AppData/Local/Android/Sdk"
      "/c/Users/$win_user/AppData/Local/Android/Sdk"
    )
  fi

  local dir
  for dir in "${candidates[@]}"; do
    dir="$(normalize_path "$dir")"
    if [[ -d "$dir" ]]; then
      export ANDROID_HOME="$dir"
      return
    fi
  done

  echo "error: ANDROID_HOME not found." >&2
  exit 1
}

resolve_adb() {
  log "resolving adb..."
  if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
    return
  fi
  if [[ -x "$ANDROID_HOME/platform-tools/adb.exe" ]]; then
    ADB="$ANDROID_HOME/platform-tools/adb.exe"
    return
  fi
  if [[ -x "$ANDROID_HOME/platform-tools/adb" ]]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
    return
  fi
  echo "error: adb not found under $ANDROID_HOME/platform-tools" >&2
  exit 1
}

run_gradle_windows() {
  local root_win java_win bat bat_win rc
  root_win="$(to_windows_path "$ROOT")"
  java_win="${WINDOWS_JAVA_HOME:-$(to_windows_path "$JAVA_HOME")}"
  bat="$ROOT/.install2device-build.bat"

  log "WSL/Windows build via cmd.exe"
  log "  JAVA_HOME (win)=$java_win"
  log "  project (win)=$root_win"
  log "Gradle 下载阶段通常没有进度条，首次约 5~15 分钟，请耐心等待"
  log "若超过 20 分钟无新输出，可 Ctrl+C 后改用 PowerShell: .\\install2device.ps1"

  # Prefer cached Gradle in %%TEMP%% to skip wrapper download; else use gradlew.bat.
  cat > "$bat" <<EOF
@echo off
setlocal
set "JAVA_HOME=$java_win"
cd /d "$root_win"
if exist "%TEMP%\\gradle-8.9\\bin\\gradle.bat" (
  echo [install2device] using cached Gradle: %TEMP%\\gradle-8.9\\bin\\gradle.bat
  call "%TEMP%\\gradle-8.9\\bin\\gradle.bat" ${GRADLE_ARGS[*]}
) else (
  echo [install2device] using gradlew.bat ^(may download Gradle 8.9, no progress bar^)
  call gradlew.bat ${GRADLE_ARGS[*]}
)
set ERR=%ERRORLEVEL%
endlocal & exit /b %ERR%
EOF

  bat_win="$(to_windows_path "$bat")"
  log "running build (see Gradle output below)..."
  cmd.exe /C "$bat_win"
  rc=$?
  rm -f "$bat"
  if [[ $rc -ne 0 ]]; then
    echo "error: Gradle build failed (exit $rc)" >&2
    exit "$rc"
  fi
}

run_gradle() {
  log "building debug APK..."
  log "first build may download Gradle (several minutes); please wait"

  if [[ "$USE_WINDOWS_GRADLE" == "1" ]]; then
    run_gradle_windows
    return
  fi

  export JAVA_HOME

  if [[ -x "$ROOT/gradlew" ]]; then
    log "running: ./gradlew ${GRADLE_ARGS[*]}"
    "$ROOT/gradlew" "${GRADLE_ARGS[@]}"
    return
  fi

  if [[ -f "$ROOT/gradlew.bat" ]] && command -v cmd.exe >/dev/null 2>&1; then
    run_gradle_windows
    return
  fi

  echo "error: no gradle wrapper found." >&2
  exit 1
}

install_to_devices() {
  [[ -f "$APK" ]] || { echo "error: APK not found: $APK" >&2; exit 1; }

  log "checking connected devices..."
  mapfile -t DEVICES < <("$ADB" devices | awk 'NR>1 && $2=="device" { print $1 }')
  if [[ ${#DEVICES[@]} -eq 0 ]]; then
    echo "error: no device connected. Run: $ADB devices" >&2
    exit 1
  fi

  log "installing to ${#DEVICES[@]} device(s)..."
  local serial
  for serial in "${DEVICES[@]}"; do
    log "  -> $serial"
    "$ADB" -s "$serial" install -r "$APK"
  done
  log "done."
}

resolve_java_home
resolve_android_home
log "ANDROID_HOME=$ANDROID_HOME"
log "JAVA_HOME=$JAVA_HOME"
[[ "$USE_WINDOWS_GRADLE" == "1" ]] && log "build mode: Windows Gradle (cmd.exe)"
resolve_adb
log "ADB=$ADB"
run_gradle
install_to_devices

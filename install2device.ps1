# Build debug APK and install to connected Android devices.
# Usage: .\install2device.ps1

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Apk = Join-Path $Root "app\build\outputs\apk\debug\app-debug.apk"

if (-not $env:JAVA_HOME) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
}
if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME not set. Install Android Studio or set JAVA_HOME to JDK 17."
}
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
}

if (-not $env:ANDROID_HOME) {
    $localProps = Join-Path $Root "local.properties"
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $sdk = ($line -replace '^sdk\.dir=', '').Replace('\:', ':').Replace('\\', '\')
            $env:ANDROID_HOME = $sdk
        }
    }
}
if (-not $env:ANDROID_HOME) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $defaultSdk) { $env:ANDROID_HOME = $defaultSdk }
}
if (-not $env:ANDROID_HOME) {
    throw "ANDROID_HOME not found. Open project in Android Studio once, or set ANDROID_HOME."
}

$Adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
if (-not (Test-Path $Adb)) {
    throw "adb not found at $Adb. Install Android SDK Platform-Tools."
}

Write-Host "==> ANDROID_HOME=$($env:ANDROID_HOME)"
Write-Host "==> JAVA_HOME=$($env:JAVA_HOME)"
Write-Host "==> Building debug APK..."

$GradleBat = Join-Path $env:TEMP "gradle-8.9\bin\gradle.bat"
if (Test-Path $GradleBat) {
    & $GradleBat assembleDebug
} else {
    & (Join-Path $Root "gradlew.bat") assembleDebug
}

if (-not (Test-Path $Apk)) {
    throw "APK not found: $Apk"
}

$devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "`tdevice$" } |
    ForEach-Object { ($_ -split "`t")[0] }

if (-not $devices) {
    throw "No device connected. Run: $Adb devices"
}

Write-Host "==> Installing to $($devices.Count) device(s)..."
foreach ($serial in $devices) {
    Write-Host "    -> $serial"
    & $Adb -s $serial install -r $Apk
}
Write-Host "==> Done."

# Run unit tests (includes Compose UI tests via Robolectric; no device required).
# Usage:
#   .\runAndroidTest.ps1
#   .\runAndroidTest.ps1 -TestClass com.andriod.reader.ui.reader.ReaderSleepTimerSheetTest
#
# For on-device instrumented tests (optional, slower):
#   .\runAndroidTest.ps1 -Instrumented

param(
    [string]$TestClass = $null,
    [switch]$Instrumented
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
}
if (-not $env:JAVA_HOME) {
    throw @"
JAVA_HOME not set.
Install Android Studio, or run once in this shell:
  `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
"@
}

$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
}

Write-Host "==> JAVA_HOME=$($env:JAVA_HOME)"

if ($Instrumented) {
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
        throw "ANDROID_HOME not found."
    }
    Write-Host "==> ANDROID_HOME=$($env:ANDROID_HOME)"
    Write-Host "==> Cleaning previous androidTest outputs..."
    & (Join-Path $Root "gradlew.bat") --stop | Out-Null
    $staleDirs = @(
        (Join-Path $Root "app\build\outputs\androidTest-results"),
        (Join-Path $Root "app\build\outputs\connected_android_test_additional_output"),
        (Join-Path $Root "app\build\reports\androidTests")
    )
    foreach ($dir in $staleDirs) {
        if (Test-Path $dir) {
            Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue
        }
    }
    $gradleArgs = @(":app:connectedDebugAndroidTest", "--no-daemon", "--no-build-cache")
    if ($TestClass) {
        $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.class=$TestClass"
    }
    & (Join-Path $Root "gradlew.bat") @gradleArgs
    exit $LASTEXITCODE
}

$testArgs = @(":app:testDebugUnitTest", "--no-daemon")
if ($TestClass) {
    Write-Host "==> Running testDebugUnitTest ($TestClass)..."
    $testArgs += "--tests"
    $testArgs += $TestClass
} else {
    Write-Host "==> Running testDebugUnitTest (all)..."
}

& (Join-Path $Root "gradlew.bat") @testArgs
exit $LASTEXITCODE

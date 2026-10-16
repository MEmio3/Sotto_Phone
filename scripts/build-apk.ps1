$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    if (!(Test-Path -LiteralPath 'local.properties')) {
        if ($env:ANDROID_HOME -and (Test-Path -LiteralPath $env:ANDROID_HOME)) {
            [IO.File]::WriteAllText((Join-Path $projectRoot 'local.properties'), 'sdk.dir=' + $env:ANDROID_HOME.Replace('\','/') + "`n")
        } else { & (Join-Path $PSScriptRoot 'setup-android.ps1') }
    }
    node (Join-Path $PSScriptRoot 'prepare-signing.cjs')
    if ($LASTEXITCODE -ne 0) { throw 'Signing setup failed.' }
    & '.\gradlew.bat' :app:testDebugUnitTest :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android build or tests failed.' }
    New-Item -ItemType Directory -Path 'dist' -Force | Out-Null
    Copy-Item -LiteralPath 'app\build\outputs\apk\release\app-release.apk' -Destination 'dist\Sotto-0.1.0-Android.apk' -Force
    $hash=(Get-FileHash -LiteralPath 'dist\Sotto-0.1.0-Android.apk' -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText((Join-Path $projectRoot 'dist\SHA256SUMS.txt'), $hash + "  Sotto-0.1.0-Android.apk`n")
    Write-Output 'APK ready: dist\Sotto-0.1.0-Android.apk'
} finally { Pop-Location }

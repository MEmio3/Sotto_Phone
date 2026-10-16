$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    if (!(Test-Path -LiteralPath 'local.properties')) {
        if ($env:ANDROID_HOME -and (Test-Path -LiteralPath $env:ANDROID_HOME)) {
            [IO.File]::WriteAllText((Join-Path $projectRoot 'local.properties'), 'sdk.dir=' + $env:ANDROID_HOME.Replace('\','/').Replace(':','\:') + "`n")
        } else { & (Join-Path $PSScriptRoot 'setup-android.ps1') }
    }
    node (Join-Path $PSScriptRoot 'prepare-signing.cjs')
    if ($LASTEXITCODE -ne 0) { throw 'Signing setup failed.' }
    & '.\gradlew.bat' :app:testDebugUnitTest :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android build or tests failed.' }
    New-Item -ItemType Directory -Path 'dist' -Force | Out-Null
    $appVersion=[regex]::Match([IO.File]::ReadAllText((Join-Path $projectRoot 'app\build.gradle')),"versionName '([0-9]+\.[0-9]+\.[0-9]+)'").Groups[1].Value
    if(!$appVersion){throw 'Could not read the Android app version.'}
    $apkName="Sotto-$appVersion-Android.apk"
    $apkPath=Join-Path $projectRoot "dist\$apkName"
    Copy-Item -LiteralPath 'app\build\outputs\apk\release\app-release.apk' -Destination $apkPath -Force
    $hash=(Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText((Join-Path $projectRoot 'dist\SHA256SUMS.txt'), $hash + "  $apkName`n")
    Write-Output "APK ready: dist\$apkName"
} finally { Pop-Location }

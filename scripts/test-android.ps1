param([string]$Serial='emulator-5560')
$ErrorActionPreference='Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'These checks modify test data and keyboard settings. Use a disposable Android emulator, not a personal phone.' }
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $adb=Join-Path $projectRoot '.android-sdk\platform-tools\adb.exe'
    if (!(Test-Path -LiteralPath $adb) -and $env:ANDROID_HOME) { $adb=Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }
    $boot=& $adb -s $Serial shell getprop sys.boot_completed
    if ($boot.Trim() -ne '1') { throw 'Wait for the disposable emulator to finish booting before running tests.' }
    & '.\gradlew.bat' :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain | Out-File -LiteralPath '.tools\native-build.log' -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw 'The test APK could not be built. See .tools/native-build.log.' }
    & $adb -s $Serial install -r 'app\build\outputs\apk\debug\app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Could not install the app on the emulator.' }
    & $adb -s $Serial install -r -t 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Could not install the test fixture.' }
    & $adb -s $Serial shell am force-stop studio.sotto.mobile.test
    $report=& $adb -s $Serial shell am instrument -w -r studio.sotto.mobile.test/studio.sotto.mobile.SmokeInstrumentation
    $report | Tee-Object -FilePath '.tools\native-test.log'
    if (($report -join "`n") -notmatch '"ok"\s*:\s*true') { throw 'Android checks did not pass. See .tools/native-test.log.' }
    New-Item -ItemType Directory -Path '.impeccable\review' -Force | Out-Null
    & $adb -s $Serial pull /sdcard/Android/data/studio.sotto.mobile/files/review/. '.impeccable/review/'
    Write-Output 'Native Android checks passed. Screenshots: .impeccable/review/'
} finally { Pop-Location }

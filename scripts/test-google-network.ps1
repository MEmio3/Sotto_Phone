param([string]$Serial='emulator-5562')
$ErrorActionPreference='Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Use a disposable Android emulator: this check changes Data Saver and Wi-Fi settings.' }
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $adb=Join-Path $projectRoot '.android-sdk\platform-tools\adb.exe'
    if (!(Test-Path -LiteralPath $adb) -and $env:ANDROID_HOME) { $adb=Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }
    function Invoke-Adb { param([string[]]$Arguments) $output=& $adb -s $Serial @Arguments 2>&1; if($LASTEXITCODE -ne 0){throw "ADB command failed: $($Arguments[0])"}; return ($output -join "`n") }
    function Open-Sotto { Invoke-Adb -Arguments @('shell','am','start','-W','-n','studio.sotto.mobile/.MainActivity') | Out-Null }
    function Start-Probe([string]$Stage) { Invoke-Adb -Arguments @('shell','am','startservice','-n','studio.sotto.mobile/.NetworkProbeService','--es','stage',$Stage) | Out-Null }
    function Read-Probe([string]$Stage,[bool]$AllowWaiting=$false) {
        $ErrorActionPreference='Continue'
        $deadline=[DateTime]::UtcNow.AddSeconds(45)
        while([DateTime]::UtcNow -lt $deadline){
            $value=& $adb -s $Serial shell run-as studio.sotto.mobile cat "files/google-network-$Stage.txt" 2>$null
            if($LASTEXITCODE -eq 0){$report=$value -join "`n";if($AllowWaiting -or $report -ne 'waiting-for-return'){return $report}}
            Start-Sleep -Milliseconds 500
        }
        throw "Timed out reading $Stage probe."
    }
    New-Item -ItemType Directory -Path '.tools' -Force | Out-Null
    & '.\gradlew.bat' :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain | Out-File -LiteralPath '.tools\google-network-build.log' -Encoding utf8
    if($LASTEXITCODE -ne 0){throw 'Debug build failed. See .tools/google-network-build.log.'}
    Invoke-Adb -Arguments @('install','-r','app\build\outputs\apk\debug\app-debug.apk') | Out-Null
    Invoke-Adb -Arguments @('install','-r','-t','app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk') | Out-Null
    Invoke-Adb -Arguments @('shell','am','force-stop','studio.sotto.mobile') | Out-Null
    Invoke-Adb -Arguments @('shell','am','force-stop','studio.sotto.mobile.test') | Out-Null
    Invoke-Adb -Arguments @('shell','run-as','studio.sotto.mobile','rm','-f','files/google-network-foreground.txt','files/google-network-background.txt','files/google-network-handoff.txt') | Out-Null
    Invoke-Adb -Arguments @('shell','cmd','netpolicy','set','restrict-background','false') | Out-Null
    Invoke-Adb -Arguments @('shell','svc','data','enable') | Out-Null
    Invoke-Adb -Arguments @('shell','svc','wifi','disable') | Out-Null
    Start-Sleep -Seconds 12
    Open-Sotto; Start-Probe 'foreground'; $foreground=Read-Probe 'foreground'
    if(([regex]::Matches($foreground,'reached Google \(HTTP 400\)')).Count -ne 2){throw "Foreground endpoints were not both reachable: $foreground"}
    Invoke-Adb -Arguments @('shell','am','force-stop','studio.sotto.mobile') | Out-Null
    Invoke-Adb -Arguments @('shell','cmd','netpolicy','set','restrict-background','true') | Out-Null
    Open-Sotto
    Start-Probe 'background'
    Invoke-Adb -Arguments @('shell','am','start','-W','-n','studio.sotto.mobile.test/studio.sotto.mobile.EditorFixtureActivity') | Out-Null
    $background=Read-Probe 'background'
    if($background -notmatch 'App visible: no' -or $background -notmatch 'Data Saver: background data restricted' -or $background -match 'reached Google' -or $background -notmatch 'UnknownHostException|SocketTimeoutException|ConnectException|SocketException'){throw "Expected blocked background requests: $background"}
    # The new flow avoids a background request entirely. Use a fresh process so the
    # deliberately failing baseline cannot poison the new flow's DNS/HTTP state.
    Invoke-Adb -Arguments @('shell','am','force-stop','studio.sotto.mobile') | Out-Null
    Open-Sotto; Start-Probe 'handoff'
    Invoke-Adb -Arguments @('shell','am','start','-W','-n','studio.sotto.mobile.test/studio.sotto.mobile.EditorFixtureActivity') | Out-Null
    $waiting=Read-Probe 'handoff' $true
    if($waiting -ne 'waiting-for-return'){throw "The foreground gate did not wait: $waiting"}
    Start-Sleep -Milliseconds 500
    if((Read-Probe 'handoff' $true) -ne 'waiting-for-return'){throw 'Token probes ran while Sotto was backgrounded.'}
    Invoke-Adb -Arguments @('shell','am','start','-W','-a','android.intent.action.VIEW','-d','studio.sotto.mobile://signin') | Out-Null
    $handoff=Read-Probe 'handoff'
    if($handoff -notmatch 'App visible: yes' -or ([regex]::Matches($handoff,'reached Google \(HTTP 400\)')).Count -ne 2){throw "Foreground return did not reach both endpoints: $handoff"}
    $proof="Normal app UID; no instrumentation runner or account credentials.`n`nFOREGROUND`n$foreground`n`nDATA SAVER BACKGROUND`n$background`n`nHANDOFF`nToken work waited for Return to Sotto: true`n$handoff`n`ncompleted: true`n"
    $proof | Tee-Object -FilePath '.tools\google-android-network-normal-uid.log'
} finally {
    if($adb){& $adb -s $Serial shell cmd netpolicy set restrict-background false | Out-Null;& $adb -s $Serial shell svc wifi enable | Out-Null}
    Pop-Location
}

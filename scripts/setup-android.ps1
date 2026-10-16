$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
$sdkRoot = Join-Path $projectRoot '.android-sdk'
New-Item -ItemType Directory -Path $toolsRoot,$sdkRoot -Force | Out-Null
$metadata = Join-Path $toolsRoot 'android-repository.xml'
Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/repository2-1.xml' -OutFile $metadata -UseBasicParsing
[xml]$repository = Get-Content -LiteralPath $metadata -Raw
$packages = $repository.SelectNodes("//*[local-name()='remotePackage']")
$package = $packages | Where-Object { $_.path -eq 'cmdline-tools;19.0' } | Select-Object -First 1
if (!$package) { throw 'Google did not return Android command-line tools metadata.' }
$archive = $package.archives.archive | Where-Object { $_.'host-os' -eq 'windows' } | Select-Object -First 1
$zip = Join-Path $toolsRoot 'commandlinetools.zip'
$env:SOTTO_SDK_URL = 'https://dl.google.com/android/repository/' + $archive.complete.url
$env:SOTTO_SDK_ZIP = $zip
node -e "const fs=require('node:fs'); const {Readable}=require('node:stream'); const {pipeline}=require('node:stream/promises'); (async()=>{const r=await fetch(process.env.SOTTO_SDK_URL,{signal:AbortSignal.timeout(1200000)});if(!r.ok)throw Error('Download HTTP '+r.status);await pipeline(Readable.fromWeb(r.body),fs.createWriteStream(process.env.SOTTO_SDK_ZIP));console.log('Android tools downloaded.');})().catch(e=>{console.error(e.message);process.exitCode=1});"
if ($LASTEXITCODE -ne 0) { throw 'Android command-line tools download failed.' }
$expected = [string]$archive.complete.checksum
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA1).Hash.ToLowerInvariant() -ne $expected.ToLowerInvariant()) { throw 'Android tools checksum did not match Google metadata.' }
$staging = Join-Path $toolsRoot 'commandlinetools-extracted'
Expand-Archive -LiteralPath $zip -DestinationPath $staging -Force
$destination = Join-Path $sdkRoot 'cmdline-tools\latest'
New-Item -ItemType Directory -Path $destination -Force | Out-Null
Copy-Item -Path (Join-Path $staging 'cmdline-tools\*') -Destination $destination -Recurse -Force
$manager = Join-Path $destination 'bin\sdkmanager.bat'
1..12 | ForEach-Object { 'y' } | & $manager --sdk_root=$sdkRoot 'platform-tools' 'platforms;android-36' 'build-tools;36.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK setup failed.' }
$local = 'sdk.dir=' + $sdkRoot.Replace('\','/')
[IO.File]::WriteAllText((Join-Path $projectRoot 'local.properties'), $local + "`n")
Write-Output 'Android SDK is ready for Sotto.'

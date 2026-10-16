@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "scripts\build-apk.ps1"
if errorlevel 1 (
  echo The Android build did not finish. See the error above.
  pause
  exit /b 1
)
start "" "%~dp0dist"

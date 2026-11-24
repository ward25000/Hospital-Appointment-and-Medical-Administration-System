@echo off
setlocal
title MedFlow - full reset
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0full-reset.ps1" %*
set "CODE=%ERRORLEVEL%"
echo.
if not "%CODE%"=="0" echo [full-reset] Failed with exit code %CODE%. See the text above.
echo Press any key to close this window...
pause >nul
exit /b %CODE%

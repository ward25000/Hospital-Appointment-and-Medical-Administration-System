@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1" %*
set "CODE=%ERRORLEVEL%"
echo.
if not "%CODE%"=="0" echo [setup] Failed with exit code %CODE%. See the text above.
echo Press any key to close this window...
pause >nul
exit /b %CODE%

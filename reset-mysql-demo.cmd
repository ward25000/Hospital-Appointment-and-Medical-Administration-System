@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0reset-mysql-demo.ps1" %*
set "CODE=%ERRORLEVEL%"
echo.
echo Press any key to close this window...
pause >nul
exit /b %CODE%

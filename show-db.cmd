@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0db.ps1" -Mode show
echo.
pause

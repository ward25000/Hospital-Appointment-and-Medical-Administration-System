#requires -Version 5.1
<#
    MedFlow - stop everything.

      - stops the application listening on port 8080
      - stops the database through "docker compose down"

    Run: double-click stop.cmd
#>
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

$ErrorActionPreference = 'Continue'
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsRoot = "$env:USERPROFILE\.medflow-tools"

$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' +
            [Environment]::GetEnvironmentVariable('Path', 'User')

Write-Host ""
Write-Host "=== Останавливаю MedFlow ===" -ForegroundColor Cyan

$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    foreach ($procId in ($listener.OwningProcess | Sort-Object -Unique)) {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host "  остановлен процесс $procId (порт 8080)" -ForegroundColor Green
    }
} else {
    Write-Host "  приложение не запущено (8080 свободен)" -ForegroundColor DarkGray
}

# Maven often leaves a wrapper process around; stop it too.
Get-CimInstance Win32_Process |
    Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -match 'maven' } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "  остановлен Maven $($_.ProcessId)" -ForegroundColor Green
    }

Push-Location $ProjectRoot
if (Get-Command docker -ErrorAction SilentlyContinue) {
    & docker compose down | Out-Null
} else {
    $shim = Join-Path $ToolsRoot 'bin\docker.cmd'
    if (Test-Path $shim) { & $shim compose down | Out-Null }
}
Pop-Location

Write-Host "Готово." -ForegroundColor Green

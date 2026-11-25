#requires -Version 5.1
<#
    MedFlow - остановка приложения и базы. Двойной клик по stop.cmd.
#>
param([string]$MySqlHome)
. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'medflow-common.ps1')
$ErrorActionPreference = 'Continue'
Write-Host ''
Write-Host '=== Останавливаю MedFlow ===' -ForegroundColor Cyan
$code = 0
try { Stop-MfApp 8080 } catch { Write-Host "  $($_.Exception.Message)" -ForegroundColor Yellow; $code = 1 }
Stop-MfMySql (Resolve-MfMySqlHome $MySqlHome)
Write-Host 'Готово.' -ForegroundColor Green
exit $code

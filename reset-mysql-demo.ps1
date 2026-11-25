#requires -Version 5.1
<#
    MedFlow - сброс данных: база пересоздаётся, демо-данные появятся при следующем запуске.
    Перед сбросом делается копия в папку backups. Приложение останавливается само.
    Если не работает сама MySQL, нужен full-reset.cmd.
#>
param(
    [string]$MySqlHome,
    [int]$Port = 3306,
    [switch]$Yes
)
. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'medflow-common.ps1')
$ErrorActionPreference = 'Continue'

try {
    Write-Host ''
    Write-Host '=== MedFlow: сброс данных ===' -ForegroundColor Yellow
    if (-not $Yes) {
        Write-Host 'Все записи, пациенты и платежи будут заменены демо-данными.'
        if ((Read-Host 'Для подтверждения введи YES') -ne 'YES') { Write-Host 'Отменено.'; exit 0 }
    }
    $mysqlFolder = Resolve-MfMySqlHome $MySqlHome
    if ($Port -eq 3306) { Stop-MfApp 8080 }
    Start-MfMySql $mysqlFolder $Port
    $copy = Backup-MfDatabase $mysqlFolder $Port
    if ($copy) { Write-Host "  копия: $copy" -ForegroundColor Green }
    $result = Invoke-MfSql $mysqlFolder $Port $script:MfUser $script:MfPassword `
        "DROP DATABASE IF EXISTS $script:MfDatabase; CREATE DATABASE $script:MfDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if (-not $result.Ok) { throw "MySQL не выполнила сброс: $($result.Output)" }
    Write-Host 'Готово. Запусти start.cmd: база заполнится демо-данными.' -ForegroundColor Green
    exit 0
} catch {
    Write-Host ''
    Write-Host "ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Если не получается, запусти full-reset.cmd.' -ForegroundColor Yellow
    exit 1
}

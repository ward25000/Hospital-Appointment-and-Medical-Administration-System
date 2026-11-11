#requires -Version 5.1
<#
    MedFlow - reset the MySQL demo database.

    Drops and re-creates hospital_db. On the next application start Flyway
    re-applies V1 + V2, so the demo data gets fresh dates relative to today.

    Stop the application FIRST (Ctrl+C in its window), otherwise it keeps
    using the old schema.

    Usage:
        powershell -ExecutionPolicy Bypass -File .\reset-mysql-demo.ps1
#>

param(
    [string]$MySQLHome = 'C:\Users\Happy\mysql84',
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Database = 'hospital_db',
    [string]$RootUser = 'root',
    [string]$RootPassword = 'root_pass'
)

$ErrorActionPreference = 'Stop'

$mysql = Join-Path $MySQLHome 'bin\mysql.exe'
if (-not (Test-Path $mysql)) {
    Write-Host "mysql.exe not found: $mysql" -ForegroundColor Red
    Write-Host 'Start MySQL first:' -ForegroundColor Yellow
    Write-Host "    powershell -ExecutionPolicy Bypass -File $(Join-Path $MySQLHome 'mysql-start.ps1')"
    exit 2
}

if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
    Write-Host "MySQL is not listening on port $Port." -ForegroundColor Red
    Write-Host 'Start it first:' -ForegroundColor Yellow
    Write-Host "    powershell -ExecutionPolicy Bypass -File $(Join-Path $MySQLHome 'mysql-start.ps1')"
    exit 2
}

if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host 'The application is still running on port 8080.' -ForegroundColor Yellow
    Write-Host 'Stop it (Ctrl+C in its window), then run this script again.' -ForegroundColor Yellow
    exit 2
}

Write-Host "Resetting demo database '$Database' ..." -ForegroundColor Cyan
$sql = "DROP DATABASE IF EXISTS $Database; CREATE DATABASE $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
$args = @(
    '--protocol=tcp', "--host=$HostName", "--port=$Port", "--user=$RootUser",
    '--default-character-set=utf8mb4', '-e', $sql
)
$env:MYSQL_PWD = $RootPassword
try {
    & $mysql @args
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to reset database $Database (check the root password)."
    }
} finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host 'Database is empty. Flyway will re-create the schema and demo data on the next start.' -ForegroundColor Green
Write-Host ''
Write-Host 'Now start the application:' -ForegroundColor Cyan
Write-Host '    powershell -ExecutionPolicy Bypass -File .\run-demo.ps1'

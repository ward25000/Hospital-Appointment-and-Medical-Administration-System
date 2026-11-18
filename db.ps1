#requires -Version 5.1
<#
    MedFlow - работа с базой данных одной командой.

    Режимы:
      -Mode show     - короткий обзор базы (таблицы, строки, проверки)
      -Mode console  - открыть интерактивную консоль mysql>
      -Mode gui      - открыть HeidiSQL, если он установлен

    Вызов из .cmd-файлов: show-db.cmd, open-mysql.cmd, open-db-gui.cmd
#>
param(
    [ValidateSet('show', 'console', 'gui')]
    [string]$Mode = 'show',
    [string]$MySqlHome = "$env:USERPROFILE\mysql84"
)

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

$mysql = Join-Path $MySqlHome 'bin\mysql.exe'
if (-not (Test-Path $mysql)) {
    Write-Host "MySQL not found in $MySqlHome. Run setup.cmd first." -ForegroundColor Red
    exit 1
}

# ВАЖНО: --host=..., а не -h127.0.0.1. PowerShell "съедает" -h127.0.0.1 до "-h127".
$common = @(
    '--protocol=tcp',
    '--host=127.0.0.1',
    '--port=3306',
    '--user=root',
    '--default-character-set=utf8mb4',
    '--database=hospital_db'
)

function Invoke-Sql {
    param([string]$Sql)
    & $mysql @common -e $Sql 2>&1
}

if ($Mode -eq 'console') {
    Write-Host "Opening MySQL console. Type 'exit' to close.`n" -ForegroundColor Cyan
    & $mysql @common
    exit $LASTEXITCODE
}

if ($Mode -eq 'gui') {
    $candidates = @(
        "$env:ProgramFiles\HeidiSQL\heidisql.exe",
        "${env:ProgramFiles(x86)}\HeidiSQL\heidisql.exe",
        "$env:LOCALAPPDATA\Programs\HeidiSQL\heidisql.exe",
        "$env:USERPROFILE\HeidiSQL\heidisql.exe"
    )
    $found = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($found) {
        Write-Host "Starting HeidiSQL: $found" -ForegroundColor Cyan
        Start-Process -FilePath $found
    } else {
        Write-Host "HeidiSQL is not installed. Download the portable version here:" -ForegroundColor Yellow
        Write-Host "  https://www.heidisql.com/download.php" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Then connect with these settings:" -ForegroundColor Yellow
    }
    Write-Host @"

    Network type : MySQL (TCP/IP)
    Host         : 127.0.0.1
    Port         : 3306
    User         : hospital
    Password     : hospital_pass
    Database     : hospital_db

"@
    exit 0
}

# --- Mode 'show' --------------------------------------------------------------
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  MedFlow - DATABASE hospital_db (MySQL 8.4)" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

Write-Host "`n1) MySQL version:" -ForegroundColor Cyan
Invoke-Sql "SELECT VERSION() AS version;"

Write-Host "`n2) Tables:" -ForegroundColor Cyan
Invoke-Sql "SHOW TABLES;"

Write-Host "`n3) Row counts:" -ForegroundColor Cyan
Invoke-Sql @"
SELECT 'doctors' AS table_name, COUNT(*) AS row_count FROM doctors
UNION ALL SELECT 'patients', COUNT(*) FROM patients
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'medical_services', COUNT(*) FROM medical_services
UNION ALL SELECT 'appointments', COUNT(*) FROM appointments
UNION ALL SELECT 'referrals', COUNT(*) FROM referrals
UNION ALL SELECT 'flyway_schema_history', COUNT(*) FROM flyway_schema_history;
"@

Write-Host "`n4) Appointments by status:" -ForegroundColor Cyan
Invoke-Sql "SELECT status, COUNT(*) AS total FROM appointments GROUP BY status ORDER BY status;"

Write-Host "`n5) Last 8 appointments:" -ForegroundColor Cyan
Invoke-Sql @"
SELECT id, doctor_id, patient_id, start_time, end_time, status, price
FROM appointments ORDER BY id DESC LIMIT 8;
"@

Write-Host "`n6) Flyway migrations applied:" -ForegroundColor Cyan
Invoke-Sql "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

Write-Host "`n7) Unique index (prevents double booking):" -ForegroundColor Cyan
Invoke-Sql @"
SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'hospital_db'
  AND TABLE_NAME = 'appointments'
  AND INDEX_NAME = 'uk_appointments_doctor_slot_status'
ORDER BY SEQ_IN_INDEX;
"@

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "  Done. To look around yourself: run open-mysql.cmd" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

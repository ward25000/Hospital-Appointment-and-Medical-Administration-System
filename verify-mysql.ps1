#requires -Version 5.1
<#
    MedFlow — MySQL-side verification.

    Proves that the running application really uses MySQL, that Flyway created
    the schema, that demo data is present and that the database itself blocks
    double booking of the same doctor slot.

    Prerequisites:
        1) portable MySQL is running:
               powershell -ExecutionPolicy Bypass -File C:\Users\Happy\mysql84\mysql-start.ps1
        2) the application has been started once against MySQL:
               .\mvnw.cmd spring-boot:run
        3) the application is still running (for the health check).

    Usage:
        powershell -ExecutionPolicy Bypass -File .\verify-mysql.ps1
#>

param(
    [string]$MySQLHome = 'C:\Users\Happy\mysql84',
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Database = 'hospital_db',
    [string]$User = 'hospital',
    [string]$Password = 'hospital_pass',
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Continue'
$script:passed = 0
$script:failed = 0

$mysql = Join-Path $MySQLHome 'bin\mysql.exe'
if (-not (Test-Path $mysql)) {
    Write-Host "mysql.exe not found: $mysql" -ForegroundColor Red
    Write-Host 'Pass -MySQLHome <path> if MySQL lives in another folder.' -ForegroundColor Yellow
    exit 2
}

function Check {
    param([string]$Name, [bool]$Ok, [string]$Details = '')
    if ($Ok) {
        $script:passed++
        Write-Host ("[PASS] {0} {1}" -f $Name, $Details) -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host ("[FAIL] {0} {1}" -f $Name, $Details) -ForegroundColor Red
    }
}

function Invoke-Sql {
    param([string]$Sql)
    $env:MYSQL_PWD = $Password
    $args = @(
        '--protocol=tcp', "--host=$HostName", "--port=$Port", "--user=$User",
        "--database=$Database",
        '--default-character-set=utf8mb4', '-N', '-B', '-e', $Sql
    )
    $output = & $mysql @args 2>$null
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    return $output
}

function Get-Scalar {
    param([string]$Sql)
    $rows = @(Invoke-Sql $Sql)
    if ($rows.Count -eq 0) { return '' }
    return ([string]$rows[0]).Trim()
}

Write-Host "MedFlow MySQL verification (database $Database at ${HostName}:$Port)" -ForegroundColor Cyan

# --- 1. Server identity ------------------------------------------------------
$version = Get-Scalar 'SELECT VERSION();'
Check 'MySQL server is reachable' ($version -ne '') "version=$version"
Check 'Server is MySQL 8.x' ($version -match '^8\.') "version=$version"

$engineRow = @(Invoke-Sql "SELECT @@default_storage_engine, @@character_set_server;")
$engine = ([string]$engineRow[0]).Split("`t")
Check 'InnoDB + utf8mb4' ($engine[0] -eq 'InnoDB' -and $engine[1] -like 'utf8mb4*') `
    "engine=$($engine[0]), charset=$($engine[1])"

$readOnly = Get-Scalar 'SELECT @@read_only;'
Check 'Server accepts writes' ($readOnly -eq '0') "read_only=$readOnly"

# --- 2. Flyway migrations ----------------------------------------------------
$migrations = @(Invoke-Sql 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;')
$migrationText = ($migrations | ForEach-Object { [string]$_ }) -join '; '
Check 'Flyway applied V1 + V2' `
    ($migrations.Count -ge 2 -and (($migrations | ForEach-Object { ([string]$_).Split("`t")[2] }) -notcontains '0')) `
    $migrationText

# --- 3. Schema ---------------------------------------------------------------
$tableCount = Get-Scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$Database' AND table_type = 'BASE TABLE';"
$tableNames = @(Invoke-Sql "SELECT table_name FROM information_schema.tables WHERE table_schema = '$Database' AND table_type = 'BASE TABLE' ORDER BY table_name;")
$expectedTables = @('appointments', 'doctor_schedules', 'doctors', 'flyway_schema_history', 'medical_services', 'patients', 'specializations', 'users')
$missing = $expectedTables | Where-Object { $tableNames -notcontains $_ }
Check 'All expected tables exist' ([int]$tableCount -ge 8 -and $missing.Count -eq 0) `
    "tables=$tableCount, missing=$($missing -join ',')"

$indexColumns = Get-Scalar @"
SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index)
FROM information_schema.statistics
WHERE table_schema = '$Database'
  AND table_name = 'appointments'
  AND index_name = 'uk_appointments_doctor_slot_status';
"@
Check 'Unique slot index exists' ($indexColumns -eq 'doctor_id,start_time,status') "columns=$indexColumns"

# --- 4. Demo data ------------------------------------------------------------
$doctorCount = Get-Scalar 'SELECT COUNT(*) FROM doctors;'
$patientCount = Get-Scalar 'SELECT COUNT(*) FROM patients;'
$serviceCount = Get-Scalar 'SELECT COUNT(*) FROM medical_services;'
Check 'Demo doctors / patients / services exist' `
    ([int]$doctorCount -ge 4 -and [int]$patientCount -ge 12 -and [int]$serviceCount -ge 8) `
    "doctors=$doctorCount, patients=$patientCount, services=$serviceCount"

$statusRows = @(Invoke-Sql "SELECT status, COUNT(*) FROM appointments GROUP BY status ORDER BY status;")
$statusMap = @{}
foreach ($row in $statusRows) {
    $parts = ([string]$row).Split("`t")
    if ($parts.Count -ge 2) { $statusMap[$parts[0]] = [int]$parts[1] }
}
$statusText = ($statusMap.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ', '
$allStatuses = $statusMap.ContainsKey('SCHEDULED') -and $statusMap.ContainsKey('COMPLETED') -and $statusMap.ContainsKey('CANCELLED')
Check 'All three statuses present' ($allStatuses -and $statusMap['SCHEDULED'] -gt 0 -and $statusMap['COMPLETED'] -gt 0 -and $statusMap['CANCELLED'] -gt 0) `
    $statusText

$completed = @(Invoke-Sql "SELECT COUNT(*), COALESCE(SUM(price), 0) FROM appointments WHERE status = 'COMPLETED';")
$completedParts = if ($completed.Count -gt 0) { ([string]$completed[0]).Split("`t") } else { @('', '') }
Check 'Completed visits carry a price' ($completedParts.Count -ge 2 -and [int]$completedParts[0] -gt 0 -and [decimal]$completedParts[1] -gt 0) `
    "completed=$($completedParts[0]), total=$($completedParts[1])"

# --- 5. Business rule: no overlapping scheduled visits -----------------------
$overlaps = Get-Scalar @"
SELECT COUNT(*)
FROM appointments a
JOIN appointments b
  ON a.doctor_id = b.doctor_id
 AND a.id < b.id
 AND a.status = 'SCHEDULED'
 AND b.status = 'SCHEDULED'
 AND a.start_time < b.end_time
 AND b.start_time < a.end_time;
"@
Check 'No overlapping SCHEDULED visits' ($overlaps -ne '' -and [int]$overlaps -eq 0) "overlaps=$overlaps"

# --- 6. Database itself rejects a duplicate slot -----------------------------
$duplicateSql = @"
START TRANSACTION;
INSERT INTO appointments
    (doctor_id, patient_id, service_id, start_time, end_time, status, price, version, created_by, created_at, updated_at)
SELECT doctor_id, patient_id, service_id, start_time, end_time, 'SCHEDULED', price, 0, 1, NOW(6), NOW(6)
FROM appointments
WHERE status = 'SCHEDULED'
ORDER BY id
LIMIT 1;
ROLLBACK;
"@
$env:MYSQL_PWD = $Password
$args = @(
    '--protocol=tcp', "--host=$HostName", "--port=$Port", "--user=$User",
    "--database=$Database",
    '--default-character-set=utf8mb4', '-e', $duplicateSql
)
$duplicateOutput = (& $mysql @args 2>&1 | ForEach-Object { $_.ToString() }) -join ' '
$duplicateCode = $LASTEXITCODE
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
$duplicateRejected = ($duplicateCode -ne 0) -and ($duplicateOutput -match 'Duplicate entry|1062')
Check 'DB rejects a duplicate doctor + slot' $duplicateRejected `
    "exit=$duplicateCode, error=$($duplicateOutput.Trim())"

# --- 7. Application sees the same database ----------------------------------
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/health" -UseBasicParsing -TimeoutSec 5
    Check 'Running app reports database UP' ($health.status -eq 'UP' -and $health.database -eq 'UP') `
        "status=$($health.status), database=$($health.database)"
} catch {
    Check 'Running app reports database UP' $false 'application is not running on 8080'
}

# --- Summary ----------------------------------------------------------------
Write-Host ''
Write-Host ("Passed: {0}  Failed: {1}" -f $script:passed, $script:failed) `
    -ForegroundColor $(if ($script:failed -eq 0) { 'Green' } else { 'Red' })
if ($script:failed -gt 0) { exit 1 } else { exit 0 }

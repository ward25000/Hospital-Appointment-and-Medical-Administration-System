#requires -Version 5.1
<#
    MedFlow - ALL CHECKS IN ONE COMMAND.

    Just double-click verify-all.cmd (or run this file in PowerShell).
    The application must be running - start it first with start.cmd.

    It shows three groups and a short Russian summary at the end:
      1) JUnit / MockMvc tests
      2) REST API, roles, CSRF, booking checks
      3) MySQL structure and data checks
#>
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

# Version banner: makes it obvious in the terminal that the NEW version is running.
Write-Host ""
Write-Host "MedFlow: единая проверка (версия 2, $(Get-Date -Format 'dd.MM.yyyy HH:mm'))" -ForegroundColor DarkCyan
Write-Host "Подробные [PASS]-строки идут ниже, итоговая таблица - в самом конце." -ForegroundColor DarkGray

$ErrorActionPreference = 'Continue'
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ProjectRoot 'medflow-common.ps1')
$ErrorActionPreference = 'Continue'
$mySqlHome = Resolve-MfMySqlHome
$javaHome = Resolve-MfJavaHome
if ($javaHome) { $env:JAVA_HOME = $javaHome }
$maven = Get-MfMaven
Set-Location $ProjectRoot

$results = New-Object System.Collections.ArrayList

function Read-Summary {
    param([string]$Text, [string]$Kind)

    if ($Kind -eq 'tests') {
        # Several per-class lines exist; the LAST one is the grand total.
        $pattern = '(?m)^\[INFO\]\s+Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+)'
    } else {
        $pattern = '(?m)^Passed:\s*(\d+)\s+Failed:\s*(\d+)'
    }

    $matchesFound = [regex]::Matches($Text, $pattern)
    if ($matchesFound.Count -eq 0) {
        return [pscustomobject]@{ Passed = 0; Total = 0 }
    }

    $last = $matchesFound[$matchesFound.Count - 1]
    $first = [int]$last.Groups[1].Value
    $second = [int]$last.Groups[2].Value

    if ($Kind -eq 'tests') {
        # first = total tests, second = failures, group 3 = errors
        $errors = [int]$last.Groups[3].Value
        return [pscustomobject]@{ Passed = ($first - $second - $errors); Total = $first }
    }
    return [pscustomobject]@{ Passed = $first; Total = ($first + $second) }
}

function Run-CheckGroup {
    param(
        [string]$Title,
        [string]$Kind,
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "=== $Title ===" -ForegroundColor Cyan
    $output = (& $Action 2>&1 | Out-String)
    $exitCode = $LASTEXITCODE
    Write-Host $output

    $summary = Read-Summary -Text $output -Kind $Kind
    $passed = [int]$summary.Passed
    $total = [int]$summary.Total

    $ok = ($exitCode -eq 0 -and $passed -gt 0)
    [void]$results.Add([pscustomobject]@{
        Title  = $Title
        Passed = $passed
        Total  = $total
        Ok     = $ok
    })
}

Run-CheckGroup '1/3  Тесты Java (JUnit / MockMvc)' 'tests' {
    & $maven test
}

Run-CheckGroup '2/3  Проверка программы (API, роли, CSRF, запись)' 'checks' {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot 'verify-system.ps1')
}

Run-CheckGroup '3/3  Проверка базы MySQL' 'checks' {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot 'verify-mysql.ps1') -MySQLHome $mySqlHome
}

# --- Short, easy-to-show summary ---------------------------------------------
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  MedFlow - ИТОГ ПРОВЕРОК" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
$allOk = $true
foreach ($r in $results) {
    $mark = if ($r.Ok) { '[OK]' } else { '[FAIL]' }
    $color = if ($r.Ok) { 'Green' } else { 'Red' }
    $line = "  {0,-48} {1,3}/{2,-3} {3}" -f $r.Title, $r.Passed, $r.Total, $mark
    Write-Host $line -ForegroundColor $color
    if (-not $r.Ok) { $allOk = $false }
}
Write-Host "------------------------------------------------------------" -ForegroundColor Green
if ($allOk) {
    Write-Host "  ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ" -ForegroundColor Green
} else {
    Write-Host "  ЕСТЬ ОШИБКИ - сначала запусти start.cmd, потом повтори" -ForegroundColor Red
}
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""

if ($allOk) { exit 0 } else { exit 1 }

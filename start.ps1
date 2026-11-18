#requires -Version 5.1
<#
    MedFlow - launcher.

    Does three things:
      1. If MedFlow is already running on port 8080 - just opens the browser.
      2. Starts the database via "docker compose up -d" (real Docker or shim).
      3. Starts the Spring Boot app from the ASCII junction C:\MedFlow.

    The window is kept open by start.cmd, so an error is always readable.
#>

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

# 'Continue' is intentional: Maven and Java write warnings to stderr, and with
# 'Stop' PowerShell 5.1 would abort the launcher on the first such warning.
$ErrorActionPreference = 'Continue'

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsRoot = "$env:USERPROFILE\.medflow-tools"
$configPath = Join-Path $ToolsRoot 'medflow-setup.json'

$junction = "$env:SystemDrive\MedFlow"
$mySqlHome = "$env:USERPROFILE\mysql84"
if (Test-Path $configPath) {
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    if ($config.Junction) { $junction = $config.Junction }
    if ($config.MySqlHome) { $mySqlHome = $config.MySqlHome }
    if ($config.JavaHome -and (Test-Path (Join-Path $config.JavaHome 'bin\java.exe'))) {
        $env:JAVA_HOME = $config.JavaHome
    }
}
$userJavaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
if ($userJavaHome -and (Test-Path (Join-Path $userJavaHome 'bin\java.exe'))) {
    $env:JAVA_HOME = $userJavaHome
}

$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' +
            [Environment]::GetEnvironmentVariable('Path', 'User')

function Test-MedFlowUp {
    try {
        $health = Invoke-RestMethod 'http://localhost:8080/api/health' -TimeoutSec 3 -UseBasicParsing
        return ($health.status -eq 'UP')
    } catch {
        return $false
    }
}

try {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  MedFlow" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green

    # --- 1. Already running? -------------------------------------------------
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        if (Test-MedFlowUp) {
            Write-Host "MedFlow уже запущена." -ForegroundColor Green
            Write-Host "Открываю http://localhost:8080" -ForegroundColor Green
            Start-Process 'http://localhost:8080'
            Write-Host ""
            Write-Host "Если нужен перезапуск: закрой это окно, запусти stop.cmd, затем снова start.cmd." -ForegroundColor Yellow
            exit 0
        }
        Write-Host "Порт 8080 занят, но MedFlow не отвечает." -ForegroundColor Red
        Write-Host "Запусти stop.cmd, чтобы освободить порт, и попробуй снова." -ForegroundColor Yellow
        exit 1
    }

    # --- 2. Database ---------------------------------------------------------
    Write-Host "[1/2] Поднимаю базу данных (docker compose up -d)..." -ForegroundColor Cyan
    $dockerCode = 1
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        Push-Location $ProjectRoot
        & docker compose up -d
        $dockerCode = $LASTEXITCODE
        Pop-Location
    }
    if ($dockerCode -ne 0) {
        $shim = Join-Path $ToolsRoot 'bin\docker.cmd'
        if (Test-Path $shim) {
            Push-Location $ProjectRoot
            & $shim compose up -d
            $dockerCode = $LASTEXITCODE
            Pop-Location
        }
    }
    if ($dockerCode -ne 0) {
        throw 'Не удалось поднять базу. Запусти setup.cmd один раз и повтори.'
    }

    # --- 3. Application ------------------------------------------------------
    if (-not (Test-Path $junction)) {
        throw "Не найдена папка $junction. Запусти setup.cmd один раз."
    }

    Write-Host "[2/2] Запускаю программу из $junction ..." -ForegroundColor Cyan
    Write-Host "Жди строку: Tomcat started on port 8080" -ForegroundColor DarkGray
    Write-Host "Потом открой: http://localhost:8080" -ForegroundColor DarkGray
    Write-Host ""

    # Open the browser automatically once the app answers.
    Start-Job -ScriptBlock {
        for ($i = 0; $i -lt 180; $i++) {
            try {
                $h = Invoke-RestMethod 'http://localhost:8080/api/health' -TimeoutSec 2 -UseBasicParsing
                if ($h.status -eq 'UP') {
                    Start-Process 'http://localhost:8080'
                    break
                }
            } catch { }
            Start-Sleep -Seconds 1
        }
    } | Out-Null

    Set-Location $junction
    & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=Europe/Moscow"
    $appCode = $LASTEXITCODE

    Write-Host ""
    Write-Host "MedFlow остановлена (код $appCode)." -ForegroundColor Yellow
    exit $appCode
} catch {
    Write-Host ""
    Write-Host "ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Что делать:" -ForegroundColor Yellow
    Write-Host "  1. Если это первый запуск - выполни setup.cmd один раз." -ForegroundColor Yellow
    Write-Host "  2. Если не помогает - запусти verify-all.cmd и пришли вывод." -ForegroundColor Yellow
    exit 1
}

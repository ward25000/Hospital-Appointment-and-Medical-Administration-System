#requires -Version 5.1
<#
    MedFlow - запуск: база, затем приложение. Двойной клик по start.cmd.
    Отдельная установка (setup.cmd) не нужна, если MySQL и Java уже есть.
#>
param(
    [string]$MySqlHome,
    [int]$Port = 3306,
    [switch]$NoBrowser
)
. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'medflow-common.ps1')
$ErrorActionPreference = 'Continue'

function Test-MedFlowUp {
    try {
        return ((Invoke-RestMethod 'http://localhost:8080/api/health' -TimeoutSec 3 -UseBasicParsing).status -eq 'UP')
    } catch { return $false }
}

try {
    Write-Host ''
    Write-Host '=== MedFlow: запуск ===' -ForegroundColor Green

    if (Test-MfPort 8080) {
        if (Test-MedFlowUp) {
            Write-Host 'MedFlow уже запущена: http://localhost:8080' -ForegroundColor Green
            Write-Host 'Чтобы перезапустить: stop.cmd, затем start.cmd.' -ForegroundColor Yellow
            if (-not $NoBrowser) { Start-Process 'http://localhost:8080' }
            exit 0
        }
        $owner = Get-MfPortProcess 8080
        throw "Порт 8080 занят ($($owner.Name), PID $($owner.ProcessId)), но MedFlow не отвечает. Запусти stop.cmd и повтори."
    }

    Write-Host '[1/3] База данных' -ForegroundColor Cyan
    Start-MfMySql (Resolve-MfMySqlHome $MySqlHome) $Port

    Write-Host '[2/3] Java' -ForegroundColor Cyan
    $javaHome = Resolve-MfJavaHome
    if (-not $javaHome) { throw 'Не найдена Java 17 или новее. Запусти setup.cmd.' }
    $env:JAVA_HOME = $javaHome
    Write-Host "  $javaHome" -ForegroundColor Green

    Write-Host '[3/3] Приложение: первый запуск занимает 1-2 минуты' -ForegroundColor Cyan
    Write-Host '  Адрес: http://localhost:8080   Остановить: Ctrl+C или stop.cmd' -ForegroundColor DarkGray
    Write-Host ''
    if (-not $NoBrowser) {
        Start-Job -ScriptBlock {
            for ($i = 0; $i -lt 300; $i++) {
                try {
                    if ((Invoke-RestMethod 'http://localhost:8080/api/health' -TimeoutSec 2 -UseBasicParsing).status -eq 'UP') {
                        Start-Process 'http://localhost:8080'
                        break
                    }
                } catch {}
                Start-Sleep -Seconds 1
            }
        } | Out-Null
    }

    if ($Port -ne 3306) {
        $env:DB_URL = "jdbc:mysql://localhost:$Port/hospital_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Almaty&characterEncoding=UTF-8"
    }
    Set-Location -LiteralPath (Get-MfRunDirectory)
    & (Get-MfMaven) spring-boot:run '-Dspring-boot.run.jvmArguments=-Duser.timezone=Asia/Almaty'
    $code = $LASTEXITCODE
    Write-Host ''
    Write-Host "MedFlow остановлена (код $code)." -ForegroundColor Yellow
    if ($code -ne 0) {
        Write-Host 'Если выше есть ошибка Flyway или MySQL, поможет full-reset.cmd.' -ForegroundColor Yellow
    }
    exit $code
} catch {
    Write-Host ''
    Write-Host "ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ''
    Write-Host 'При проблеме с базой запусти full-reset.cmd. Для установки Java нужен setup.cmd.' -ForegroundColor Yellow
    exit 1
}

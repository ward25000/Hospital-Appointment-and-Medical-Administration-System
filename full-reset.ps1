#requires -Version 5.1
<#
    MedFlow - ПОЛНЫЙ СБРОС, когда не помогает ничего другое.

      1. останавливает приложение и MySQL;
      2. делает копию базы (если MySQL отвечает) и откладывает папку данных
         в сторону: ничего не удаляется;
      3. создаёт чистую MySQL с базой hospital_db и пользователем hospital;
      4. убирает собранные файлы (target) и старые настройки установки;
      5. запускает MedFlow.

    Параметры: -Yes (без вопроса), -NoStart (не запускать в конце).
#>
param(
    [string]$MySqlHome,
    [int]$Port = 3306,
    [switch]$Yes,
    [switch]$NoStart
)
. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'medflow-common.ps1')
$ErrorActionPreference = 'Continue'

try {
    Write-Host ''
    Write-Host '============================================================' -ForegroundColor Yellow
    Write-Host '  MedFlow: ПОЛНЫЙ СБРОС' -ForegroundColor Yellow
    Write-Host '============================================================' -ForegroundColor Yellow
    $mysqlFolder = Resolve-MfMySqlHome $MySqlHome
    if (-not (Test-Path -LiteralPath (Join-Path $mysqlFolder 'bin\mysqld.exe'))) {
        throw "MySQL не найдена в $mysqlFolder. Запусти setup.cmd: он её установит."
    }
    Write-Host "MySQL: $mysqlFolder"
    Write-Host 'База станет новой, с демо-данными. Старая останется рядом, в папке data-backup-...'
    if (-not $Yes) {
        if ((Read-Host 'Для подтверждения введи RESET') -ne 'RESET') { Write-Host 'Отменено.'; exit 0 }
    }

    Write-Host '[1/4] Остановка' -ForegroundColor Cyan
    if ($Port -eq 3306) { Stop-MfApp 8080 }
    $owner = Get-MfPortProcess $Port
    if ($owner -and -not (Test-MfOwnMySql $owner $mysqlFolder)) {
        throw ("Порт $Port занят другой программой: $($owner.Name) ($($owner.ExecutablePath)).`n" +
            'Закрой её (например, другую MySQL или Docker) и запусти сброс снова.')
    }

    Write-Host '[2/4] Сохранение старых данных' -ForegroundColor Cyan
    if ($owner) {
        $copy = $null
        try { $copy = Backup-MfDatabase $mysqlFolder $Port } catch { Write-Host "  SQL-копию сделать не удалось, папка данных всё равно сохранится" -ForegroundColor Yellow }
        if ($copy) { Write-Host "  копия: $copy" -ForegroundColor Green }
    }
    Stop-MfMySql $mysqlFolder
    $data = Join-Path $mysqlFolder 'data'
    if (Test-Path -LiteralPath $data) {
        $saved = Join-Path $mysqlFolder ('data-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        for ($i = 0; $i -lt 10; $i++) {
            try { Move-Item -LiteralPath $data -Destination $saved -ErrorAction Stop; break } catch { Start-Sleep -Seconds 1 }
        }
        if (Test-Path -LiteralPath $data) { throw "Папка $data занята другой программой. Перезагрузи компьютер и повтори." }
        Write-Host "  старые данные: $saved" -ForegroundColor Green
    }
    Remove-Item -LiteralPath (Join-Path $mysqlFolder 'configured.flag') -Force -ErrorAction SilentlyContinue

    Write-Host '[3/4] Чистая MySQL' -ForegroundColor Cyan
    Start-MfMySql $mysqlFolder $Port

    Write-Host '[4/4] Очистка сборки и настроек' -ForegroundColor Cyan
    $target = Join-Path $script:MfRoot 'target'
    if (Test-Path -LiteralPath $target) {
        Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction SilentlyContinue
        if (Test-Path -LiteralPath $target) { Write-Host '  часть target занята (VS Code), это не мешает' -ForegroundColor DarkGray }
    }
    if (-not $MySqlHome) {
        Remove-Item -LiteralPath (Join-Path $script:MfTools 'medflow-setup.json') -Force -ErrorAction SilentlyContinue
    }
    Write-Host ''
    Write-Host 'Полный сброс выполнен.' -ForegroundColor Green
    if ($NoStart) { exit 0 }
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $script:MfRoot 'start.ps1') -MySqlHome $mysqlFolder -Port $Port
    exit $LASTEXITCODE
} catch {
    Write-Host ''
    Write-Host "ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

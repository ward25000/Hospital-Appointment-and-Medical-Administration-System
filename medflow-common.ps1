#requires -Version 5.1
<#
    MedFlow - общие функции для start / stop / reset / full-reset / db / setup.
    Сам по себе ничего не запускает: подключается через точку.

    Правило: приложение и скрипты работают под пользователем hospital.
    Пароль root нужен, только если этого пользователя ещё нет.
#>

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
# PowerShell 5.1 по умолчанию передаёт в программы ASCII: русский текст в SQL терялся бы.
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$script:MfRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:MfKit = Split-Path -Parent $script:MfRoot
$script:MfTools = "$env:USERPROFILE\.medflow-tools"
$script:MfDatabase = 'hospital_db'
$script:MfUser = 'hospital'
$script:MfPassword = 'hospital_pass'

function Get-MfConfig {
    $path = Join-Path $script:MfTools 'medflow-setup.json'
    if (Test-Path -LiteralPath $path) {
        try { return (Get-Content -LiteralPath $path -Raw | ConvertFrom-Json) } catch {}
    }
    return $null
}

# Портативная MySQL: настройка из setup, затем профиль пользователя, затем комплект.
function Resolve-MfMySqlHome {
    param([string]$Preferred)
    if ($Preferred) { return $Preferred }
    $config = Get-MfConfig
    $candidates = @()
    if ($config -and $config.MySqlHome) { $candidates += $config.MySqlHome }
    $candidates += "$env:USERPROFILE\mysql84"
    $candidates += (Join-Path $script:MfKit 'runtime\mysql84')
    foreach ($item in $candidates) {
        if (Test-Path -LiteralPath (Join-Path $item 'bin\mysqld.exe')) { return $item }
    }
    return "$env:USERPROFILE\mysql84"
}

function Get-MfJavaMajor {
    param([string]$JavaHome)
    $release = Join-Path $JavaHome 'release'
    if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) { return 0 }
    if (-not (Test-Path -LiteralPath $release)) { return 0 }
    $text = Get-Content -LiteralPath $release -Raw
    if ($text -match 'JAVA_VERSION="(\d+)') { return [int]$Matches[1] }
    return 0
}

function Resolve-MfJavaHome {
    $config = Get-MfConfig
    $candidates = @((Join-Path $script:MfKit 'runtime\jdk-21'))
    if ($config -and $config.JavaHome) { $candidates += $config.JavaHome }
    $portableJdkRoot = Join-Path $script:MfTools 'jdk'
    if (Test-Path -LiteralPath $portableJdkRoot) {
        $candidates += @(Get-ChildItem -LiteralPath $portableJdkRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } |
            Sort-Object Name -Descending |
            ForEach-Object { $_.FullName })
    }
    $candidates += [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
    $candidates += [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    $candidates += $env:JAVA_HOME
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($javaCommand -and $javaCommand.Source) {
        $candidates += Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
    }
    foreach ($item in $candidates) {
        if ($item -and (Get-MfJavaMajor $item) -ge 17) { return $item }
    }
    return $null
}

# Запуск mysql.exe. Предупреждения в stderr не должны обрывать скрипт,
# поэтому ErrorActionPreference здесь всегда Continue.
function Invoke-MfSql {
    param(
        [string]$MySqlHome, [int]$Port, [string]$User, [string]$Password,
        [string]$Sql, [string]$Database
    )
    $ErrorActionPreference = 'Continue'
    $mysqlArgs = @('--protocol=tcp', '--host=127.0.0.1', "--port=$Port", "--user=$User",
        '--connect-timeout=5', '--default-character-set=utf8mb4', '-N', '-B')
    if ($Database) { $mysqlArgs += "--database=$Database" }
    # Не --execute: PowerShell 5.1 теряет кавычки внутри аргумента.
    $env:MYSQL_PWD = $Password
    try {
        $output = $Sql | & (Join-Path $MySqlHome 'bin\mysql.exe') @mysqlArgs 2>&1 | ForEach-Object { "$_" } | Out-String
        return [pscustomobject]@{ Ok = ($LASTEXITCODE -eq 0); Output = $output.Trim() }
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Test-MfPort {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Get-MfPortProcess {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $connection) { return $null }
    return Get-CimInstance Win32_Process -Filter "ProcessId=$($connection.OwningProcess)" -ErrorAction SilentlyContinue
}

function Test-MfOwnMySql {
    param($Process, [string]$MySqlHome)
    if (-not $Process -or -not $Process.ExecutablePath) { return $false }
    $expected = [IO.Path]::GetFullPath((Join-Path $MySqlHome 'bin\mysqld.exe'))
    return ([IO.Path]::GetFullPath($Process.ExecutablePath) -eq $expected)
}

function Test-MfAppUser {
    param([string]$MySqlHome, [int]$Port)
    return (Invoke-MfSql $MySqlHome $Port $script:MfUser $script:MfPassword 'SELECT 1;' $script:MfDatabase).Ok
}

# Создаёт hospital_db и пользователя hospital. Подходит и к root без пароля,
# и к root_pass, который ставит комплект START-DEMO и docker-compose.
function Initialize-MfAppUser {
    param([string]$MySqlHome, [int]$Port)
    if (Test-MfAppUser $MySqlHome $Port) { return }

    # Пользователь есть, а базы нет (сброс прервали на середине).
    $null = Invoke-MfSql $MySqlHome $Port $script:MfUser $script:MfPassword `
        "CREATE DATABASE IF NOT EXISTS $script:MfDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if (Test-MfAppUser $MySqlHome $Port) { return }

    $passwords = @('', 'root_pass')
    if ($env:MEDFLOW_MYSQL_ROOT_PASSWORD) { $passwords = @($env:MEDFLOW_MYSQL_ROOT_PASSWORD) + $passwords }
    $sql = @"
CREATE DATABASE IF NOT EXISTS $script:MfDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$script:MfUser'@'%' IDENTIFIED BY '$script:MfPassword';
CREATE USER IF NOT EXISTS '$script:MfUser'@'localhost' IDENTIFIED BY '$script:MfPassword';
ALTER USER '$script:MfUser'@'%' IDENTIFIED BY '$script:MfPassword';
ALTER USER '$script:MfUser'@'localhost' IDENTIFIED BY '$script:MfPassword';
GRANT ALL PRIVILEGES ON $script:MfDatabase.* TO '$script:MfUser'@'%';
GRANT ALL PRIVILEGES ON $script:MfDatabase.* TO '$script:MfUser'@'localhost';
FLUSH PRIVILEGES;
"@
    $last = ''
    foreach ($password in $passwords) {
        $result = Invoke-MfSql $MySqlHome $Port 'root' $password $sql
        if ($result.Ok -and (Test-MfAppUser $MySqlHome $Port)) { return }
        $last = $result.Output
    }
    throw ("MySQL работает, но войти в неё нельзя: пароль root другой, а пользователя hospital нет.`n" +
        "Ответ MySQL: $last`n" +
        "Решение: запусти full-reset.cmd (создаст чистую базу, старые данные сохранит в папке рядом).")
}

# Запускает портативную MySQL и готовит базу. Повторный вызов безопасен.
function Start-MfMySql {
    param([string]$MySqlHome, [int]$Port = 3306)
    $mysqld = Join-Path $MySqlHome 'bin\mysqld.exe'
    if (-not (Test-Path -LiteralPath $mysqld)) {
        throw "MySQL не найдена: $mysqld. Запусти setup.cmd."
    }

    if (Test-MfPort $Port) {
        if (Test-MfAppUser $MySqlHome $Port) {
            Write-Host "  база уже работает на порту $Port" -ForegroundColor Green
            return
        }
        $owner = Get-MfPortProcess $Port
        if ($owner -and $owner.Name -notmatch 'mysqld|docker|wslrelay') {
            throw "Порт $Port занят программой $($owner.Name) ($($owner.ExecutablePath)). Закрой её и повтори."
        }
        Initialize-MfAppUser $MySqlHome $Port
        Write-Host "  база настроена на порту $Port" -ForegroundColor Green
        return
    }

    if ($MySqlHome -match '[^ -]') {
        throw "MySQL не работает из папки с русскими буквами: $MySqlHome. Перенеси её, например, в C:\mysql84."
    }
    $data = Join-Path $MySqlHome 'data'
    $log = Join-Path $MySqlHome 'mysql-error.log'
    if (-not (Test-Path -LiteralPath (Join-Path $data 'mysql'))) {
        if (Test-Path -LiteralPath $data) {
            # Недосозданная папка: не удаляем, а откладываем в сторону.
            $broken = "$data-broken-" + (Get-Date -Format 'yyyyMMdd-HHmmss')
            Move-Item -LiteralPath $data -Destination $broken
        }
        Write-Host '  создаю папку данных MySQL (один раз)...'
        New-Item -ItemType Directory -Path $data -Force | Out-Null
        $ErrorActionPreference = 'Continue'
        $output = & $mysqld --no-defaults --initialize-insecure "--basedir=$MySqlHome" "--datadir=$data" --console 2>&1 | Out-String
        if ($LASTEXITCODE -ne 0) { throw "MySQL не смогла создать папку данных:`n$output" }
    }

    Write-Host "  запускаю MySQL на порту $Port ..."
    $serverArgs = @('--no-defaults', "--basedir=`"$MySqlHome`"", "--datadir=`"$data`"", "--port=$Port",
        '--bind-address=127.0.0.1', '--mysqlx=0', '--character-set-server=utf8mb4',
        '--collation-server=utf8mb4_unicode_ci', "--log-error=`"$log`"")
    $process = Start-Process -FilePath $mysqld -ArgumentList $serverArgs -PassThru -WindowStyle Hidden
    for ($i = 0; $i -lt 120; $i++) {
        if (Test-MfPort $Port) { break }
        if ($process.HasExited) { break }
        Start-Sleep -Milliseconds 500
    }
    if (-not (Test-MfPort $Port)) {
        $tail = if (Test-Path -LiteralPath $log) { (Get-Content -LiteralPath $log -Tail 12) -join "`n" } else { '' }
        throw "MySQL не запустилась. Последние строки журнала:`n$tail"
    }
    Initialize-MfAppUser $MySqlHome $Port
    Write-Host "  база готова: $script:MfDatabase, порт $Port" -ForegroundColor Green
}

# Останавливает только MySQL из указанной папки.
function Stop-MfMySql {
    param([string]$MySqlHome)
    $own = @(Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" -ErrorAction SilentlyContinue |
        Where-Object { Test-MfOwnMySql $_ $MySqlHome })
    if ($own.Count -eq 0) {
        Write-Host '  MySQL не запущена' -ForegroundColor DarkGray
        return
    }
    $ErrorActionPreference = 'Continue'
    $ports = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $own.ProcessId -contains $_.OwningProcess } | Select-Object -ExpandProperty LocalPort -Unique)
    foreach ($port in $ports) {
        foreach ($password in @('root_pass', '', $env:MEDFLOW_MYSQL_ROOT_PASSWORD)) {
            if ((Invoke-MfSql $MySqlHome $port 'root' $password 'SHUTDOWN;').Ok) { break }
        }
    }
    for ($i = 0; $i -lt 20; $i++) {
        $left = @($own | Where-Object { Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue })
        if ($left.Count -eq 0) { break }
        Start-Sleep -Milliseconds 500
    }
    # InnoDB восстановится сама, если вежливо остановить не удалось.
    foreach ($item in $own) { Stop-Process -Id $item.ProcessId -Force -ErrorAction SilentlyContinue }
    Remove-Item -LiteralPath (Join-Path $MySqlHome 'mysqld.pid') -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 500
    Write-Host '  MySQL остановлена' -ForegroundColor Green
}

# Останавливает приложение MedFlow, чужие программы на порту не трогает.
function Stop-MfApp {
    param([int]$Port = 8080)
    $process = Get-MfPortProcess $Port
    if (-not $process) {
        Write-Host "  приложение не запущено ($Port свободен)" -ForegroundColor DarkGray
        return
    }
    if ($process.Name -notmatch '^javaw?\.exe$' -or
        $process.CommandLine -notmatch 'HospitalApplication|hospital-appointment-system|spring-boot') {
        throw "Порт $Port занят другой программой: $($process.Name) (PID $($process.ProcessId)). Закрой её и повтори."
    }
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    for ($i = 0; $i -lt 20 -and (Test-MfPort $Port); $i++) { Start-Sleep -Milliseconds 300 }
    Write-Host "  приложение остановлено (PID $($process.ProcessId))" -ForegroundColor Green
}

# Копия базы в папку backups проекта. Возвращает путь или $null, если копировать нечего.
function Backup-MfDatabase {
    param([string]$MySqlHome, [int]$Port)
    $tables = Invoke-MfSql $MySqlHome $Port $script:MfUser $script:MfPassword `
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$script:MfDatabase';"
    if (-not $tables.Ok -or $tables.Output -eq '0') { return $null }
    $folder = Join-Path $script:MfRoot 'backups'
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    $file = Join-Path $folder ("$script:MfDatabase-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
    $ErrorActionPreference = 'Continue'
    $env:MYSQL_PWD = $script:MfPassword
    try {
        $output = & (Join-Path $MySqlHome 'bin\mysqldump.exe') --protocol=tcp --host=127.0.0.1 "--port=$Port" `
            "--user=$script:MfUser" --single-transaction --no-tablespaces --default-character-set=utf8mb4 `
            "--result-file=$file" $script:MfDatabase 2>&1 | Out-String
        if ($LASTEXITCODE -ne 0) { throw "Не удалось сделать копию базы, сброс отменён:`n$output" }
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
    return $file
}

# spring-boot:run не понимает кириллицу в пути: тогда нужна ссылка с латинским именем.
function Get-MfRunDirectory {
    if ($script:MfRoot -notmatch '[^\x00-\x7F]') { return $script:MfRoot }
    for ($i = 1; $i -le 9; $i++) {
        $link = "$env:SystemDrive\MedFlow" + $(if ($i -eq 1) { '' } else { $i })
        if (Test-Path -LiteralPath $link) {
            $item = Get-Item -LiteralPath $link -Force
            if ($item.LinkType -ne 'Junction') { continue }
            if (@($item.Target) -contains $script:MfRoot) { return $link }
            & cmd /c rmdir "$link" | Out-Null
        }
        & cmd /c mklink /J "$link" "$script:MfRoot" | Out-Null
        if ($LASTEXITCODE -eq 0) { return $link }
    }
    throw 'Не удалось создать ссылку C:\MedFlow. Перенеси проект в папку без русских букв в пути.'
}

function Get-MfMaven {
    $kit = Join-Path $script:MfKit 'tools\apache-maven-3.9.9\bin\mvn.cmd'
    if (Test-Path -LiteralPath $kit) { return $kit }
    return (Join-Path $script:MfRoot 'mvnw.cmd')
}

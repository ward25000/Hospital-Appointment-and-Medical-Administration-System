#requires -Version 5.1
<#
    MedFlow - сброс пароля любого пользователя.

    Пароли в базе хранятся как BCrypt-хеши, поэтому "узнать" старый пароль нельзя.
    Зато можно поставить новый - этот скрипт делает это одной командой.

    Примеры:
        .\set-password.cmd doctor.petrova NewPass123!
        .\set-password.cmd Arshagul MySecret1!
        powershell -ExecutionPolicy Bypass -File .\set-password.ps1 -Username admin -Password Admin123!
        powershell -ExecutionPolicy Bypass -File .\set-password.ps1 -Username admin -Password Admin123! -VerifyOnly
        .\set-password.cmd -List            (показать всех пользователей)
#>
param(
    [string]$Username,
    [string]$Password,
    [switch]$VerifyOnly,
    [switch]$List,
    [string]$MySqlHome,
    [string]$Database = 'hospital_db'
)

. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'medflow-common.ps1')
# Continue: mysql.exe и java пишут предупреждения в stderr, они не должны обрывать скрипт.
$ErrorActionPreference = 'Continue'
trap { Write-Host "ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }

$MySqlHome = Resolve-MfMySqlHome $MySqlHome
$mysql = Join-Path $MySqlHome 'bin\mysql.exe'
if (-not (Test-Path -LiteralPath $mysql) -or -not (Test-MfAppUser $MySqlHome 3306)) {
    Write-Host 'База не отвечает. Сначала запусти start.cmd (если не поможет, full-reset.cmd).' -ForegroundColor Red
    exit 1
}
$env:MYSQL_PWD = $script:MfPassword

$sqlArgs = @(
    '--protocol=tcp',
    '--host=127.0.0.1',
    '--port=3306',
    "--user=$script:MfUser",
    '--default-character-set=utf8mb4',
    "--database=$Database"
)

function Invoke-Sql {
    param([string]$Sql)
    return ($Sql | & $mysql @sqlArgs 2>&1)
}

# ---------------------------------------------------------------- показать всех
if ($List) {
    Write-Host ""
    Write-Host "Пользователи в базе ${Database}:" -ForegroundColor Cyan
    Invoke-Sql "SELECT id, username, role_name, full_name, enabled FROM users ORDER BY id;"
    Write-Host ""
    Write-Host "Демо-пароли: admin/Admin123!  reception/Reception123!  doctor.*/Doctor123!  patient.demo/Patient123!" -ForegroundColor DarkGray
    exit 0
}

# ---------------------------------------------------------------- поиск Java
function Find-JavaHome {
    $candidates = @()
    $found = Resolve-MfJavaHome
    if ($found) { $candidates += $found }
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $cfg = Join-Path "$env:USERPROFILE\.medflow-tools" 'medflow-setup.json'
    if (Test-Path $cfg) {
        $json = Get-Content $cfg -Raw | ConvertFrom-Json
        if ($json.JavaHome) { $candidates += $json.JavaHome }
    }
    $userJava = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
    if ($userJava) { $candidates += $userJava }
    foreach ($c in $candidates) {
        if ($c -and (Test-Path (Join-Path $c 'bin\jshell.exe'))) { return $c }
    }
    $found = Get-ChildItem "$env:LOCALAPPDATA\Programs\jdk-17" -Directory -ErrorAction SilentlyContinue |
             Sort-Object Name -Descending | Select-Object -First 1
    if ($found -and (Test-Path (Join-Path $found.FullName 'bin\jshell.exe'))) { return $found.FullName }
    return $null
}

function Get-CryptoJars {
    $repo = Join-Path $env:USERPROFILE '.m2\repository'
    $crypto = Get-ChildItem (Join-Path $repo 'org\springframework\security\spring-security-crypto') -Recurse -Filter 'spring-security-crypto-*.jar' -ErrorAction SilentlyContinue |
              Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object Name -Descending | Select-Object -First 1
    $jcl = Get-ChildItem (Join-Path $repo 'org\springframework\spring-jcl') -Recurse -Filter 'spring-jcl-*.jar' -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $crypto -or -not $jcl) { return $null }
    return "$($crypto.FullName);$($jcl.FullName)"
}

function Invoke-JShell {
    param([string]$Code)
    $javaHome = Find-JavaHome
    if (-not $javaHome) { throw 'Не найдена Java. Запусти setup.cmd.' }
    $jars = Get-CryptoJars
    if (-not $jars) { throw 'Не найден spring-security-crypto в .m2. Запусти .\mvnw.cmd -q -DskipTests package или start.cmd один раз.' }

    $script = Join-Path $env:TEMP ("medflow-hash-{0}.jsh" -f ([guid]::NewGuid().ToString('N')))
    try {
        Set-Content -Path $script -Value $Code -Encoding ASCII
        $out = & (Join-Path $javaHome 'bin\jshell.exe') --class-path $jars -q $script 2>&1
    } finally {
        Remove-Item $script -Force -ErrorAction SilentlyContinue
    }
    return $out
}

function Get-BCryptHash {
    param([string]$Plain)
    # Пароль передаётся в base64 - так кавычки и спецсимволы ничего не ломают.
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Plain))
    $code = 'var p = new String(java.util.Base64.getDecoder().decode("' + $b64 + '"), java.nio.charset.StandardCharsets.UTF_8); ' +
            'System.out.println("HASH=" + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(p));'
    $out = Invoke-JShell $code
    $line = $out | Where-Object { $_ -like 'HASH=*' } | Select-Object -First 1
    if (-not $line) { throw "Не удалось создать хеш. Вывод Java: $($out -join ' | ')" }
    return $line.Substring(5).Trim()
}

function Test-BCryptHash {
    param([string]$Plain, [string]$Hash)
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Plain))
    # Хеш безопасен для Java-строки, но экранируем на всякий случай.
    $safeHash = $Hash.Replace('\', '\\').Replace('"', '\"')
    $code = 'var p = new String(java.util.Base64.getDecoder().decode("' + $b64 + '"), java.nio.charset.StandardCharsets.UTF_8); ' +
            'System.out.println("MATCH=" + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(p, "' + $safeHash + '"));'
    $out = Invoke-JShell $code
    $line = $out | Where-Object { $_ -like 'MATCH=*' } | Select-Object -First 1
    return ($line -eq 'MATCH=true')
}

# ---------------------------------------------------------------- ввод данных
if (-not $Username) {
    Write-Host ""
    Write-Host "Пользователи в базе:" -ForegroundColor Cyan
    Invoke-Sql "SELECT id, username, role_name FROM users ORDER BY id;"
    Write-Host ""
    $Username = (Read-Host 'Логин пользователя, которому меняем пароль').Trim()
}
if (-not $Username) { Write-Host 'Логин не указан.' -ForegroundColor Red; exit 1 }

$safeUser = $Username.Replace("'", "''")
$existing = Invoke-Sql "SELECT username, role_name, password FROM users WHERE username = '$safeUser';"
if ($existing -match 'Empty set' -or -not ($existing | Where-Object { $_ -match [regex]::Escape($Username) })) {
    Write-Host "Пользователь '$Username' не найден." -ForegroundColor Red
    Write-Host "Вот кто есть:" -ForegroundColor Yellow
    Invoke-Sql "SELECT username, role_name FROM users ORDER BY id;"
    exit 1
}

$storedHash = $null
foreach ($row in $existing) {
    if ($row -match '^\S+\s+\S+\s+(\$2[aby]\$\S+)$') { $storedHash = $Matches[1] }
}

# ---------------------------------------------------------------- режим проверки
if ($VerifyOnly) {
    if (-not $Password) {
        $sec = Read-Host 'Пароль для проверки' -AsSecureString
        $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
    }
    if (-not $storedHash) { Write-Host 'Не удалось прочитать хеш из базы.' -ForegroundColor Red; exit 1 }
    $ok = Test-BCryptHash -Plain $Password -Hash $storedHash
    if ($ok) {
        Write-Host "ПОДХОДИТ: пароль '$Password' верный для '$Username'." -ForegroundColor Green
        exit 0
    }
    Write-Host "НЕ ПОДХОДИТ: пароль '$Password' не подходит для '$Username'." -ForegroundColor Red
    Write-Host "Сбросить: .\set-password.cmd $Username НовыйПароль" -ForegroundColor Yellow
    exit 1
}

if (-not $Password) {
    $sec = Read-Host "Новый пароль для '$Username'" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}
if (-not $Password) { Write-Host 'Пароль не указан.' -ForegroundColor Red; exit 1 }
if ($Password.Length -lt 6) {
    Write-Host 'Пароль короче 6 символов - приложение такой не примет (минимум 6).' -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------- сброс
Write-Host ""
Write-Host "Готовлю BCrypt-хеш..." -ForegroundColor DarkGray
$hash = Get-BCryptHash -Plain $Password

$update = "UPDATE users SET password = '$hash', updated_at = NOW(6) WHERE username = '$safeUser'; SELECT username, role_name, LEFT(password, 12) AS hash_prefix FROM users WHERE username = '$safeUser';"
$result = Invoke-Sql $update

Write-Host ""
Write-Host "Готово: пароль для '$Username' изменён." -ForegroundColor Green
Write-Host "  Новый пароль: $Password" -ForegroundColor Cyan
Write-Host "  Войти: http://localhost:8080" -ForegroundColor DarkGray
Write-Host ""
$result | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
Write-Host ""
Write-Host "Проверить: .\set-password.cmd $Username `"$Password`" -VerifyOnly" -ForegroundColor DarkGray

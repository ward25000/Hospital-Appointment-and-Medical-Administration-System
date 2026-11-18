#requires -Version 5.1
<#
    MedFlow - automatic setup for a new Windows device.

    Installs (without administrator rights):
      - portable Temurin JDK 17 (if Java 17+ is missing)
      - portable Git (if Git is missing)
      - portable MySQL 8.4.11 with hospital_db / hospital
      - a small "docker" compatibility shim if real Docker Desktop is unavailable
      - an ASCII junction (C:\MedFlow) so spring-boot:run works with Cyrillic paths

    Run:  powershell -ExecutionPolicy Bypass -File .\setup.ps1
    Or just double-click setup.cmd
#>
param(
    [string]$ToolsRoot = "$env:USERPROFILE\.medflow-tools",
    [string]$MySqlHome = "$env:USERPROFILE\mysql84",
    [string]$PreferredJunction = "$env:SystemDrive\MedFlow"
)

# 'Continue' is intentional: java -version and Maven write some output to stderr,
# and with 'Stop' PowerShell 5.1 treats that as a terminating error.
$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

# Refresh environment: the terminal is usually older than the variables we set.
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' +
            [Environment]::GetEnvironmentVariable('Path', 'User')
$userJavaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
if ($userJavaHome) { $env:JAVA_HOME = $userJavaHome }

function Write-Step {
    param([string]$Message)
    Write-Host "[setup] $Message" -ForegroundColor Cyan
}

function Ensure-Dir {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Download-File {
    param([string]$Url, [string]$Out)
    if ((Test-Path $Out) -and ((Get-Item $Out).Length -gt 1000)) { return }
    Write-Step "Downloading $Url"
    $parent = Split-Path -Parent $Out
    Ensure-Dir $parent
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        & curl.exe -L --fail --retry 3 --silent --show-error --output $Out $Url
    } else {
        Invoke-WebRequest -Uri $Url -OutFile $Out -UseBasicParsing -TimeoutSec 900
    }
    if (-not (Test-Path $Out) -or ((Get-Item $Out).Length -lt 1000)) {
        throw "Download failed: $Url"
    }
}

function Add-UserPath {
    param([string]$Entry)
    $entry = $Entry.TrimEnd('\')
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $parts = @($userPath -split ';' | Where-Object { $_ })
    if ($parts -notcontains $entry) {
        $parts += $entry
        [Environment]::SetEnvironmentVariable('Path', ($parts -join ';'), 'User')
    }
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' +
                [Environment]::GetEnvironmentVariable('Path', 'User')
}

function Set-JavaHome {
    param([string]$Path)
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $Path, 'User')
    $env:JAVA_HOME = $Path
    Add-UserPath (Join-Path $Path 'bin')
}

function Get-JavaMajor {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { return 0 }
    $versionText = (cmd /c "java -version 2>&1" | Out-String)
    if ($versionText -match 'version\s+"?(\d+)') {
        $major = [int]$Matches[1]
        if ($major -eq 1) { return 8 }
        return $major
    }
    return 0
}

Write-Host "MedFlow automatic setup" -ForegroundColor Green
Write-Host "Project: $ProjectRoot"
Write-Host "Tools:   $ToolsRoot"
Write-Host ""

Ensure-Dir $ToolsRoot
Ensure-Dir (Join-Path $ToolsRoot 'bin')

# --- 1. Java 17 ---------------------------------------------------------------
Write-Step "Checking Java (need 17 or newer)"
if ((Get-JavaMajor) -lt 17) {
    Write-Step "Java 17+ is missing - installing portable Temurin JDK 17"
    $jdkRoot = Join-Path $ToolsRoot 'jdk'
    Ensure-Dir $jdkRoot
    $jdkHome = $null
    $existingJdk = Get-ChildItem -Path $jdkRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
        Select-Object -First 1
    if ($existingJdk) {
        $jdkHome = $existingJdk.FullName
    } else {
        $jdkZip = Join-Path $ToolsRoot 'jdk17.zip'
        Download-File 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' $jdkZip
        $extractDir = Join-Path $ToolsRoot 'jdk-extract'
        Ensure-Dir $extractDir
        Expand-Archive -Path $jdkZip -DestinationPath $extractDir -Force
        $inner = Get-ChildItem -Path $extractDir -Directory |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
            Select-Object -First 1
        if (-not $inner) { throw 'JDK archive did not contain bin\\java.exe' }
        $jdkHome = Join-Path $jdkRoot $inner.Name
        if (-not (Test-Path $jdkHome)) { Move-Item -Path $inner.FullName -Destination $jdkHome }
        Remove-Item -Path $extractDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    Set-JavaHome $jdkHome
    Write-Step "Java installed: $jdkHome"
} else {
    Write-Step "Java $(Get-JavaMajor) found, skipping"
}

# --- 2. Git -------------------------------------------------------------------
Write-Step "Checking Git"
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Step "Git is missing - installing portable Git for Windows"
    $gitRoot = Join-Path $ToolsRoot 'git'
    $gitOk = $false
    try {
        $release = Invoke-RestMethod -Uri 'https://api.github.com/repos/git-for-windows/git/releases/latest' -TimeoutSec 30 -UseBasicParsing
        $asset = $release.assets | Where-Object { $_.name -match '^PortableGit-.*-64-bit\.7z\.exe$' } | Select-Object -First 1
        if ($asset) {
            $gitInstaller = Join-Path $ToolsRoot 'portable-git.exe'
            Download-File $asset.browser_download_url $gitInstaller
            Ensure-Dir $gitRoot
            & $gitInstaller -y "-o$gitRoot" | Out-Null
            if (Test-Path (Join-Path $gitRoot 'cmd\git.exe')) { $gitOk = $true }
        }
    } catch {
        Write-Step "Portable Git download failed: $($_.Exception.Message)"
    }
    if (-not $gitOk) {
        $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
        $principal = New-Object Security.Principal.WindowsPrincipal($identity)
        if ($principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
            & winget install --id Git.Git --accept-package-agreements --accept-source-agreements
            $gitOk = $true
        } else {
            throw 'Install Git manually: https://git-scm.com/download/win'
        }
    }
    Add-UserPath (Join-Path $gitRoot 'cmd')
    Add-UserPath (Join-Path $gitRoot 'bin')
    Write-Step "Git installed: $gitRoot"
} else {
    Write-Step "Git found: $((Get-Command git).Source)"
}

# --- 3. Maven wrapper ---------------------------------------------------------
Write-Step "Checking Maven Wrapper (it downloads Maven 3.9.9 on first use)"
Push-Location $ProjectRoot
try {
    & .\mvnw.cmd -v 2>&1 | Select-Object -First 2
} finally {
    Pop-Location
}

# --- 4. MySQL -----------------------------------------------------------------
Write-Step "Checking portable MySQL 8.4"
if (-not (Test-Path (Join-Path $MySqlHome 'bin\mysqld.exe'))) {
    Write-Step "MySQL is missing - installing portable MySQL 8.4.11"
    $mysqlZip = Join-Path $ToolsRoot 'mysql8411.zip'
    Download-File 'https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-winx64.zip' $mysqlZip
    $extractDir = Join-Path $ToolsRoot 'mysql-extract'
    Ensure-Dir $extractDir
    Expand-Archive -Path $mysqlZip -DestinationPath $extractDir -Force
    $inner = Get-ChildItem -Path $extractDir -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\mysqld.exe') } |
        Select-Object -First 1
    if (-not $inner) { throw 'MySQL archive did not contain bin\\mysqld.exe' }
    Ensure-Dir $MySqlHome
    Get-ChildItem -Path $inner.FullName -Force | Move-Item -Destination $MySqlHome -Force
    Remove-Item -Path $extractDir -Recurse -Force -ErrorAction SilentlyContinue
}

$mysqlStart = Join-Path $MySqlHome 'mysql-start.ps1'
$mysqlStop = Join-Path $MySqlHome 'mysql-stop.ps1'

$startScript = @'
#requires -Version 5.1
param(
    [string]$MySQLHome = "$env:USERPROFILE\mysql84",
    [int]$Port = 3306
)
$ErrorActionPreference = 'Stop'
$mysqld = Join-Path $MySQLHome 'bin\mysqld.exe'
$mysql = Join-Path $MySQLHome 'bin\mysql.exe'
$mysqladmin = Join-Path $MySQLHome 'bin\mysqladmin.exe'
$data = Join-Path $MySQLHome 'data'
$pidFile = Join-Path $MySQLHome 'mysqld.pid'
if (-not (Test-Path $mysqld)) { throw "mysqld.exe not found in $MySQLHome\bin" }
if (-not (Test-Path $data)) {
    Write-Host "Initializing data directory (first run only)..."
    New-Item -ItemType Directory -Path $data -Force | Out-Null
    & $mysqld --initialize-insecure "--basedir=$MySQLHome" "--datadir=$data" --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci 2>&1
    if ($LASTEXITCODE -ne 0) { throw "MySQL initialization failed" }
}
$listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $listening) {
    Write-Host "Starting MySQL on port $Port ..."
    $serverArgs = @(
        "--basedir=$MySQLHome",
        "--datadir=$data",
        "--port=$Port",
        "--bind-address=127.0.0.1",
        "--character-set-server=utf8mb4",
        "--collation-server=utf8mb4_unicode_ci",
        "--default-storage-engine=InnoDB"
    )
    $process = Start-Process -FilePath $mysqld -ArgumentList $serverArgs -PassThru -WindowStyle Hidden
    Set-Content -Path $pidFile -Value $process.Id
}
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    $ping = & $mysqladmin "--protocol=tcp" "-h127.0.0.1" "-P$Port" "-uroot" ping 2>&1
    if ($LASTEXITCODE -eq 0 -and "$ping" -match 'alive') { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw "MySQL did not become ready on port $Port" }
$sql = @"
CREATE DATABASE IF NOT EXISTS hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hospital'@'%' IDENTIFIED BY 'hospital_pass';
CREATE USER IF NOT EXISTS 'hospital'@'localhost' IDENTIFIED BY 'hospital_pass';
GRANT ALL PRIVILEGES ON hospital_db.* TO 'hospital'@'%';
GRANT ALL PRIVILEGES ON hospital_db.* TO 'hospital'@'localhost';
FLUSH PRIVILEGES;
"@
& $mysql "--protocol=tcp" "-h127.0.0.1" "-P$Port" "-uroot" "--default-character-set=utf8mb4" -e $sql 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not create hospital_db / hospital user" }
Write-Host "MySQL is ready: database hospital_db, user hospital, port $Port"
'@

$stopScript = @'
#requires -Version 5.1
param([string]$MySQLHome = "$env:USERPROFILE\mysql84")
$ErrorActionPreference = 'Continue'
$stopped = 0
Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" -ErrorAction SilentlyContinue |
    Where-Object {
        $_.ExecutablePath -like "$MySQLHome*" -or
        $_.CommandLine -match [regex]::Escape("--datadir=$MySQLHome")
    } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped MySQL process $($_.ProcessId)"
        $stopped++
    }
if ($stopped -eq 0) { Write-Host "MySQL is not running." }
$pidFile = Join-Path $MySQLHome 'mysqld.pid'
if (Test-Path $pidFile) { Remove-Item $pidFile -Force -ErrorAction SilentlyContinue }
'@

Set-Content -Path $mysqlStart -Value $startScript -Encoding UTF8
Set-Content -Path $mysqlStop -Value $stopScript -Encoding UTF8

Write-Step "Starting MySQL and creating hospital_db"
& powershell -NoProfile -ExecutionPolicy Bypass -File $mysqlStart
if ($LASTEXITCODE -ne 0) { throw 'MySQL start failed' }

# --- 5. Docker compatibility --------------------------------------------------
Write-Step "Checking Docker"
$dockerInfo = Get-Command docker -ErrorAction SilentlyContinue
$dockerOk = $false
if ($dockerInfo) {
    if ($dockerInfo.Source -like '*\.medflow-tools\*') {
        $dockerOk = $true
    } else {
        try {
            & docker --version 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
        } catch { }
    }
}

$dockerMode = 'docker'
if (-not $dockerOk) {
    Write-Step "Real Docker is unavailable - installing compatibility shim for 'docker compose'"
    $shim = @'
@echo off
setlocal
set "REAL_DOCKER="
for %%P in (
  "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe"
  "%ProgramFiles(x86)%\Docker\Docker\resources\bin\docker.exe"
  "%LOCALAPPDATA%\Programs\Docker\Docker\resources\bin\docker.exe"
) do if exist %%P set "REAL_DOCKER=%%~P"
if defined REAL_DOCKER (
  "%REAL_DOCKER%" %*
  exit /b %ERRORLEVEL%
)
if /i "%~1"=="compose" goto COMPOSE
if /i "%~1"=="--version" goto VERSION
if /i "%~1"=="version" goto VERSION
if /i "%~1"=="info" goto INFO
echo [MedFlow] Docker Desktop is not installed. This compatibility shim supports: docker compose up^|down^|ps, docker --version, docker info. 1>&2
exit /b 1

:VERSION
echo Docker version 24.0.0, build medflow-portable
exit /b 0

:INFO
echo Server Version: 24.0.0 (portable MySQL compatibility mode)
echo [MedFlow] Real Docker is not installed; portable MySQL 8.4 is used instead.
exit /b 0

:COMPOSE
if /i "%~2"=="up" goto UP
if /i "%~2"=="down" goto DOWN
if /i "%~2"=="ps" goto PS
echo [MedFlow] Unsupported docker compose command: %~2 1>&2
exit /b 1

:UP
powershell -NoProfile -ExecutionPolicy Bypass -File "__MYSQL_START__"
if errorlevel 1 exit /b 1
echo [MedFlow] Portable MySQL is up: hospital_db on 127.0.0.1:3306.
exit /b 0

:DOWN
powershell -NoProfile -ExecutionPolicy Bypass -File "__MYSQL_STOP__"
if errorlevel 1 exit /b 1
echo [MedFlow] Portable MySQL is stopped.
exit /b 0

:PS
powershell -NoProfile -Command "$c = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue; Write-Host 'NAME                STATUS'; if ($c) { Write-Host 'medflow-mysql       Up (127.0.0.1:3306)' } else { Write-Host 'medflow-mysql       Exited' }"
exit /b 0
'@
    $shim = $shim -replace '__MYSQL_START__', (Join-Path $MySqlHome 'mysql-start.ps1')
    $shim = $shim -replace '__MYSQL_STOP__', (Join-Path $MySqlHome 'mysql-stop.ps1')
    Set-Content -Path (Join-Path $ToolsRoot 'bin\docker.cmd') -Value $shim -Encoding ASCII
    Add-UserPath (Join-Path $ToolsRoot 'bin')
    $dockerMode = 'shim'
} else {
    Write-Step "Real Docker found, leaving it as is"
}

# --- 6. ASCII junction for spring-boot:run ----------------------------------
Write-Step "Preparing ASCII junction for the project path"
$junction = $PreferredJunction
$created = $false
for ($i = 0; $i -lt 10; $i++) {
    if (-not (Test-Path -LiteralPath $junction)) {
        & cmd /c mklink /J "$junction" "$ProjectRoot" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Cannot create junction $junction" }
        $created = $true
        break
    }
    $item = Get-Item -LiteralPath $junction -Force
    if ($item.LinkType -eq 'Junction') {
        if (@($item.Target) -contains $ProjectRoot) { break }
        & cmd /c rmdir "$junction" | Out-Null
        & cmd /c mklink /J "$junction" "$ProjectRoot" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Cannot recreate junction $junction" }
        $created = $true
        break
    }
    $junction = Join-Path $env:SystemDrive ("MedFlow" + ($i + 2))
}
if ($created) { Write-Step "Junction created: $junction" } else { Write-Step "Junction already points to the project: $junction" }

# --- 7. Marker and summary ---------------------------------------------------
$config = [ordered]@{
    Junction    = $junction
    JavaHome    = $env:JAVA_HOME
    MySqlHome   = $MySqlHome
    ToolsRoot   = $ToolsRoot
    DockerMode  = $dockerMode
    ProjectRoot = $ProjectRoot
    SetupAt     = (Get-Date).ToString('o')
}
$config | ConvertTo-Json | Set-Content -Path (Join-Path $ToolsRoot 'medflow-setup.json') -Encoding UTF8

Write-Host ""
Write-Host "MedFlow setup is complete." -ForegroundColor Green
Write-Host "Open a NEW PowerShell window, then run:" -ForegroundColor Yellow
Write-Host "  cd $junction"
Write-Host "  .\setup.ps1          # safe to re-run any time"
Write-Host "  .\mvnw.cmd spring-boot:run"
Write-Host ""
Write-Host "Docker mode: $dockerMode"
Write-Host "MySQL:       $MySqlHome (hospital_db / hospital / hospital_pass)"
Write-Host "Junction:    $junction -> $ProjectRoot"

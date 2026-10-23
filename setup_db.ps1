# =====================================================================
# Personal Expense Tracker - Automated PowerShell Database Setup
# =====================================================================

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host " Personal Expense Tracker - Automated MySQL Setup" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will:" -ForegroundColor Yellow
Write-Host " 1. Create a dedicated 'expense_tracker' database"
Write-Host " 2. Grant 'skillgap_user' access only to 'expense_tracker.*'"
Write-Host " 3. Preserve 'skill_gap_db' and all existing data"
Write-Host " 4. Import categories and transactions tables with seed data"
Write-Host " 5. Point Tomcat conf\db.properties to 'expense_tracker'"
Write-Host ""

$password = Read-Host "Enter MySQL root password (press Enter if blank)"

$mysqlPath = "mysql"
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe") {
        $mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    } elseif (Test-Path "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe") {
        $mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    } else {
        Write-Host "[Error] MySQL CLI binary not found in PATH or Program Files." -ForegroundColor Red
        exit 1
    }
}

$passArg = if ($password) { "-p$password" } else { "" }

Write-Host "`n[1/3] Creating 'expense_tracker' database and granting access to 'skillgap_user'..." -ForegroundColor Green
$grantSql = "CREATE DATABASE IF NOT EXISTS expense_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON expense_tracker.* TO 'skillgap_user'@'localhost'; FLUSH PRIVILEGES;"
$proc1 = Start-Process -FilePath $mysqlPath -ArgumentList "-u root $passArg -e `"$grantSql`"" -NoNewWindow -PassThru -Wait

if ($proc1.ExitCode -ne 0) {
    Write-Host "[Error] Failed to create database or grant permissions. Please check MySQL root password." -ForegroundColor Red
    exit 1
}

Write-Host "[2/3] Populating schema and transactions into 'expense_tracker'..." -ForegroundColor Green
$sqlFile = Resolve-Path "database\database.sql"
$proc2 = Start-Process -FilePath $mysqlPath -ArgumentList "-u root $passArg expense_tracker" -RedirectStandardInput $sqlFile -NoNewWindow -PassThru -Wait

if ($proc2.ExitCode -ne 0) {
    Write-Host "[Error] Failed to import database.sql into 'expense_tracker'." -ForegroundColor Red
    exit 1
}

Write-Host "[3/3] Updating Tomcat configuration to point to 'expense_tracker'..." -ForegroundColor Green
$confPath = "C:\apache-tomcat-9\conf\db.properties"
if (Test-Path $confPath) {
    (Get-Content $confPath) -replace 'db\.name\s*=.*', 'db.name=expense_tracker' | Set-Content $confPath -Encoding UTF8
    Write-Host "[Tomcat] Successfully updated db.name=expense_tracker in $confPath" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] 'expense_tracker' database initialized successfully!" -ForegroundColor Green
Write-Host "====================================================================" -ForegroundColor Cyan

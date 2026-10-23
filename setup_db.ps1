# =====================================================================
# Personal Expense Tracker - PowerShell MySQL Database Setup Script
# =====================================================================

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host " Personal Expense Tracker - MySQL Database Setup" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
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
        Write-Host "Please import database\database.sql manually via MySQL Workbench." -ForegroundColor Yellow
        exit 1
    }
}

$passArg = if ($password) { "-p$password" } else { "" }
Write-Host "[Database] Initializing 'expense_tracker' database and tables..." -ForegroundColor Green

$sqlFile = Resolve-Path "database\database.sql"
$proc = Start-Process -FilePath $mysqlPath -ArgumentList "-u root $passArg" -RedirectStandardInput $sqlFile -NoNewWindow -PassThru -Wait

if ($proc.ExitCode -eq 0) {
    Write-Host "[Database] Database 'expense_tracker' created and seeded successfully!" -ForegroundColor Green

    $tomcatConf = "C:\apache-tomcat-9\conf"
    if (Test-Path $tomcatConf) {
        $confPath = Join-Path $tomcatConf "db.properties"
        $content = @"
# Personal Expense Tracker Database Configuration
db.host=localhost
db.port=3306
db.name=expense_tracker
db.user=root
db.pass=$password
"@
        Set-Content -Path $confPath -Value $content -Encoding UTF8
        Write-Host "[Tomcat] Configured $confPath with expense_tracker database!" -ForegroundColor Cyan
    }
    Write-Host "Done! You can now start Tomcat and open the dashboard." -ForegroundColor Green
} else {
    Write-Host "[Error] Setup failed. Please check MySQL root password or service status." -ForegroundColor Red
}

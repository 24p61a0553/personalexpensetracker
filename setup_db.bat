@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo  Personal Expense Tracker - Automated MySQL Setup
echo ====================================================================
echo.
echo This script will:
echo  1. Create a dedicated 'expense_tracker' database
echo  2. Grant 'skillgap_user' access only to 'expense_tracker.*'
echo  3. Preserve 'skill_gap_db' and all existing data
echo  4. Import categories and transactions tables with seed data
echo  5. Point Tomcat conf\db.properties to 'expense_tracker'
echo.
set /p MYSQL_PASS="Enter MySQL 'root' password: "

set MYSQL_BIN=mysql
where mysql >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
        set MYSQL_BIN="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    ) else if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
        set MYSQL_BIN="C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    ) else (
        echo [Error] MySQL CLI binary not found in PATH or Program Files.
        pause
        exit /b 1
    )
)

echo.
echo [1/3] Creating 'expense_tracker' database and granting access to 'skillgap_user'...
if "%MYSQL_PASS%"=="" (
    %MYSQL_BIN% -u root -e "CREATE DATABASE IF NOT EXISTS expense_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON expense_tracker.* TO 'skillgap_user'@'localhost'; FLUSH PRIVILEGES;"
    if !ERRORLEVEL! neq 0 goto failure
    echo [2/3] Populating schema and transactions into 'expense_tracker'...
    %MYSQL_BIN% -u root expense_tracker < database\database.sql
    if !ERRORLEVEL! neq 0 goto failure
) else (
    %MYSQL_BIN% -u root -p"%MYSQL_PASS%" -e "CREATE DATABASE IF NOT EXISTS expense_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON expense_tracker.* TO 'skillgap_user'@'localhost'; FLUSH PRIVILEGES;"
    if !ERRORLEVEL! neq 0 goto failure
    echo [2/3] Populating schema and transactions into 'expense_tracker'...
    %MYSQL_BIN% -u root -p"%MYSQL_PASS%" expense_tracker < database\database.sql
    if !ERRORLEVEL! neq 0 goto failure
)

echo [3/3] Updating Tomcat configuration...
powershell -NoProfile -Command "$path = 'C:\apache-tomcat-9\conf\db.properties'; if (Test-Path $path) { (Get-Content $path) -replace 'db\.name\s*=.*', 'db.name=expense_tracker' | Set-Content $path -Encoding UTF8; Write-Host '[Tomcat] Updated db.name=expense_tracker in' $path }"

echo.
echo ====================================================================
echo [SUCCESS] 'expense_tracker' database is initialized and ready!
echo ====================================================================
echo.
pause
exit /b 0

:failure
echo.
echo [Error] Database creation failed.
echo Please verify your MySQL root password and ensure MySQL service is running.
echo ====================================================================
pause
exit /b 1

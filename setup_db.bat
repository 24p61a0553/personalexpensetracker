@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo Personal Expense Tracker - MySQL Database Setup Script
echo ====================================================================
echo.

set /p MYSQL_PASS="Enter MySQL 'root' password (press Enter if blank): "

set MYSQL_BIN=mysql
where mysql >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
        set MYSQL_BIN="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    ) else if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
        set MYSQL_BIN="C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    ) else (
        echo [Error] MySQL CLI binary not found in PATH or Program Files.
        echo Please import database\database.sql manually in MySQL Workbench.
        pause
        exit /b 1
    )
)

echo.
echo [Database] Creating 'expense_tracker' database and schema...
if "%MYSQL_PASS%"=="" (
    %MYSQL_BIN% -u root < database\database.sql
    if !ERRORLEVEL! neq 0 goto failure
) else (
    %MYSQL_BIN% -u root -p"%MYSQL_PASS%" < database\database.sql
    if !ERRORLEVEL! neq 0 goto failure
)

echo.
echo ====================================================================
echo [Database] SUCCESS! 'expense_tracker' is initialized with seed data!
echo ====================================================================
echo.

:: Automatically offer to configure Tomcat conf\db.properties if Tomcat exists
if exist "C:\apache-tomcat-9\conf" (
    echo [Tomcat] Detected Tomcat at C:\apache-tomcat-9
    echo [Tomcat] Updating C:\apache-tomcat-9\conf\db.properties with your MySQL credentials...
    (
        echo # Personal Expense Tracker Database Configuration
        echo db.host=localhost
        echo db.port=3306
        echo db.name=expense_tracker
        echo db.user=root
        echo db.pass=%MYSQL_PASS%
    ) > "C:\apache-tomcat-9\conf\db.properties"
    echo [Tomcat] Saved! Tomcat will now automatically connect to expense_tracker.
)

echo.
echo Setup completed successfully!
pause
exit /b 0

:failure
echo.
echo [Error] Failed to execute database\database.sql.
echo Please verify your MySQL root password and ensure MySQL service is running.
echo ====================================================================
pause
exit /b 1

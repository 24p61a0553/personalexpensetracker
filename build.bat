@echo off
echo ====================================================================
echo Personal Expense Tracker - Ant Build & Packaging Script
echo ====================================================================
echo.

where ant >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [Error] Apache Ant is not installed or not in your PATH.
    echo Please install Apache Ant or run build.ps1 via PowerShell.
    pause
    exit /b 1
)

echo [1/3] Cleaning previous builds...
call ant clean

echo.
echo [2/3] Compiling Java Servlets and DAOs...
call ant compile
if %ERRORLEVEL% neq 0 (
    echo [Error] Compilation failed! Check error messages above.
    pause
    exit /b 1
)

echo.
echo [3/3] Packaging Deployable WAR file...
call ant war
if %ERRORLEVEL% neq 0 (
    echo [Error] WAR packaging failed!
    pause
    exit /b 1
)

echo.
echo ====================================================================
echo BUILD SUCCESSFUL!
echo WAR Location: dist\PersonalExpenseTracker.war
echo ====================================================================
echo.
pause

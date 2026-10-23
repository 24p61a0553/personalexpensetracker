# =====================================================================
# Personal Expense Tracker - PowerShell Build Script
# =====================================================================

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host " Personal Expense Tracker - Build & WAR Packaging" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

if (Get-Command ant -ErrorAction SilentlyContinue) {
    Write-Host "[Ant] Detected Apache Ant on PATH. Invoking 'ant war'..." -ForegroundColor Green
    & ant war
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[SUCCESS] WAR package created at dist\PersonalExpenseTracker.war" -ForegroundColor Green
        exit 0
    }
}

Write-Host "[Fallback] Compiling with OpenJDK javac..." -ForegroundColor Yellow
$buildClasses = "build\classes"
$distDir = "dist"
$warTemp = "build\war_temp"

New-Item -ItemType Directory -Force -Path $buildClasses, $distDir, $warTemp | Out-Null

$sources = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$classpath = "lib\*;WebContent\WEB-INF\lib\*"

& javac -cp $classpath -d $buildClasses -encoding UTF-8 $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[Error] Javac compilation failed!" -ForegroundColor Red
    exit 1
}

# Copy properties
Copy-Item "src\db.properties" "$buildClasses\" -Force -ErrorAction SilentlyContinue

# Package WAR
Copy-Item -Path "WebContent\*" -Destination $warTemp -Recurse -Force
Copy-Item -Path $buildClasses -Destination "$warTemp\WEB-INF\" -Recurse -Force

Set-Location $warTemp
& jar -cvf "..\..\dist\PersonalExpenseTracker.war" *
Set-Location ..\..

Remove-Item -Recurse -Force $warTemp -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Built WAR package at: dist\PersonalExpenseTracker.war" -ForegroundColor Green
Write-Host "====================================================================" -ForegroundColor Cyan

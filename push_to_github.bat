@echo off
echo ====================================================================
echo  Personal Expense Tracker - GitHub Push
echo ====================================================================
echo.
echo Target Repository: https://github.com/24p61a0553/personalexpensetracker.git
echo Branch: main
echo.
echo Launching Git Push...
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Your project has been successfully uploaded to GitHub!
) else (
    echo [ERROR] Push failed. If prompted, please complete the GitHub login in your browser.
)
echo ====================================================================
pause

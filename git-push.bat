@echo off
:: 🚀 Auto Git Commit & Push Script

:: Set terminal title
title Git Push Utility

echo ================================================
echo    Auto Git Push Utility
echo ================================================
echo.

:: Show current git status
git status

echo.
echo Adding all changes...
git add .

:: Ask for commit message
set /p msg=Enter commit message (default: "Auto update"): 
if "%msg%"=="" set msg=Auto update

echo.
echo Committing changes...
git commit -m "%msg%"

echo.
echo Pushing to remote...
git push

echo.
echo Done!
echo ================================================

pause

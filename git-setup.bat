@echo off
:: 👑 Git Full Auto Setup & Push by Abdul Mueed

title 🚀 GIT INIT & PUSH - Abdul Mueed 💻

echo ======================================================
echo 👾  Welcome, Abdul Mueed — Let’s push this repo in style
echo ======================================================
echo.

:: Check if git is installed
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Git not found! Please install Git first.
    pause
    exit /b
)

:: Ask for GitHub repo URL
set /p repoUrl=🌐 Enter your GitHub repo URL (e.g. https://github.com/username/repo.git): 

:: Initialize git (if not already)
if not exist ".git" (
    echo 🧩 Initializing Git repository...
    git init
) else (
    echo 🔁 Git repo already initialized.
)

:: Add all files
echo ➕ Adding files...
git add .

:: Commit with message
set /p msg=💬 Enter commit message (default: "Initial commit"): 
if "%msg%"=="" set msg=Initial commit
git commit -m "%msg%"

:: Create & switch to main branch (if not exists)
echo 🪄 Setting up main branch...
git branch -M main

:: Add remote origin
echo 🌍 Linking remote repository...
git remote remove origin >nul 2>&1
git remote add origin %repoUrl%

:: Push to GitHub
echo 🚀 Pushing code to remote repo...
git push -u origin main

echo.
echo ✅ SUCCESS! Your repo has been pushed to GitHub, boss 😎
echo ======================================================
pause

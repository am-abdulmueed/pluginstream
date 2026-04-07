@echo off
:menu
cls
echo ======================================================
echo            GRADLE BUILD MANAGER (GamePlus)
echo ======================================================
echo.
echo  [1] Build DEBUG APK   (assembleDebug)
echo  [2] Build RELEASE APK (assembleRelease)
echo  [3] Clean Project     (gradlew clean)
echo  [4] Exit
echo.
echo ======================================================
set /p choice="Enter your choice (1, 2, 3, or 4): "

if "%choice%"=="1" (
    echo Starting DEBUG Build...
    call .\gradlew assembledebug
    pause
    goto menu
)

if "%choice%"=="2" (
    echo Starting RELEASE Build...
    call .\gradlew assemblerelease
    pause
    goto menu
)

if "%choice%"=="3" (
    echo Cleaning project...
    call .\gradlew clean
    pause
    goto menu
)

if "%choice%"=="4" (
    echo Exiting...
    exit /b
)

echo Invalid choice, try again.
pause
goto menu
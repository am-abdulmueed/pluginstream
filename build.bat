@echo off
:menu
cls
echo ======================================================
echo             GRADLE BUILD MANAGER (PluginStream)
echo ======================================================
echo.
echo  [1] Build DEBUG APK      (assembleDebug)
echo  [2] Build RELEASE APK    (assembleRelease)
echo  [3] Clean Project        (gradlew clean)
echo  [4] Start Python Server  (Port 1010)
echo  [5] Exit
echo.
echo ======================================================
set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" (
    echo Starting DEBUG Build...
    call .\gradlew assembleDebug
    pause
    goto menu
)

if "%choice%"=="2" (
    echo Starting RELEASE Build...
    call .\gradlew assembleRelease
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
    echo Starting Python Server on Port 1010...
    echo Press Ctrl+C to stop the server.
    cd /d "E:\Projects\IN_BUILD\cloudstream\app\build\outputs\apk\prerelease\debug"
    python -m http.server 1010
    pause
    goto menu
)

if "%choice%"=="5" (
    echo Exiting...
    exit /b
)

echo Invalid choice, try again.
pause
goto menu
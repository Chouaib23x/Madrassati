@echo off
chcp 65001 >nul
echo ============================================
echo   MADRASATI - Installation debug
echo ============================================
echo.
set APK=app\build\outputs\apk\debug\app-debug.apk
if not exist %APK% (
    echo [ECHEC] APK introuvable : lancez d'abord  gradlew.bat assembleDebug
    pause
    exit /b 1
)
adb install -r %APK%
if %ERRORLEVEL% EQU 0 (
    echo [OK] Application installee.
    adb shell am start -n dz.madrassati/.MainActivity
) else (
    echo [ECHEC] Installation impossible.
)
pause

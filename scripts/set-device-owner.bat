@echo off
chcp 65001 >nul
echo ============================================
echo   MADRASATI - Activation Device Owner
echo ============================================
echo.
echo La tablette doit :
echo   1. Etre deconnectee de tout compte (pas de Google, pas de travail)
echo   2. Avoir le debogage USB active
echo   3. Etre connectee a ce PC
echo.
adb devices
echo.
adb shell dpm set-device-owner dz.madrassati/.kiosk.KioskAdminReceiver
echo.
if %ERRORLEVEL% EQU 0 (
    echo [OK] Device Owner active.
) else (
    echo [ECHEC] Verifiez les 3 conditions ci-dessus.
)
pause

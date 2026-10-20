@echo off
chcp 65001 >nul
echo ============================================
echo   MADRASATI - Retrait Device Owner
echo ============================================
echo.
adb shell dpm remove-active-admin dz.madrassati/.kiosk.KioskAdminReceiver
echo.
if %ERRORLEVEL% EQU 0 (
    echo [OK] Device Owner retire.
) else (
    echo [ECHEC] Impossible de retirer l'administrateur.
)
pause

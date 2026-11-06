@echo off
REM Smart Emergency Command Center - starts backend + frontend and opens the browser.
REM Double-click this file. Logs are written to the "logs" folder.
cd /d "%~dp0"
if not exist logs mkdir logs

echo Starting backend (Spring Boot, port 8080)...
start "SECC backend" powershell -NoExit -ExecutionPolicy Bypass -Command "Set-Location '%~dp0backend'; mvn clean spring-boot:run 2>&1 | Tee-Object -FilePath '%~dp0logs\backend.log'"

echo Starting frontend (React, port 5173)...
start "SECC frontend" powershell -NoExit -ExecutionPolicy Bypass -Command "Set-Location '%~dp0frontend'; if (-not (Test-Path node_modules)) { npm install 2>&1 | Tee-Object -FilePath '%~dp0logs\npm-install.log' }; npm run dev 2>&1 | Tee-Object -FilePath '%~dp0logs\frontend.log'"

echo Waiting for the backend to start (this can take 1-3 minutes the first time)...
set /a tries=0
:wait
set /a tries+=1
powershell -NoProfile -Command "try { (New-Object Net.Sockets.TcpClient('localhost',8080)).Close(); exit 0 } catch { exit 1 }"
if not errorlevel 1 goto up
if %tries% GEQ 100 goto timeout
timeout /t 3 /nobreak >nul
goto wait

:up
echo Backend is running. Opening http://localhost:5173 ...
timeout /t 5 /nobreak >nul
start "" http://localhost:5173
echo.
echo Done. Close the two PowerShell windows to stop the application.
pause
exit /b

:timeout
echo The backend did not start. Look at the "SECC backend" window or logs\backend.log.
pause

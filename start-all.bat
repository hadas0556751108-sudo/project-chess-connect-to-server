@echo off
pushd "%~dp0"

call build.bat
if %errorlevel% neq 0 (
    echo Build failed
    popd
    exit /b %errorlevel%
)

start "Chess Server" cmd /k "run-server.bat"
timeout /t 4 /nobreak >nul

start "Chess Client 1" cmd /k "run-client.bat"
timeout /t 1 /nobreak >nul

start "Chess Client 2" cmd /k "run-client.bat"

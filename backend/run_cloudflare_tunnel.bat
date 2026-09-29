@echo off
title Cloudflare Quick Tunnel + Auto Update - Aula Virtual
color 0B
echo ===================================================
echo     AULA VIRTUAL - CLOUDFLARE AUTOMATED TUNNEL
echo ===================================================
echo.
echo Requisitos:
echo 1. Asegurate de tener FastAPI ejecutandose en http://localhost:8000
echo.

set PY_CMD=py
where py >nul 2>nul
if %errorlevel% neq 0 (
    set PY_CMD=python
)

if exist "%~dp0run_tunnel_and_update_app.py" (
    echo [i] Ejecutando script de auto-actualizacion de RetrofitClient.java...
    %PY_CMD% "%~dp0run_tunnel_and_update_app.py"
) else (
    echo [!] Ejecutando Cloudflare Tunnel directamente...
    cloudflared tunnel --protocol http2 --edge-ip-version 4 --no-autoupdate --url http://127.0.0.1:8000
)

pause

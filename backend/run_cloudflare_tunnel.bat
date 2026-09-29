@echo off
title Cloudflare Quick Tunnel - Aula Virtual
color 0B
echo ===================================================
echo     AULA VIRTUAL - CLOUDFLARE QUICK TUNNEL
echo ===================================================
echo.
echo Requisitos:
echo 1. Asegurate de tener FastAPI ejecutandose en http://localhost:8000
echo.

set CLOUDFLARED_CMD=cloudflared
where cloudflared >nul 2>nul
if %errorlevel% neq 0 (
    if exist .\cloudflared.exe (
        set CLOUDFLARED_CMD=.\cloudflared.exe
    ) else (
        if exist %~dp0cloudflared.exe (
            set CLOUDFLARED_CMD=%~dp0cloudflared.exe
        ) else (
            echo [!] cloudflared no esta instalado en el PATH ni en la carpeta actual.
            echo [i] Descargando executable cloudflared.exe...
            powershell -Command "Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile '%~dp0cloudflared.exe'"
            if exist %~dp0cloudflared.exe (
                set CLOUDFLARED_CMD=%~dp0cloudflared.exe
            ) else (
                echo [ERROR] No se pudo descargar cloudflared.
                pause
                exit /b 1
            )
        )
    )
)

echo [OK] Iniciando Cloudflare Tunnel sobre HTTP2 e IPv4...
echo.
echo [!] NOTA: La linea 'Registered tunnel connection... location=...' confirma que el tunel esta 100%% ACTIVO.
echo [!] Copia la URL con dominio .trycloudflare.com que aparece arriba en la pantalla.
echo.

%CLOUDFLARED_CMD% tunnel --protocol http2 --edge-ip-version 4 --no-autoupdate --url http://127.0.0.1:8000

pause

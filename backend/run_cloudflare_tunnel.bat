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
        echo [!] cloudflared no esta instalado en el PATH ni en la carpeta actual.
        echo [i] Descargando executable cloudflared.exe...
        powershell -Command "Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile 'cloudflared.exe'"
        if exist cloudflared.exe (
            set CLOUDFLARED_CMD=.\cloudflared.exe
        ) else (
            echo [ERROR] No se pudo descargar cloudflared. Descargalo manualmente de:
            echo https://github.com/cloudflare/cloudflared/releases
            pause
            exit /b 1
        )
    )
)

echo [OK] Iniciando Cloudflare Tunnel con protocolo HTTP2 e IPv4 para evitar bloqueos de DNS/Firewall...
echo.
%CLOUDFLARED_CMD% tunnel --protocol http2 --edge-ip-version 4 --url http://localhost:8000

pause

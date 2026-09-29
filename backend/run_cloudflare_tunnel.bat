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

where cloudflared >nul 2>nul
if %errorlevel% neq 0 (
    if exist .\cloudflared.exe (
        echo [OK] Ejecutando cloudflared.exe local...
        .\cloudflared.exe tunnel --url http://localhost:8000
    ) else (
        echo [!] cloudflared no esta instalado en el PATH ni en la carpeta actual.
        echo [i] Descargando executable cloudflared.exe...
        powershell -Command "Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile 'cloudflared.exe'"
        if exist cloudflared.exe (
            echo [OK] cloudflared.exe descargado exitosamente.
            echo [i] Iniciando Quick Tunnel hacia http://localhost:8000 ...
            .\cloudflared.exe tunnel --url http://localhost:8000
        ) else (
            echo [ERROR] No se pudo descargar cloudflared. Descargalo manualmente de:
            echo https://github.com/cloudflare/cloudflared/releases
        )
    )
) else (
    echo [OK] Ejecutando Cloudflare Quick Tunnel desde PATH...
    cloudflared tunnel --url http://localhost:8000
)

pause

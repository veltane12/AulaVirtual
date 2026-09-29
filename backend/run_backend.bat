@echo off
title FastAPI Backend - Aula Virtual
color 0A
cd /d "%~dp0"
echo ===================================================
echo     AULA VIRTUAL - SERVIDOR BACKEND FASTAPI
echo ===================================================
echo.

set PY_CMD=py
where py >nul 2>nul
if %errorlevel% neq 0 (
    set PY_CMD=python
)

if exist .\venv\Scripts\activate.bat (
    echo [i] Activando entorno virtual venv...
    call .\venv\Scripts\activate.bat
)

echo [i] Iniciando servidor FastAPI con Uvicorn en http://localhost:8000 ...
%PY_CMD% -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload

pause

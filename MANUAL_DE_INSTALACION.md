# 📘 Guía de Instalación y Configuración del Proyecto - Aula Virtual

Este documento proporciona los pasos detallados y las herramientas necesarias para instalar, configurar y ejecutar el proyecto **Aula Virtual** desde cero en una nueva computadora con una instalación reciente de **Android Studio**.

---

## 🔗 Links de Descarga de Requisitos Previos

Antes de comenzar, asegúrate de descargar e instalar las siguientes herramientas en la nueva PC:

| Herramienta | Descripción | Link de Descarga |
| :--- | :--- | :--- |
| **Android Studio** | IDE oficial para desarrollo Android (incluye Android SDK, Emulador y JDK) | [Descargar Android Studio](https://developer.android.com/studio) |
| **XAMPP** | Servidor local para gestionar la base de datos MySQL / MariaDB | [Descargar XAMPP](https://www.apachefriends.org/es/index.html) |
| **Python** (v3.10 o superior) | Entorno de ejecución para el servidor backend FastAPI | [Descargar Python](https://www.python.org/downloads/) |
| **Git** | Control de versiones para clonar el repositorio | [Descargar Git](https://git-scm.com/downloads) |

---

## 🛠️ Pasos de Instalación y Configuración

### Paso 1: Clonar u Obtener el Proyecto

Abre la terminal o PowerShell y clona el repositorio (o copia la carpeta del proyecto en tu disco local):

```bash
git clone <URL_DEL_REPOSITORIO>
cd MyApplication
```

---

### Paso 2: Configurar la Base de Datos (MySQL con XAMPP)

1. Abre el **XAMPP Control Panel**.
2. Inicia el servicio **MySQL** (y **Apache** si deseas usar phpMyAdmin).
3. Abre tu navegador web e ingresa a phpMyAdmin:
   [http://localhost/phpmyadmin/](http://localhost/phpmyadmin/)
4. Crea una nueva base de datos llamada `aula_virtual`:
   - Haz clic en **Nueva**.
   - Nombre de la base de datos: `aula_virtual`.
   - Cotejamiento: `utf8mb4_unicode_ci`.
   - Haz clic en **Crear**.
5. Importa la estructura y datos iniciales:
   - Selecciona la base de datos `aula_virtual`.
   - Ve a la pestaña **Importar**.
   - Selecciona el archivo `backend/init.sql` ubicado en la carpeta del proyecto.
   - Haz clic en **Importar** (o **Continuar**) al final de la página.

> **Método alternativo por Consola/Terminal:**
> ```bash
> mysql -u root -e "CREATE DATABASE aula_virtual CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
> mysql -u root aula_virtual < backend/init.sql
> ```

---

### Paso 3: Configurar y Levantar el Servidor Backend (FastAPI)

1. Abre una terminal y navega a la carpeta `backend` del proyecto:
   ```bash
   cd backend
   ```
2. Crea un entorno virtual de Python (recomendado):
   - **En Windows (PowerShell/CMD):**
     ```powershell
     python -m venv venv
     .\venv\Scripts\activate
     ```
   - **En macOS / Linux:**
     ```bash
     python3 -m venv venv
     source venv/bin/activate
     ```
3. Instala las dependencias requeridas de Python:
   ```bash
   pip install fastapi uvicorn sqlalchemy mysql-connector-python pydantic
   ```
4. Verificación de credenciales de la base de datos:
   - Revisa el archivo `backend/main.py`.
   - Por defecto, la conexión se realiza sin contraseña para el usuario `root` de XAMPP:
     `DATABASE_URL = "mysql+mysqlconnector://root:@localhost/aula_virtual"`
   - Si tu MySQL en XAMPP tiene contraseña, actualízala en esa línea (ej. `root:tu_password@localhost`).
5. Ejecuta el servidor backend:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```
6. **Verificación:** Abre en tu navegador [http://localhost:8000/docs](http://localhost:8000/docs). Deberías ver la documentación interactiva de la API (Swagger UI).

---

### Paso 4: Configurar la Aplicación Android en Android Studio

1. Abre **Android Studio**.
2. En la pantalla principal, selecciona **Open** y elige la carpeta raíz del proyecto (`MyApplication`).
3. Espera a que Android Studio descargue las dependencias Gradle y sincronice el proyecto (**Gradle Sync**).
4. Verifica que la versión del JDK de Gradle esté configurada en Java 17 o Java 11:
   - Ve a `File` > `Settings` (o `Android Studio` > `Settings` en macOS).
   - Navega a `Build, Execution, Deployment` > `Build Tools` > `Gradle`.
   - En **Gradle JDK**, selecciona `Embedded JDK` o `JDK 17`.

---

### Paso 5: Ajustar la Dirección IP del Backend en la App Android

El archivo `RetrofitClient.java` gestiona la conexión HTTP entre la aplicación Android y el servidor backend.

Abre el archivo: `app/src/main/java/com/aula/virtual/data/RetrofitClient.java`

#### Opción A: Si usas el Emulador de Android Studio (AVD)
El emulador de Android accede a la PC local a través de la IP `10.0.2.2`:
```java
private static final String BASE_URL = "http://10.0.2.2:8000/";
```

#### Opción B: Si usas un Dispositivo Físico (Teléfono por USB / Wi-Fi)
1. Asegúrate de que tu teléfono y tu PC estén conectados a la **misma red Wi-Fi**.
2. Obtén la IP local de tu computadora:
   - **En Windows:** Abre la terminal y ejecuta `ipconfig` (busca la dirección IPv4, ej: `192.168.1.35`).
   - **En macOS/Linux:** Ejecuta `ifconfig` o `ip a`.
3. Reemplaza la dirección en `RetrofitClient.java`:
   ```java
   private static final String BASE_URL = "http://192.168.1.35:8000/"; // Reemplaza por la IP de tu PC
   ```

---

### Paso 6: Compilar y Ejecutar la Aplicación

1. En Android Studio, abre el **Device Manager** y crea o selecciona un dispositivo virtual (AVD) con versión de Android **10.0 (API level 29)** o superior.
2. Haz clic en el botón **Run 'app'** (triángulo verde ▶) o presiona `Shift + F10`.
3. Cuando la aplicación abra en el emulador o dispositivo, prueba iniciar sesión con los siguientes usuarios de prueba incluidos en `init.sql`:

#### 🔑 Credenciales de Prueba por Rol:

| Rol | Carnet | Contraseña |
| :--- | :--- | :--- |
| **Administrador** | `ADM001` | `admin` |
| **Docente / Profesor** | `DOC001` | `profesor` |
| **Estudiante** | `EST001` | `estudiante` |

---

## ❓ Solución de Problemas Comunes

1. **Error `Failed to connect to /10.0.2.2:8000` o `/192.168.x.x:8000`:**
   - Asegúrate de que el servidor FastAPI esté corriendo (`uvicorn main:app --host 0.0.0.0 --port 8000 --reload`).
   - Revisa el Firewall de Windows para permitir tráfico entrante en el puerto 8000.
   - Si estás en un dispositivo físico, confirma que la PC y el teléfono usen la misma Wi-Fi.

2. **Error al conectar con la base de datos en Python (`Can't connect to MySQL server`):**
   - Verifica que el servicio MySQL esté iniciado en el panel de XAMPP.
   - Revisa la cadena `DATABASE_URL` en `backend/main.py`.

3. **Error de sincronización de Gradle en Android Studio:**
   - En Android Studio, ve a `File` > `Invalidate Caches...` > marca las casillas y selecciona `Invalidate and Restart`.
   - Asegúrate de tener conexión a Internet activa para la descarga inicial de las librerías.

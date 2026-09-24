# 🎓 Aula Virtual - Aplicación Móvil & Backend API

Sistema integral de gestión para Aula Virtual compuesto por una **aplicación móvil Android** desarrollada en Java/Android Jetpack y un **servidor Backend API REST** desarrollado en Python con FastAPI y MySQL.

---

## 🚀 Inicio Rápido e Instalación en una nueva PC

Para instalar y ejecutar el proyecto desde cero en una computadora con una versión nueva de Android Studio, consulta nuestra guía detallada:

👉 **[Guía de Instalación Paso a Paso (MANUAL_DE_INSTALACION.md)](MANUAL_DE_INSTALACION.md)**

---

## 🛠️ Tecnologías Utilizadas

### 📱 Cliente Móvil (Android)
- **Lenguaje:** Java (Compatible con JDK 11/17)
- **SDK Objetivo:** Android 14 / 15 (API level 36) | **SDK Mínimo:** Android 10 (API level 29)
- **Componentes:**
  - Architecture Components (ViewModel, LiveData, ViewBinding)
  - Navigation Component (Fragments & NavGraph)
  - Retrofit 2 & Gson (Consumo de API REST)
  - Room Database (Persistencia de datos offline y caché)
  - Biometric Prompt & EncryptedSharedPreferences (Seguridad)

### ⚙️ Backend API
- **Lenguaje:** Python 3.10+
- **Framework:** FastAPI + Uvicorn
- **ORM:** SQLAlchemy
- **Base de Datos:** MySQL / MariaDB (vía XAMPP)

---

## 👥 Usuarios y Credenciales de Prueba

La base de datos inicial (`backend/init.sql`) incluye las siguientes cuentas predeterminadas:

| Rol | Carnet | Contraseña |
| :--- | :--- | :--- |
| **Administrador** | `ADM001` | `admin` |
| **Profesor / Docente** | `DOC001` | `profesor` |
| **Estudiante** | `EST001` | `estudiante` |

---

## 📂 Estructura del Proyecto

```text
MyApplication/
├── app/                      # Código fuente de la aplicación Android
│   ├── src/main/java/        # Lógica Java (UI, Data, Repositories, Database)
│   └── src/main/res/         # Recurso de diseño, layouts y temas
├── backend/                  # Servidor Backend API (Python + FastAPI)
│   ├── main.py               # Endpoints REST y modelos ORM
│   └── init.sql              # Script SQL para creación e inicialización de la DB
├── MANUAL_DE_INSTALACION.md  # Guía detallada de instalación y solución de problemas
└── README.md                 # Descripción general del proyecto
```

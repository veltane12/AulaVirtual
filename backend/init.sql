-- Script de inicialización para Aula Virtual
-- Este archivo define la estructura de la base de datos para el sistema de gestión académica.
-- Las descripciones de las columnas sirven para guiar a sistemas automatizados de generación de datos.

CREATE DATABASE IF NOT EXISTS aula_virtual;
USE aula_virtual;

-- Tabla de Colores Permitidos: Listado estricto de colores soportados por el tema de la aplicación móvil
CREATE TABLE IF NOT EXISTS allowed_colors (
    color_name VARCHAR(50) PRIMARY KEY -- Nombre identificador del color (Ej: 'BLUE_LIGHT')
);

-- Inserción de la paleta oficial de colores del sistema (Soportada en ThemeHelper)
INSERT IGNORE INTO allowed_colors (color_name) VALUES
('BLUE_LIGHT'), ('BLUE'), ('BLUE_DARK'),
('RED_LIGHT'), ('RED'), ('RED_DARK'),
('GREEN_LIGHT'), ('GREEN'), ('GREEN_DARK'),
('PURPLE_LIGHT'), ('PURPLE'), ('PURPLE_DARK'),
('CYAN_LIGHT'), ('CYAN'), ('CYAN_DARK'),
('YELLOW_LIGHT'), ('YELLOW'), ('YELLOW_DARK');

-- Tabla de Facultades: Entidades académicas principales (Ej: Facultad de Ingeniería)
CREATE TABLE IF NOT EXISTS faculties (
    id INT AUTO_INCREMENT PRIMARY KEY, -- Identificador único interno
    name VARCHAR(255) NOT NULL,        -- Nombre completo de la facultad (Ej: Ciencias Sociales)
    description TEXT                   -- Breve reseña o propósito de la facultad
);

-- Tabla de Usuarios: Almacena todos los roles (Administradores, Profesores, Alumnos)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    carnet VARCHAR(10) NOT NULL UNIQUE,         -- Código de identificación institucional (10 caracteres alfanuméricos)
    name VARCHAR(255) NOT NULL,                 -- Nombre completo y apellidos del usuario
    password VARCHAR(255) NOT NULL,             -- Contraseña de acceso (actualmente en texto plano)
    role VARCHAR(50) NOT NULL,                  -- Rol del usuario: 'ADMIN', 'STUDENT' o 'PROFESSOR'
    faculty VARCHAR(255),                       -- Nombre de la facultad a la que pertenece (relacionado con faculties.name)
    address TEXT,                               -- Dirección domiciliar del usuario
    personal_email VARCHAR(255),                -- Correo electrónico de contacto personal
    profile_image LONGTEXT,                     -- Foto de perfil en formato Base64 para visualización directa
    can_change_photo BOOLEAN DEFAULT 1          -- Flag: 1 permite cambiar foto, 0 bloquea por infracciones
);

-- Tabla de Materias: Cursos académicos ofrecidos
CREATE TABLE IF NOT EXISTS subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    name VARCHAR(255) NOT NULL,                 -- Nombre de la materia (Ej: Matemática II)
    description TEXT,                           -- Contenido o resumen de la materia
    faculty VARCHAR(255),                       -- Facultad que imparte la materia
    professorId INT,                            -- ID del usuario con rol 'PROFESSOR' asignado
    section VARCHAR(10) DEFAULT '01',           -- Sección de la materia (Ej: 01, 02)
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color representativo para la interfaz (Ej: 'RED', 'GREEN', 'PURPLE')
    FOREIGN KEY (professorId) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabla de Inscripciones y Notas: Relaciona alumnos con materias y sus calificaciones
CREATE TABLE IF NOT EXISTS enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    studentId INT NOT NULL,                     -- ID del usuario con rol 'STUDENT'
    subjectId INT NOT NULL,                     -- ID de la materia inscrita
    grade1 DOUBLE,                              -- Nota del primer periodo (Escala 0.0 - 10.0)
    grade2 DOUBLE,                              -- Nota del segundo periodo (Escala 0.0 - 10.0)
    grade3 DOUBLE,                              -- Nota del tercer periodo (Escala 0.0 - 10.0)
    grade4 DOUBLE,                              -- Nota del cuarto periodo (Escala 0.0 - 10.0)
    grade5 DOUBLE,                              -- Nota del quinto periodo (Escala 0.0 - 10.0)
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color preferido por el alumno para esta materia en su vista
    FOREIGN KEY (studentId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (studentId, subjectId)               -- Evita inscripciones duplicadas del mismo alumno en la misma materia
);

-- Tabla de Colores Personalizados: Preferencias estéticas de horario por usuario
CREATE TABLE IF NOT EXISTS user_subject_colors (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    userId INT NOT NULL,                        -- ID del usuario (Alumno o Profesor)
    subjectId INT NOT NULL,                     -- ID de la materia
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color CSS o nombre de color (Ej: 'YELLOW_DARK')
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (userId, subjectId)
);

-- Tabla de Blog de Materias: Publicaciones de tareas, avisos o exámenes
CREATE TABLE IF NOT EXISTS blog_entries (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    subjectId INT NOT NULL,                     -- ID de la materia dueña del blog
    category VARCHAR(50),                       -- Tipo de publicación: 'Aviso', 'Tarea', 'Parcial'
    title VARCHAR(255) NOT NULL,                -- Título descriptivo de la asignación
    content TEXT,                               -- Instrucciones detalladas o cuerpo del aviso
    position INT DEFAULT 0,                     -- Orden de visualización (Drag and Drop)
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Tabla de Comentarios del Blog: Discusión entre usuarios sobre una entrada
CREATE TABLE IF NOT EXISTS blog_comments (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    blogEntryId INT NOT NULL,                   -- ID de la entrada comentada
    userId INT NOT NULL,                        -- ID del usuario que comentó
    content TEXT NOT NULL,                      -- Contenido del mensaje
    timestamp VARCHAR(50),                      -- Fecha y hora del comentario
    FOREIGN KEY (blogEntryId) REFERENCES blog_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabla de Instalaciones: Lugares físicos dentro de la institución
CREATE TABLE IF NOT EXISTS facilities (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    name VARCHAR(255) NOT NULL,                 -- Nombre o código del aula/lugar (Ej: Aula BJ 205)
    type VARCHAR(100),                          -- Clasificación: 'Aula', 'Laboratorio', 'Auditorio', 'Edificio'
    description TEXT                            -- Detalles adicionales como ubicación o capacidad
);

-- Tabla de Horarios por Instalación: Programación semanal de clases
CREATE TABLE IF NOT EXISTS facility_schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    facilityId INT NOT NULL,                    -- ID del lugar físico
    subjectId INT NOT NULL,                     -- ID de la materia que se impartirá
    professorId INT NOT NULL,                   -- ID del profesor que impartirá la clase
    days VARCHAR(255),                          -- Días de la semana (Ej: "Lunes, Miercoles, Viernes")
    startTime VARCHAR(10),                      -- Hora de inicio en formato 24h o 12h con AM/PM (Ej: "08:00 AM")
    endTime VARCHAR(10),                        -- Hora de finalización (Ej: "10:00 AM")
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color para representar el bloque en el calendario
    FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (professorId) REFERENCES users(id) ON DELETE CASCADE
);

-- Triggers de Validación de Límites y Traslape de Horarios para evitar colisiones
DELIMITER //

CREATE TRIGGER before_insert_facility_schedule
BEFORE INSERT ON facility_schedules
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT;

    -- Convertir formatos de tiempo de texto (Ej: "08:00 AM") a minutos totales del día para comparaciones lineales precisas
    SET @new_start = TIME_TO_SEC(STR_TO_DATE(NEW.startTime, '%h:%i %p')) / 60;
    SET @new_end = TIME_TO_SEC(STR_TO_DATE(NEW.endTime, '%h:%i %p')) / 60;

    -- Validar que la hora de inicio sea anterior a la de finalización
    IF @new_start >= @new_end THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: La hora de inicio debe ser estrictamente menor a la hora de finalización.';
    END IF;

    -- 1. Validar traslape en la misma Aula/Instalación para los mismos días
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE facilityId = NEW.facilityId
      AND (
          -- Verificación básica de coincidencia de subcadena de días
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: Esta aula ya está ocupada en los días y rango de horario solicitados.';
    END IF;

    -- 2. Validar traslape para el mismo Profesor (No puede estar en dos aulas a la vez)
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE professorId = NEW.professorId
      AND (
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: El profesor ya tiene un horario asignado en este mismo intervalo.';
    END IF;
END//

CREATE TRIGGER before_update_facility_schedule
BEFORE UPDATE ON facility_schedules
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT;

    SET @new_start = TIME_TO_SEC(STR_TO_DATE(NEW.startTime, '%h:%i %p')) / 60;
    SET @new_end = TIME_TO_SEC(STR_TO_DATE(NEW.endTime, '%h:%i %p')) / 60;

    IF @new_start >= @new_end THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: La hora de inicio debe ser estrictamente menor a la hora de finalización.';
    END IF;

    -- 1. Validar traslape en la misma Aula (Excluyendo el propio registro que se actualiza)
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE facilityId = NEW.facilityId
      AND id <> NEW.id
      AND (
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: Esta aula ya está ocupada en los días y rango de horario solicitados.';
    END IF;

    -- 2. Validar traslape para el mismo Profesor
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE professorId = NEW.professorId
      AND id <> NEW.id
      AND (
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: El profesor ya tiene un horario asignado en este mismo intervalo.';
    END IF;
END//

-- Trigger para forzar e inyectar facultades automáticas e inmutables segun el rol asignado
CREATE TRIGGER before_insert_user_force_faculty
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    IF NEW.role = 'PROFESSOR' THEN
        SET NEW.faculty = 'Docencia';
    ELSEIF NEW.role = 'ADMIN' THEN
        SET NEW.faculty = 'Administrativa';
    END IF;
END//

CREATE TRIGGER before_update_user_force_faculty
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.role = 'PROFESSOR' THEN
        SET NEW.faculty = 'Docencia';
    ELSEIF NEW.role = 'ADMIN' THEN
        SET NEW.faculty = 'Administrativa';
    END IF;
END//

DELIMITER ;

-- Inserción de las Facultades obligatorias del sistema
INSERT IGNORE INTO faculties (id, name, description) VALUES
(998, 'Docencia', 'Facultad obligatoria asignada automáticamente a todos los Profesores.'),
(999, 'Administrativa', 'Facultad obligatoria asignada automáticamente a todos los Administradores.');

-- Inserción del Admin Maestro inicial
-- Datos de prueba base:
-- Carnet (User login): ADMIN12345
-- Password: ASD###
-- Rol: Administrador del sistema
INSERT IGNORE INTO users (carnet, name, password, role, faculty)
VALUES ('ADMIN12345', 'Admin Maestro', 'ASD###', 'ADMIN', 'Administrativa');

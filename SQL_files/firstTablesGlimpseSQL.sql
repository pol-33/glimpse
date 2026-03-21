/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  alumne
 * Created: 19 mar 2026
 */

-- Opcional: Borrar las tablas si ya existen (descomentar si es necesario)
-- DROP TABLE videos;
-- DROP TABLE users;

-- 1. Creación de la tabla USERS (Usuarios)
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,       -- nombre_usuario
    name VARCHAR(100) NOT NULL,             -- nombre
    surname VARCHAR(150) NOT NULL,          -- apellidos
    email VARCHAR(100) NOT NULL,            -- correo electrónico
    password VARCHAR(256) NOT NULL          -- contraseña
);

-- 2. Creación de la tabla VIDEOS (Vídeos)
CREATE TABLE videos (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,                 -- identificador
    title VARCHAR(100) NOT NULL,            -- título
    author VARCHAR(50) NOT NULL,           -- autor
    creation_date DATE,                     -- fecha de creación (ej. '2023-10-24')
    duration TIME,                          -- duración (ej. '00:32:00')
    views INTEGER DEFAULT 0,                -- reproducciones
    description VARCHAR(255),               -- descripción
    format VARCHAR(10),                     -- formato (ej. 'mp4', 'ogg')
    
    -- AMPLIACIÓN PROPUESTA
    file_path VARCHAR(512)                  -- ruta del fichero / URL
    CONSTRAINT fk_author FOREIGN KEY (author) REFERENCES users(username)
);

-- 3. (Opcional) Inserción de datos de prueba en inglés
INSERT INTO users (username, name, surname, email, password) 
VALUES ('admin', 'System', 'Administrator', 'admin@system.com', '12345');

INSERT INTO videos (id, title, author, creation_date, duration, views, description, format, file_path) 
VALUES (1, 'Big Buck Bunny', 'Peach Open Movie', '2014-10-14', '00:32:00', 150, 'Story of a giant bunny', 'mp4', 'http://example.com/bunny.mp4');
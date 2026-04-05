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
    id              INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    title           VARCHAR(100) NOT NULL,
    author          VARCHAR(50) NOT NULL,
    creation_date   DATE,
    duration        TIME,
    views           INTEGER DEFAULT 0,
    description     VARCHAR(255),
    format          VARCHAR(10),

    file_path       VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255),
    file_source     VARCHAR(10) NOT NULL,
    CONSTRAINT chk_file_source CHECK (file_source IN ('url', 'upload')),
    CONSTRAINT fk_author FOREIGN KEY (author) REFERENCES users(username)
);

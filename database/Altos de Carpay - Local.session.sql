-- Creación de la BD :)

CREATE DATABASE altos_de_carpay_DB;

-- Creación de tablas maestras
-- Extrañaba SQL Ojalá no deje la tendalada :)

CREATE TABLE Usuario (
    id_usuario SERIAL PRIMARY KEY,
    correo VARCHAR(100) UNIQUE NOT NULL,
    rut VARCHAR(12) UNIQUE NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) CHECK (rol IN ('Cliente', 'Admin')) NOT NULL,
    ultima_conexion TIMESTAMP NULL,
    estado VARCHAR(20) CHECK (estado IN ('Activo', 'Inactivo')) NOT NULL
);

CREATE TABLE Sector(
    id_sector SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    valor_hora DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) CHECK (
        estado in ('Disponible', 'Mantencion', 'Inactivo')) NOT NULL
);

CREATE TABLE Servicio (
    id_servicio SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    valor DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) CHECK(
        estado IN('Activo', 'Inactivo')) NOT NULL
);

CREATE TABLE Reserva (
    id_reserva SERIAL PRIMARY KEY,
    id_usuario INT REFERENCES Usuario(id_usuario),
    fecha DATE NOT NULL,
    horas_reservadas INT NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) CHECK (
        estado IN('Pendiente', 'En Proceso', 'Confirmado', 'Cancelado')) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- No deje la crema en el proceso Yay!
-- Creación de la estructura del sistema :)

CREATE TABLE Reserva_Sector (
    id_reserva INT REFERENCES Reserva(id_reserva) ON DELETE CASCADE,
    id_sector INT REFERENCES Sector(id_sector) ON DELETE CASCADE,
    horas INT NOT NULL,
    valor_hora DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id_reserva, id_sector)
);

CREATE TABLE Reserva_Servicio (
    id_reserva INT REFERENCES Reserva(id_reserva) ON DELETE CASCADE,
    id_servicio INT REFERENCES Servicio(id_servicio) ON DELETE CASCADE,
    cantidad INT NOT NULL DEFAULT 1,
    valor_unitario DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id_reserva, id_servicio)
);

CREATE TABLE Pago (
    id_pago SERIAL PRIMARY KEY,
    id_reserva INT REFERENCES Reserva(id_reserva),
    monto DECIMAL(10,2) NOT NULL,
    transaccion_id VARCHAR(100),
    estado_pago VARCHAR(9) CHECK (
        estado_pago IN ('Pendiente', 'Aprobado', 'Rechazado')) NOT NULL,
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Validacion_Acceso (
    id_validacion SERIAL PRIMARY KEY,
    id_reserva INT REFERENCES Reserva(id_reserva),
    token_qr VARCHAR(255) UNIQUE NOT NULL,
    tipo_registro VARCHAR(10) CHECK (
        tipo_registro IN('Entrada', 'Salida')) NOT NULL,
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    validado_por INT REFERENCES Usuario(id_usuario)
);

CREATE TABLE Logs_Modificacion (
    id_log SERIAL PRIMARY KEY,
    id_usuario INT REFERENCES Usuario(id_usuario),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tabla_modificada VARCHAR(50) NOT NULL,
    id_registro INT NOT NULL,
    valor_previo TEXT NULL,
    valor_nuevo TEXT NULL
);

CREATE TABLE Review (
    id_review SERIAL PRIMARY KEY,
    id_reserva INT REFERENCES Reserva(id_reserva),
    id_evaluador INT REFERENCES Usuario(id_usuario),
    id_evaluado INT REFERENCES Usuario(id_usuario),
    calificacion INT CHECK (calificacion BETWEEN 1 AND 5) NOT NULL,
    comentario VARCHAR(255) NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
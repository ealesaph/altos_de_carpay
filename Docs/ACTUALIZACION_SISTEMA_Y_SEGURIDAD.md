# Actualización Técnica, Seguridad y Cumplimiento de Requerimientos — Altos de Carpay

**Fecha de actualización:** 18 de noviembre de 2026  
**Alcance:** Base de Datos (PostgreSQL), Backend (Django REST Framework), Frontend Web (React + Vite + Nginx) y App Móvil (Android Kotlin + Jetpack Compose).

---

## 1. Funcionalidades Implementadas según la Documentación

### 1.1 Control de Acceso por QR, Check-in / Check-out y Sobretiempo (RF-03, RF-04, RF-05 / HU-03, HU-04)
* **Modelo `ValidacionAcceso` (`reservasApp/models.py`)**: Materializa en Django la tabla `validacion_acceso` del diseño relacional original (`id_validacion`, `reserva`, `token_qr`, `tipo_registro` [`Entrada`/`Salida`], `fecha_hora` del servidor, `validado_por`, `minutos_excedidos`, `cargo_sobretiempo_propuesto`).
* **Nuevo Estado `Finalizado` en `Reserva`**: Se incorporó el estado `Finalizado` en `Reserva.Estado` y se actualizó la máquina de estados en `reservasApp/services.py` (`Confirmado → En Proceso → Finalizado`).
* **Endpoint `POST /api/v1/reservas/{id}/validar-acceso/`**:
  * Al registrar **Entrada**: valida que la reserva esté `Confirmado`, cambia su estado a `En Proceso` y estampa la hora del servidor.
  * Al registrar **Salida**: valida que la reserva esté `En Proceso`, cambia su estado a `Finalizado`, compara la hora actual del servidor contra `reserva.fin` y calcula automáticamente los minutos excedidos y el cargo extra sugerido.
* **App Móvil (Android)**:
  * **Generación de QR real (`QrCodeGenerator.kt` + `UserReservaDetailScreen.kt`)**: Genera códigos QR reales con ZXing a partir del `token_qr` almacenado en PostgreSQL.
  * **Escáner QR real (`AdminScannerScreen.kt`)**: Solicita permiso `android.permission.CAMERA` en tiempo de ejecución y utiliza **CameraX + ML Kit Barcode Scanning** para leer códigos QR en vivo y validar el acceso en la base de datos.

### 1.2 Bitácora Digital de Incumplimientos en Modo Solo Lectura (RF-07 / HU-06, HU-07 / Ley N° 19.799)
* **Modelo `BitacoraIncidencia` (`reservasApp/models.py`)**: Registra `id_incidencia`, `reserva`, `autor`, `titulo`, `descripcion` y `fecha_creacion` del servidor.
* **Inmutabilidad y Solo Lectura Post-Evento**:
  * El modelo prohíbe editar (`pk is not None`) o eliminar (`delete()`) registros existentes.
  * Cuando una reserva pasa a estado `Finalizado`, la bitácora queda bloqueada automáticamente en modo **Solo Lectura** tanto en el backend (`BitacoraIncidenciaSerializer` y `BitacoraIncidencia.save()`) como en la interfaz móvil (`AdminBitacoraScreen.kt`), resguardando su valor probatorio conforme a la Ley N° 19.799.
* **Endpoint `/api/v1/incidencias/`**: Permite listar por reserva (`?reserva=<id>`) y crear nuevas observaciones internas (exclusivo para rol `Admin`).

### 1.3 Consultas y Exportación a Excel desde Frontend Web (RF-08, RF-09 / HU-08)
* **Frontend Web (`frontend/src/api.js` y `frontend/src/pages/AdminDashboard.jsx`)**:
  * Se conectó la descarga directa de reportes CSV compatibles con Excel (`GET /api/v1/reservas/exportar/`), manteniendo la protección contra CSV Injection del backend.
  * Se conectó el panel de **Consultas** en el Dashboard Administrativo (`GET /api/v1/consultas/` y `POST /api/v1/consultas/{id}/responder/`) sin alterar el diseño visual ni los estilos de la interfaz.

### 1.4 Cumplimiento de Derechos ARCO (Ley N° 21.719)
* **Endpoint `GET /api/v1/auth/mis-datos/`**: Entrega al titular autenticado una copia estructurada de sus datos personales, fecha de consentimiento y conteo de registros asociados (Derecho de Acceso).
* **Endpoint `POST /api/v1/auth/desactivar-cuenta/`**: Permite al cliente desactivar su cuenta e invalidar todos sus tokens JWT activos (Derecho de Cancelación/Oposición), preservando la integridad referencial histórica.

---

## 2. Auditoría de Seguridad y Correcciones Aplicadas

| Componente | Hallazgo | Medida Aplicada / Recomendación |
| :--- | :--- | :--- |
| **Backend (`settings.py`)** | Contraseña de PostgreSQL por defecto escrita como fallback en `DATABASES`. | Se eliminó la contraseña hardcodeada; ahora se exige vía variables de entorno (`DB_PASSWORD` / `POSTGRES_PASSWORD`). |
| **Auditoría (`auditoriaApp`)** | `ValidacionAcceso` y `BitacoraIncidencia` no estaban suscritos a señales de auditoría. | Se añadieron ambos modelos a `MODELOS_AUDITADOS` en `auditoriaApp/signals.py`. |
| **Permisos y Aislamiento** | Validación de acceso por roles (RBAC). | Todos los endpoints de validación QR, incidencias, exportación y administración de sectores/servicios exigen `EsAdministrador`. Los clientes solo acceden a sus propios recursos. |
| **App Android (`AndroidManifest.xml`)** | `usesCleartextTraffic="true"` habilitado para pruebas HTTP locales (`10.0.2.2:8000`). | **Pendiente para Producción:** desactivar tráfico en texto claro al migrar a dominio con certificado SSL/HTTPS, y migrar `TokenManager` a `EncryptedSharedPreferences`. |
| **Despliegue (`.env` / `docker-compose.yml`)** | `DJANGO_DEBUG=True` y `DJANGO_ALLOWED_HOSTS=*` para entorno local. | **Pendiente para Producción:** establecer `DJANGO_DEBUG=False`, restringir `DJANGO_ALLOWED_HOSTS` al dominio oficial, configurar HTTPS en Nginx y automatizar copias de seguridad de PostgreSQL + cron de `cancelar_reservas_impagas`. |

---

## 3. Estado Final de Requerimientos (Matriz de Cumplimiento)

| ID | Requerimiento | Estado |
| :--- | :--- | :--- |
| **RF-01** | Gestión de usuarios (Registro, Login JWT, Perfil, ARCO) | **Completado** |
| **RF-02** | Gestión de eventos / reservas con cálculo de anticipo (30%) | **Completado** |
| **RF-03** | Registro de ingreso (Check-in por QR en App + `ValidacionAcceso`) | **Completado** |
| **RF-04** | Registro de salida (Check-out por QR en App + estado `Finalizado`) | **Completado** |
| **RF-05** | Control de horarios y detección automática de sobretiempo | **Completado** |
| **RF-06** | Gestión de áreas / sectores (Estado global y desglose por reserva) | **Completado** |
| **RF-07** | Bitácora digital de incidencias con bloqueo Solo Lectura | **Completado** |
| **RF-08** | Consulta de historial (Reservas, Auditoría, Consultas, Incidencias) | **Completado** |
| **RF-09** | Exportación de información a Excel/CSV | **Completado** (Backend + Web) |
| **RF-10** | Página web informativa y catálogo público | **Completado** |

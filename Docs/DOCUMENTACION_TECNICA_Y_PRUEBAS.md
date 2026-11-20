# Documentación Técnica, Integración SMTP y Pruebas E2E — Altos de Carpay

**Fecha:** 19 de noviembre de 2026  
**Alcance:** Revisión de conexiones del sistema, desarrollo del módulo de envío de correos de reserva, configuración SMTP con Brevo y ejecución de pruebas integrales (End-to-End).

---

## 1. Arquitectura y Conexiones Verificadas

El sistema opera sobre tres contenedores orquestados en `docker-compose.yml` bajo la red `carpay_network`:

| Servicio | Contenedor | Tecnología | Puerto / Conexión |
| :--- | :--- | :--- | :--- |
| Base de Datos | `carpay_postgres` | PostgreSQL 16 Alpine | `5432` (interno `db:5432`) |
| Backend API | `carpay_backend` | Django 5.2 + DRF + SimpleJWT | `8000` (`http://localhost:8000/api/v1/`) |
| Frontend Web | `carpay_frontend` | React + Vite + Nginx | `80` (`http://localhost/`, proxy `/api/` y `/admin/` hacia `backend:8000`) |
| App Móvil | Android Kotlin | Jetpack Compose + Retrofit | Configurable en `ApiClient.kt` (`10.0.2.2:8000` emulador / IP local dispositivo físico) |

### Endpoints principales conectados
* **Autenticación y ARCO (`/api/v1/auth/`):** `login/`, `registro/`, `refresh/`, `logout/`, `perfil/`, `cambiar-password/`, `mis-datos/`, `desactivar-cuenta/`.
* **Catálogo (`/api/v1/`):** `sectores/`, `servicios/`.
* **Reservas y Operaciones (`/api/v1/reservas/`):** CRUD de reservas, `{id}/pagos/`, `{id}/cancelar/`, `{id}/estado/`, `{id}/validar-acceso/`, `{id}/enviar-correo/`, `exportar/`.
* **Soporte y Control (`/api/v1/`):** `consultas/`, `consultas/{id}/responder/`, `incidencias/`, `auditoria/logs/`.

---

## 2. Problemas Detectados y Soluciones Aplicadas

### 2.1 Envío de correo con detalles de la reserva
* **Situación inicial:**
  * `backend/altosDeCarpay/.env` no tenía variables `EMAIL_*`, por lo que Django usaba `console.EmailBackend` (solo imprimía en consola).
  * No existía `backend/altosDeCarpay/.env.example` (referenciado en `README.md`).
  * `reservasApp/services.py` solo enviaba un `send_mail` en texto plano básico al confirmar el anticipo, sin HTML, sin código QR, sin historial de pagos, sin notificación en pagos de saldo y sin endpoint de reenvío.
* **Solución implementada:**
  * **Módulo dedicado (`reservasApp/emails.py`):** Implementación de `enviar_correo_reserva(reserva_id, motivo=..., fail_silently=...)` usando `EmailMultiAlternatives` (texto plano + HTML).
  * **Plantillas (`reservasApp/templates/reservasApp/emails/detalle_reserva.html` y `.txt`):** Incluyen datos del titular, N° y estado de reserva, fecha, horario de inicio y término, código de acceso (`token_qr`), desglose de sectores y servicios extra, total, monto pagado, saldo pendiente, pagos aprobados y política de cancelación.
  * **Disparo automático (`reservasApp/services.py`):** Se envía correo al confirmar el anticipo (`motivo="confirmacion"`) y al registrar pagos adicionales aprobados (`motivo="pago"`).
  * **Reenvío manual (`reservasApp/views.py`):** Nuevo endpoint `POST /api/v1/reservas/{id}/enviar-correo/` accesible para el cliente titular o administrador.
  * **Integración Frontend (`frontend/src/api.js` y `frontend/src/pages/Confirmation.jsx`):** Función `sendReservationEmail(id)` y botón **"Reenviar detalles al correo"** en la vista de confirmación con indicador de estado.
  * **Pruebas unitarias (`reservasApp/tests.py`):** Cobertura para generación de correo HTML/TXT y permisos del endpoint `enviar-correo`.

### 2.2 Configuración SMTP (Google Workspace → Brevo)
* **Incidencias detectadas durante la configuración:**
  1. `DEFAULT_FROM_EMAIL` estaba inicialmente en `@altosdecarpay.cl` mientras el usuario SMTP era `@altosdecarpay.com` (riesgo de rechazo `550 Sender verify failed`).
  2. La cuenta `reservas@altosdecarpay.com` en Google Workspace tenía deshabilitada la creación de **contraseñas de aplicación** a nivel de organización, impidiendo autenticar por SMTP directo de Gmail con 2FA activo.
* **Solución aplicada:**
  * Se habilitó `EMAIL_USE_TLS` y `EMAIL_USE_SSL` configurables por entorno en `altosDeCarpay/settings.py`.
  * Se configuró **Brevo SMTP** (`smtp-relay.brevo.com:587`, `EMAIL_USE_TLS=True`) en `backend/altosDeCarpay/.env` con `DEFAULT_FROM_EMAIL=reservas@altosdecarpay.com` y se verificó conectividad TCP al puerto `587`.
  * Se creó `backend/altosDeCarpay/.env.example` sin credenciales reales.

### 2.3 Panel Administrativo Interactivo, Gestión de Imágenes y Jerarquía de Usuarios
* **Panel Administrativo Interactivo (`frontend/src/pages/AdminDashboard.jsx` y `frontend/src/api.js`):**
  * Se conectó la barra lateral (`Vista general`, `Reservas`, `Catálogo y precios`, `Consultas`, `Usuarios y personal`, `Configuración`) y la barra móvil al contenido dinámico.
  * **Quick Menu (Tarjetas KPI):** clic en *Reservas*, *Saldo por cobrar*, *Consultas pendientes* o *Espacios publicados* navega directamente a la sección y filtro correspondiente.
  * **Reservas interactivas:** filtros funcionales (`Todas`, `Con saldo`, `Pendiente`, `Confirmado`), buscador y filas desplegables para registrar pagos manuales (Transferencia/Efectivo), cambiar estado (`Confirmado` → `En Proceso` → `Finalizado`), reenviar correo o cancelar.
  * **Catálogo y precios inline:** edición directa de nombre, tarifa por hora, estado (`Disponible`, `Mantencion`, `Inactivo`), descripción e imagen al hacer clic en cada espacio.
  * **Consultas interactivas:** filtros por estado y respuesta directa con cierre inmediato (`Respondida`).
* **Gestión de Imágenes por Archivo y URL (`reservasApp` + `frontend`):**
  * Se habilitó `MEDIA_URL = '/media/'` y `MEDIA_ROOT` en `settings.py`, servicio de medios en `altosDeCarpay/urls.py` y proxy `/media/` en Nginx (`frontend/Dockerfile`) y Vite (`vite.config.js`).
  * Nuevo endpoint `POST /api/v1/sectores/{id}/subir-imagen/` (JPG/PNG/WEBP, máx. 5 MB) y soporte de rutas `/media/` en `SectorSerializer`.
  * Se creó la ilustración por defecto de hojas naturales verdes (`frontend/src/assets/default-space.svg`) usada automáticamente en `Home.jsx` y `AdminDashboard.jsx` cuando un espacio no tiene imagen o se elimina la existente.
* **Jerarquía Superusuario vs Personal Administrador (`usuariosApp` + `altosDeCarpayApp/permissions.py`):**
  * Se agregó el permiso `EsSuperusuario` (`is_superuser=True`) y el serializer `CrearPersonalAdminSerializer`.
  * `POST /api/v1/usuarios/` (exclusivo para el superusuario) permite crear cuentas de **Personal Administrador** (`rol='Admin'`, `is_superuser=False`) que operan reservas, catálogo y consultas, pero sin atribuciones para crear ni modificar otros administradores.
  * El superusuario principal queda protegido frente a degradación de rol o desactivación.
  * Nueva pestaña **Usuarios y personal** en `AdminDashboard.jsx` con creación de personal, búsqueda, filtro por rol y asignación/activación de cuentas.

### 2.4 Landing Page Pública, Consultas y Gestión de Cuenta Propia
* **Formulario de Consultas en Landing (`frontend/src/pages/Home.jsx`):**
  * Conectado a `POST /api/v1/consultas/` (`createConsulta`), limpiando el formulario al enviar y mostrando confirmación explícita (o aviso con enlace a `/login` si el visitante no tiene sesión de cliente).
* **Navegación Amigable en Landing (`Home.jsx`):**
  * La portada nunca exige login. Sin sesión muestra botones claros de **Iniciar sesión** (`/login`) y **Registrarse** (`/registro`); con sesión activa muestra **Mi cuenta** (`/mi-cuenta`), acceso directo a **Panel Admin** (`/admin`) o **Mis reservas** (`/mis-reservas`) según rol, y **Salir**.
* **Nueva Vista "Mi cuenta" (`frontend/src/pages/Account.jsx` en `/mi-cuenta`):**
  * Permite a cualquier usuario autenticado modificar sus datos personales (`PATCH /api/v1/auth/perfil/`) y cambiar su contraseña (`POST /api/v1/auth/cambiar-password/`).
  * Incluye botón **Volver al Panel Admin** para administradores y **Mis reservas** para clientes, además de **Cerrar sesión**.
  * La opción **Cerrar mi cuenta** (desactivación ARCO) se muestra solo a clientes y queda oculta para administradores/superusuario en coherencia con el backend.

---

## 3. Registro de Pruebas Exhaustivas (End-to-End)

Pruebas ejecutadas directamente contra el despliegue Docker en ejecución (`http://localhost/api/v1/` y `http://localhost/`):

| # | Flujo / Caso de Prueba | Endpoint / Acción | Resultado Observado | Estado |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Disponibilidad Frontend SPA | `GET http://localhost/` | HTTP `200` | Aprobado |
| 2 | Catálogo público vía proxy Nginx | `GET /api/v1/sectores/` y `/servicios/` | `5` sectores y `6` servicios (`200`) | Aprobado |
| 3 | Registro de usuario con RUT y consentimiento Ley N° 21.719 | `POST /api/v1/auth/registro/` | Usuario creado (`e2e_231802@altosdecarpay.cl`) | Aprobado |
| 4 | Control de fuerza bruta (Rate Limiting) | Múltiples `POST /api/v1/auth/login/` en < 1 min | HTTP `429` (`throttled`) | Aprobado |
| 5 | Login JWT, Perfil y Derechos ARCO | `POST /auth/login/`, `GET /auth/perfil/`, `GET /auth/mis-datos/` | Tokens válidos, perfil y reporte ARCO (`200`) | Aprobado |
| 6 | Creación de reserva con sector y servicio | `POST /api/v1/reservas/` | Reserva ID `2`, estado `Pendiente`, Total `76500.00`, Anticipo `22950.00`, `token_qr` generado | Aprobado |
| 7 | Pago de anticipo (simulador Webpay) | `POST /api/v1/reservas/2/pagos/` | Pago ID `3` `Aprobado`, reserva pasó a `Confirmado` | Aprobado |
| 8 | Envío de correo real vía Brevo SMTP | `POST /api/v1/reservas/2/enviar-correo/` | HTTP `200` (`Correo con los detalles de la reserva enviado...`) | Aprobado |
| 9 | Aislamiento entre clientes | Cliente B consulta reserva ID `2` de Cliente A | HTTP `404` | Aprobado |
| 10 | Control de permisos RBAC | Cliente consulta `/reservas/exportar/` y `/auditoria/logs/` | HTTP `403` | Aprobado |
| 11 | Prevención de doble reserva (solapamiento horario) | Segundo `POST /api/v1/reservas/` mismo sector/fecha/hora | HTTP `409` (Conflicto) | Aprobado |
| 12 | Cancelación por el cliente | `POST /api/v1/reservas/3/cancelar/` | Reserva pasó a `Cancelado` | Aprobado |
| 13 | Consultas y respuesta administrativa | `POST /consultas/` y `POST /consultas/1/responder/` | Estado pasó de `Pendiente` a `Respondida` | Aprobado |
| 14 | Check-in QR (`Entrada`) | `POST /reservas/2/validar-acceso/` (`Entrada`) | Estado pasó a `En Proceso` | Aprobado |
| 15 | Bitácora de incidencias durante evento | `POST /api/v1/incidencias/` | Incidencia ID `1` registrada | Aprobado |
| 16 | Check-out QR (`Salida`) | `POST /reservas/2/validar-acceso/` (`Salida`) | Estado pasó a `Finalizado` (`0` min excedidos) | Aprobado |
| 17 | Bloqueo Solo Lectura de Bitácora post-evento | `POST /api/v1/incidencias/` con reserva `Finalizado` | Rechazado con HTTP `400` | Aprobado |
| 18 | Auditoría y Exportación CSV | `GET /auditoria/logs/` y `GET /reservas/exportar/` | `25` logs y descarga CSV HTTP `200` (`302` bytes) | Aprobado |
| 19 | Derecho de cancelación ARCO | `POST /api/v1/auth/desactivar-cuenta/` | Cuenta desactivada y refresh token invalidado | Aprobado |

---

## 4. Pendientes para Despliegue en Producción

1. Configurar `DJANGO_DEBUG=False`, `DJANGO_ALLOWED_HOSTS` con el dominio productivo y certificado SSL/HTTPS en Nginx.
2. En la app Android, desactivar `usesCleartextTraffic="true"` al pasar a HTTPS y migrar `TokenManager` a `EncryptedSharedPreferences`.
3. Programar tarea periódica (cron) para ejecutar `python manage.py cancelar_reservas_impagas` y respaldos automáticos de PostgreSQL.
4. En Brevo, completar la autenticación del dominio `altosdecarpay.com` (registros DKIM/SPF en DNS) para máxima entregabilidad en bandeja principal.

---

## 5. Guía de Traspaso (Handover) para el Siguiente Equipo

1. **Levantar el sistema completo en local:**
   ```powershell
   docker compose up -d --build
   ```
   Acceder por `http://localhost/` (sin `https` en entorno local).
2. **Variables de entorno y secretos:**
   * `backend/altosDeCarpay/.env` está excluido de Git (`.gitignore`). El nuevo equipo debe copiar `backend/altosDeCarpay/.env.example` a `.env` y configurar las credenciales definitivas de PostgreSQL, `DJANGO_SECRET_KEY` y el servidor SMTP del cliente.
3. **Cuentas iniciales creadas por `seed_data.py`:**
   * **Superusuario:** `admin@altosdecarpay.cl` / `CarpayAdmin2026!` (único habilitado para crear/gestionar personal administrador desde **Usuarios y personal**).
   * **Cliente de prueba:** `camila.rojas@gmail.com` / `ClientePass2026!`.

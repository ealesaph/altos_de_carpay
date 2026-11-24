# Documentación Técnica, Integración SMTP y Pruebas E2E — Altos de Carpay

**Fecha:** 19 de noviembre de 2026 (actualizado el 23 de noviembre de 2026)  
**Alcance:** Revisión de conexiones del sistema, desarrollo del módulo de envío de correos de reserva, configuración SMTP con Brevo y ejecución de pruebas integrales (End-to-End).

---

## 1. Arquitectura y Conexiones Verificadas

El sistema opera sobre tres contenedores orquestados en `docker-compose.yml` bajo la red `carpay_network`:

| Servicio | Contenedor | Tecnología | Puerto / Conexión |
| :--- | :--- | :--- | :--- |
| Base de Datos | `carpay_postgres` | PostgreSQL 16 Alpine | `5432` (interno `db:5432`) |
| Backend API | `carpay_backend` | Django 5.2 + DRF + SimpleJWT | `8000` (`http://localhost:8000/api/v1/`) |
| Frontend Web | `carpay_frontend` | React + Vite + Nginx | `80` (`http://localhost/`, proxy `/api/` y `/admin/` hacia `backend:8000`; el panel de la web está en `/panel`) |
| App Móvil | Android Kotlin | Jetpack Compose + Retrofit | Configurable en `ApiClient.kt` (`10.0.2.2:8000` emulador / IP local dispositivo físico) |

### Endpoints principales conectados
* **Autenticación y ARCO (`/api/v1/auth/`):** `login/`, `registro/`, `refresh/`, `logout/`, `perfil/`, `cambiar-password/`, `mis-datos/`, `desactivar-cuenta/`.
* **Catálogo (`/api/v1/`):** `sectores/`, `servicios/`.
* **Reservas y Operaciones (`/api/v1/reservas/`):** CRUD de reservas, `{id}/pagos/`, `{id}/cancelar/` (con `origen` Cliente/Recinto), `{id}/simular-cancelacion/`, `{id}/registrar-devolucion/`, `{id}/modificar/`, `{id}/estado/`, `{id}/validar-acceso/`, `{id}/enviar-correo/`, `exportar/`, `resumen/` y `?vista=proximas`.
* **Soporte y Control (`/api/v1/`):** `consultas/` (también sin cuenta), `consultas/{id}/responder/` (envía la respuesta por correo), `incidencias/`, `auditoria/logs/`.

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
  * Conectado a `POST /api/v1/consultas/` (`createConsulta`), limpiando el formulario al enviar y mostrando confirmación explícita. Desde el 23-11-2026 no exige sesión: un visitante indica nombre, correo, teléfono opcional y acepta la política de privacidad.
* **Navegación Amigable en Landing (`Home.jsx`):**
  * La portada nunca exige login. Sin sesión muestra botones claros de **Iniciar sesión** (`/login`) y **Registrarse** (`/registro`); con sesión activa muestra **Mi cuenta** (`/mi-cuenta`), acceso directo a **Panel Admin** (`/panel`) o **Mis reservas** (`/mis-reservas`) según rol, y **Salir**.
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

### 3.1 Pruebas de las correcciones solicitadas por el cliente (23-11-2026)

Pruebas automáticas en `reservasApp/tests_observaciones.py`, ejecutadas en el contenedor `carpay_backend` (`python manage.py test`: 35 de 35 aprobadas), más revisión en el navegador sobre `http://localhost/`:

| # | Observación / Hallazgo | Caso de prueba | Resultado esperado | Estado |
| :--- | :--- | :--- | :--- | :--- |
| 20 | Obs. 5 / B4 | Reservar 13:00–18:00 y 19:00–00:00; luego 22:00 + 6 h, 16:00 + 3 h, 10:00 y 13:30 | `201` en los bloques; `400` fuera de ellos | Aprobado |
| 21 | Obs. 6 / M5 | `POST /reservas/{id}/modificar/` cambia hora y horas | Misma reserva, nuevo total, sin reserva duplicada | Aprobado |
| 22 | Obs. 8 / B3 | Crear reserva a las 19:00 | `plazo_saldo_vence` = inicio − 24 h | Aprobado |
| 23 | Obs. 2 / M9 | Consulta sin cuenta, sin datos y con datos completos | `400` sin datos; `201` y correo al responder | Aprobado |
| 24 | Obs. 9 / M8 | Cliente consulta, admin responde | El cliente ve la respuesta y recibe correo | Aprobado |
| 25 | Obs. 11 / M1 | `GET /reservas/resumen/` con una reserva cancelada | Saldo solo de vigentes y cobrado del mes | Aprobado |
| 26 | Obs. 12 / M1 | `GET /reservas/?vista=proximas` | Solo vigentes desde hoy, en orden ascendente | Aprobado |
| 27 | Obs. 13 / M2 | `cerrar_reservas_vencidas()` | Confirmada vencida → `No se presentó`; en proceso → `Finalizado` | Aprobado |
| 28 | Obs. 15 / A5 | Cancelar con 20 días (cliente), simular con 3 días, cancelar por el recinto | Devolución $70.000, $0 (sin cambiar estado) y $100.000 | Aprobado |
| 29 | Obs. 16 / A3 | Entrada de una reserva a 12 días y de otra en 30 minutos | `409` y `201` | Aprobado |
| 30 | A1 | 5 logins fallidos con IP distinta cada vez y luego la clave correcta | `429` «Cuenta bloqueada temporalmente» | Aprobado |
| 31 | A2 | Recargar `http://localhost/panel` | Abre el panel de la web | Aprobado |
| 32 | Obs. 3 / B9 | Portada en 375 px | Botón de menú con todas las secciones | Aprobado |

### 3.2 Pruebas y correcciones de la app Android (23-11-2026)

Se recorrió la app en el emulador (Android 17) con los perfiles cliente y administrador. Se encontraron 23 problemas (APP-A1 a A6, M1 a M10 y B1 a B7) y en la segunda prueba aparecieron 3 más (APP-R1 a R3). Todos quedaron corregidos. El detalle de cada problema y su solución está en el documento «Pruebas exhaustivas de la app Android – Altos de Carpay», en el capítulo «Correcciones aplicadas». Resumen:

- **Datos y sesión:** se eliminaron los datos de ejemplo del prototipo y las reservas fijas (`RES-0261`, `RES-0248`). El inicio de sesión espera la respuesta real del servidor. Si la sesión vence, la app vuelve al login con un aviso (`SessionState.sesionExpirada`).
- **Acciones con confirmación:** el escáner primero busca la reserva y luego pide confirmar el ingreso o la salida. Lo mismo ocurre con el estado de los espacios, las respuestas a consultas y las acciones del detalle de reserva (estado, pago manual, cancelación con devolución simulada y registro de la devolución).
- **Funciones completadas:** la solicitud de servicios extra se envía como consulta «RES-XXXX: …». Se agregaron notificaciones, el perfil con los derechos de la Ley N° 21.719, los filtros por estado del servidor y el estado «No se presentó».
- **Formato:** se usa la función común `ui/utils/Formato.kt` para montos CLP, fechas en hora de Chile, RUT, teléfono, estados y mensajes de error sin JSON.
- **Web:** si la sesión vence, la web vuelve a `/login?sesion=expirada` (`api.js`). La web usa el mismo código de reserva que la app y el QR (`codigoReserva`, por ejemplo `RES-0008`). No hubo cambios en el backend ni en la API.

| # | Prueba | Resultado esperado | Estado |
| :--- | :--- | :--- | :--- |
| 33 | `gradlew :app:testDebugUnitTest` | 20 de 20 pruebas, incluida `FormatoTest` | Aprobado |
| 34 | `python manage.py test` después de los cambios | 35 de 35 | Aprobado |
| 35 | Consulta enviada desde el detalle de RES-0009 en la app | Aparece en el panel web como «RES-0009: …», pendiente | Aprobado |
| 36 | Consulta sin cuenta creada en la web | Se ve en Tickets de la app con el correo de respuesta | Aprobado |
| 37 | Ingreso por código manual fuera de plazo | Muestra la reserva, pide confirmar y luego muestra el mensaje del servidor sin JSON | Aprobado |
| 38 | Sesión web con tokens inválidos | Redirige a `/login?sesion=expirada` | Aprobado |

## 4. Pendientes para Despliegue en Producción

1. Configurar `DJANGO_DEBUG=False`, `DJANGO_ALLOWED_HOSTS` con el dominio productivo y certificado SSL/HTTPS en Nginx.
2. En la app Android, desactivar `usesCleartextTraffic="true"` al pasar a HTTPS y migrar `TokenManager` a `EncryptedSharedPreferences`.
3. Programar respaldos automáticos de PostgreSQL. (La liberación de reservas impagas y el cierre de reservas vencidas ya corren cada 15 minutos con `tareas_programadas`, iniciada por `entrypoint.sh`.)
4. En Brevo, completar la autenticación del dominio `altosdecarpay.com` (registros DKIM/SPF en DNS) para máxima entregabilidad en bandeja principal.
5. Integrar Webpay real con credenciales de comercio de Transbank y agregar cabeceras de seguridad (CSP) en Nginx.
6. Completar la página `/condiciones` con el texto del contrato de arriendo del cliente.

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
4. **App Android en emulador y celular a la vez:**
   * El emulador usa `10.0.2.2:8000` sin configurar nada.
   * Un celular por USB usa `127.0.0.1:8000`. Con el celular conectado y la depuración USB activada, ejecutar `powershell -ep bypass -f .\app\AltosDeCarpay\conectar-celular.ps1`, que aplica `adb reverse` a los puertos 8000 (API) y 80 (web) e instala la app.
   * Por Wi-Fi: en la pantalla de inicio de sesión, tocar «Servidor» y escribir la IP local del PC (por ejemplo `192.168.1.20`). Hay que permitir el puerto 8000 en el firewall de Windows.

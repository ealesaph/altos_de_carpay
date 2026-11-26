# Altos de Carpay

Aplicación web React/Vite conectada a una API Django REST y PostgreSQL, orquestada completamente mediante Docker Compose. El flujo de cliente usa el catálogo y los precios del servidor para registrar reservas, simular pagos y consultar reservas; el panel administrativo permite revisar reservas, gestionar tarifas, responder consultas y exportar reportes CSV y Excel.

---

## Despliegue Rápido con Docker (Recomendado)

1. Copia `backend/altosDeCarpay/.env.example` a `backend/altosDeCarpay/.env` y completa los valores. `docker-compose.yml` lee de ese archivo las credenciales de PostgreSQL y del backend (no tiene contraseñas escritas). Define `FIELD_ENCRYPTION_KEY`, la clave con que se cifran en la base el RUT y el teléfono (genérala con `python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"` y respáldala: si se pierde, esos datos no se pueden leer). No subas archivos `.env` a Git.
2. Asegúrate de tener **Docker Desktop** abierto y ejecutándose.
3. Levanta los 3 servicios (`carpay_postgres`, `carpay_backend`, `carpay_frontend`) con un solo comando:

   ```powershell
   docker compose up -d --build
   ```

4. El contenedor `carpay_backend` espera automáticamente a que PostgreSQL esté saludable (`healthy`) y aplica `migrate`.
   * Con `DJANGO_DEBUG=true` (desarrollo) además ejecuta `makemigrations`, carga los datos semilla (`seed_data.py`) y usa el servidor de desarrollo de Django.
   * Con `DJANGO_DEBUG=false` (producción) ejecuta `collectstatic` y levanta **Gunicorn**; los datos semilla solo se cargan si `CARGAR_DATOS_SEMILLA=true`, y se exigen `DJANGO_SECRET_KEY` y `FIELD_ENCRYPTION_KEY`.
   * PostgreSQL solo publica el puerto 5432 en `127.0.0.1` (no queda expuesto a la red).
   * En ambos modos el contenedor ejecuta `python manage.py tareas_programadas` cada 15 minutos (`INTERVALO_TAREAS_SEGUNDOS`, por defecto `900`): libera las reservas sin anticipo pagado a tiempo y cierra las reservas vencidas (estado «No se presentó» o «Finalizado»). No hace falta configurar un cron aparte.
   * Nginx reemplaza `X-Forwarded-For` con la IP real del visitante y `docker-compose.yml` fija `NUM_PROXIES=1`, para que el límite de intentos de inicio de sesión no se pueda saltar falseando la IP.

### Accesos y Credenciales de Prueba (Seed Automático)

* **Frontend Web (Nginx + React):** `http://localhost/`
* **Frontend Web con HTTPS:** `https://localhost/` (puerto 443, certificado autofirmado generado al construir la imagen; el navegador muestra una advertencia la primera vez. En producción se reemplaza por un certificado de una autoridad reconocida montado en `/etc/nginx/certs`).
* **Backend API REST:** `http://localhost:8000/api/v1/` (también proxy en `http://localhost/api/v1/`)
* **Panel administrativo de la web:** `http://localhost/panel` (antes `/admin`; esa ruta ahora redirige al panel)
* **Django Admin (uso interno):** `http://localhost/admin/`
* **Condiciones de reserva públicas:** `http://localhost/condiciones`
* **Emulador Android Studio:** `http://10.0.2.2:8000/api/v1/`

Cuentas iniciales creadas automáticamente en PostgreSQL:

| Rol | Correo | Contraseña |
| :--- | :--- | :--- |
| **Administrador** | `admin@altosdecarpay.cl` | `CarpayAdmin2026!` |
| **Cliente** | `camila.rojas@gmail.com` | `ClientePass2026!` |

---

## Ejecución Local sin Docker (Windows)

### Backend y PostgreSQL

1. Copia `backend\altosDeCarpay\.env.example` a `backend\altosDeCarpay\.env` y configura la conexión local de PostgreSQL.
2. Instala dependencias:

   ```powershell
   cd backend\altosDeCarpay
   python -m pip install -r requirements.txt
   ```

3. Comprueba Django y aplica migraciones:

   ```powershell
   python manage.py check
   python manage.py migrate
   python seed_data.py
   ```

4. Inicia el servidor:

   ```powershell
   python manage.py runserver 127.0.0.1:8000
   ```

### Frontend

En otra terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Vite reenvía `/api` al servidor Django en `127.0.0.1:8000`. El endpoint público de catálogo es `GET /api/v1/sectores/`; registro, login, reservas y pagos usan los endpoints `/api/v1/auth/` y `/api/v1/reservas/`.

Las credenciales deben residir solo en el `.env` local.

---

## Reglas de arriendo configurables

Las reglas acordadas con el cliente en la revisión del prototipo (23-11-2026) están en `backend/altosDeCarpay/altosDeCarpay/settings.py` y, para la web, en `frontend/src/reglas.js`. Si cambian, se deben modificar en ambos archivos:

| Regla | Parámetro | Valor |
| :--- | :--- | :--- |
| Bloques de arriendo (horas completas, nunca después de medianoche) | `BLOQUES_HORARIOS` | 13:00–18:00 y 19:00–00:00 |
| Plazo para pagar el anticipo | `PLAZO_PAGO_ANTICIPO_HORAS` | 24 horas desde la creación |
| Plazo para pagar el saldo | `PLAZO_SALDO_HORAS_ANTES` | 24 horas antes del evento |
| Cancelación del arrendatario con devolución parcial (todo menos el anticipo) | `DIAS_DEVOLUCION_PARCIAL` | más de 7 días antes |
| Registro de entrada (QR o manual) | `CHECKIN_MINUTOS_ANTES` | desde 60 minutos antes del inicio |
| Cierre automático de reservas en proceso | `CIERRE_EN_PROCESO_HORAS` | 2 horas después del término |
| Bloqueo de la cuenta por intentos fallidos | `LOGIN_MAX_INTENTOS` / `LOGIN_BLOQUEO_MINUTOS` | 5 intentos / 15 minutos |

## Pruebas automáticas

Todas las pruebas están en la carpeta [`pruebas/`](pruebas/README.md), ordenadas por tipo (unitarias, integración, sistema, regresión, rendimiento y seguridad). El manual con cada comando y para qué sirve está en `pruebas/README.md`.

```powershell
.\pruebas\ejecutar_pruebas.ps1 todas        # o: unitarias | integracion | sistema | regresion | rendimiento | estres | seguridad
docker exec carpay_backend python manage.py test pruebas   # suite automática completa (93 pruebas)
```

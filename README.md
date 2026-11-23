# Altos de Carpay

Aplicación web React/Vite conectada a una API Django REST y PostgreSQL, orquestada completamente mediante Docker Compose. El flujo de cliente usa el catálogo y los precios del servidor para registrar reservas, simular pagos y consultar reservas; el panel administrativo permite revisar reservas, gestionar tarifas, responder consultas y exportar reportes CSV.

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

### Accesos y Credenciales de Prueba (Seed Automático)

* **Frontend Web (Nginx + React):** `http://localhost/`
* **Backend API REST:** `http://localhost:8000/api/v1/` (también proxy en `http://localhost/api/v1/`)
* **Django Admin:** `http://localhost/admin/`
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

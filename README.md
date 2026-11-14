# Altos de Carpay

Aplicación web React/Vite conectada a una API Django REST y PostgreSQL. El flujo de cliente usa el catálogo y los precios del servidor para registrar reservas, simular pagos y consultar reservas.

## Backend y PostgreSQL (Windows)

1. Copia `backend\altosDeCarpay\.env.example` a `backend\altosDeCarpay\.env` y configura la conexión local de PostgreSQL. No guardes ese archivo en Git.
2. Instala dependencias:

   ```powershell
   cd backend\altosDeCarpay
   python -m pip install -r requirements.txt
   ```

3. Comprueba Django:

   ```powershell
   python manage.py check
   ```

4. Si la base está vacía, aplica las migraciones y crea un administrador:

   ```powershell
   python manage.py migrate
   python manage.py createsuperuser
   ```

5. Inicia el servidor:

   ```powershell
   python manage.py runserver 127.0.0.1:8000
   ```

## Frontend

En otra terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Vite reenvía `/api` al servidor Django en `127.0.0.1:8000`. El endpoint público de catálogo es `GET /api/v1/sectores/`; registro, login, reservas y pagos usan los endpoints `/api/v1/auth/` y `/api/v1/reservas/`.

## Base de datos existente

La documentación de PostgreSQL describe un esquema anterior al modelo Django actual. La migración `reservasApp 0003` agrega los campos de compatibilidad y las tablas de pertenencia que faltan; no elimina datos. Antes de usarla:

1. Haz un respaldo de la base y confirma que sus tablas/columnas coinciden con el esquema heredado documentado.
2. Con las credenciales locales configuradas, crea normalmente las tablas de autenticación de Django y registra como aplicadas las migraciones iniciales que representan las tablas del proyecto ya existentes:

   ```powershell
   python manage.py migrate auth
   python manage.py migrate usuariosApp 0001 --fake
   python manage.py migrate reservasApp 0002 --fake
   ```

3. Aplica la migración de compatibilidad:

   ```powershell
   python manage.py migrate
   ```

No uses `--fake` si la estructura real no coincide con la documentada, y no ejecutes estos pasos sin respaldo. Si la base contiene tablas `django_migrations` o `django_*` de una instalación previa, revisa `python manage.py showmigrations` antes de marcar nada. Para pruebas desechables, es preferible usar una base PostgreSQL vacía. La API calcula los importes y no almacena datos de tarjetas.

Las credenciales deben residir solo en el `.env` local. Rota cualquier credencial que ya haya quedado escrita en archivos o documentos del proyecto.

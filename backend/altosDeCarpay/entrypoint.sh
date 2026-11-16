#!/bin/sh
set -e

echo "Esperando que PostgreSQL esté listo..."
until python -c "import psycopg2, os; psycopg2.connect(dbname=os.environ.get('DB_NAME','altos_de_carpay_db'), user=os.environ.get('DB_USER','carpay_admin'), password=os.environ.get('DB_PASSWORD','CarpaySecurePass2026!'), host=os.environ.get('DB_HOST','db'), port=os.environ.get('DB_PORT','5432'))" 2>/dev/null; do
  echo "PostgreSQL no está disponible aún - esperando 2 segundos..."
  sleep 2
done

echo "PostgreSQL listo. Creando y aplicando migraciones..."
python manage.py makemigrations usuariosApp reservasApp consultasApp auditoriaApp --noinput || true
python manage.py migrate --noinput

echo "Cargando datos semilla..."
python seed_data.py || true

echo "Iniciando servidor Django REST API en 0.0.0.0:8000..."
exec python manage.py runserver 0.0.0.0:8000

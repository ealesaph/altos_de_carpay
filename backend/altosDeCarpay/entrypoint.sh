#!/bin/sh
set -e

# Las credenciales llegan solo por variables de entorno (.env); no hay valores por defecto en este archivo.
: "${DB_NAME:?Defina DB_NAME}"
: "${DB_USER:?Defina DB_USER}"
: "${DB_PASSWORD:?Defina DB_PASSWORD}"

echo "Esperando que PostgreSQL esté listo..."
until python -c "import psycopg2, os; psycopg2.connect(dbname=os.environ['DB_NAME'], user=os.environ['DB_USER'], password=os.environ['DB_PASSWORD'], host=os.environ.get('DB_HOST','db'), port=os.environ.get('DB_PORT','5432'))" 2>/dev/null; do
  echo "PostgreSQL no está disponible aún - esperando 2 segundos..."
  sleep 2
done

MODO_DEBUG=$(echo "${DJANGO_DEBUG:-false}" | tr '[:upper:]' '[:lower:]')

if [ "$MODO_DEBUG" = "true" ] || [ "$MODO_DEBUG" = "1" ]; then
  echo "Modo desarrollo: generando migraciones pendientes..."
  python manage.py makemigrations usuariosApp reservasApp consultasApp auditoriaApp --noinput || true
fi

echo "Aplicando migraciones..."
python manage.py migrate --noinput

SEMILLA=$(echo "${CARGAR_DATOS_SEMILLA:-$MODO_DEBUG}" | tr '[:upper:]' '[:lower:]')
if [ "$SEMILLA" = "true" ] || [ "$SEMILLA" = "1" ]; then
  echo "Cargando datos semilla (cuentas y catálogo de demostración)..."
  python seed_data.py || true
fi

if [ "$MODO_DEBUG" = "true" ] || [ "$MODO_DEBUG" = "1" ]; then
  echo "Iniciando servidor de desarrollo Django en 0.0.0.0:8000..."
  exec python manage.py runserver 0.0.0.0:8000
fi

echo "Recopilando archivos estáticos..."
python manage.py collectstatic --noinput

echo "Iniciando Gunicorn (producción) en 0.0.0.0:8000..."
exec gunicorn altosDeCarpay.wsgi:application \
  --bind 0.0.0.0:8000 \
  --workers "${GUNICORN_WORKERS:-3}" \
  --timeout 60 \
  --access-logfile - \
  --error-logfile -

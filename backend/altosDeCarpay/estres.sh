#!/bin/sh
# Prueba de estrés: levanta un Gunicorn temporal (3 workers, configuración de producción sin límites
# de solicitudes) en el puerto 8001, ejecuta la carga y lo detiene. El servidor normal (8000) no se toca.
cd /app
DJANGO_SETTINGS_MODULE=altosDeCarpay.settings_estres gunicorn altosDeCarpay.wsgi:application \
  --bind 127.0.0.1:8001 --workers 3 --timeout 60 --log-level warning >/tmp/estres_gunicorn.log 2>&1 &
PID=$!
sleep 4
python pruebas_estres/estres.py
kill $PID
python pruebas_estres/limite.py

#!/bin/sh
# PRUEBA DE ESTRÉS (rendimiento con carga creciente de 10 a 200 usuarios simultáneos).
# Levanta un Gunicorn temporal (3 workers, configuración de producción SIN límites de solicitudes:
# altosDeCarpay/settings_estres.py) en el puerto 8001, ejecuta la carga y lo detiene.
# El servidor normal (puerto 8000) no se toca.
# Ejecutar:  docker exec carpay_backend sh pruebas/rendimiento/estres.sh
# Variables opcionales: ESTRES_ETAPAS=10,25,50,100,200  ESTRES_POR_USUARIO=10
cd /app
DJANGO_SETTINGS_MODULE=altosDeCarpay.settings_estres gunicorn altosDeCarpay.wsgi:application \
  --bind 127.0.0.1:8001 --workers 3 --timeout 60 --log-level warning >/tmp/estres_gunicorn.log 2>&1 &
PID=$!
sleep 5
python pruebas/rendimiento/estres.py
RESULTADO=$?
kill $PID
exit $RESULTADO

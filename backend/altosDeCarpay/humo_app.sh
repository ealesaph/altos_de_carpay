echo "== Prueba de humo de la API de la app movil =="
python pruebas_app/humo_app.py
echo
echo "== Suite automatica del backend =="
python manage.py test 2>&1 | tail -4

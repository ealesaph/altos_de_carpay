"""Configuración usada SOLO para la prueba de estrés (pruebas_estres/estres.sh).

Hereda toda la configuración del proyecto y desactiva los límites de solicitudes por IP/usuario,
para medir la capacidad real del servidor. No se usa en desarrollo ni en producción.
"""
from .settings import *  # noqa: F401,F403

REST_FRAMEWORK = {**REST_FRAMEWORK, 'DEFAULT_THROTTLE_CLASSES': (), 'DEFAULT_THROTTLE_RATES': {**REST_FRAMEWORK['DEFAULT_THROTTLE_RATES']}}
DEBUG = False
ALLOWED_HOSTS = ['*']

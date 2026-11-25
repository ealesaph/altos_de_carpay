"""Límites de solicitudes con exclusión mutua.

Los límites de Django REST Framework leen el historial de la caché, lo modifican y lo vuelven a
guardar. Con solicitudes simultáneas (varios hilos o workers de Gunicorn) dos solicitudes pueden leer
el mismo historial y una sobrescribe a la otra, de modo que el contador se queda corto. La prueba de
estrés del 24-11-2026 lo demostró: 150 solicitudes anónimas en 2,4 s pasaron todas, cuando el límite
es 60 por minuto. Aquí la verificación y el registro se hacen dentro de un bloqueo de archivo
(fcntl.flock), compartido por todos los hilos y procesos del contenedor, por lo que el conteo es exacto.
"""
import fcntl
import os

from rest_framework.throttling import AnonRateThrottle, ScopedRateThrottle, UserRateThrottle

_ARCHIVO_BLOQUEO = os.path.join(os.environ.get('CACHE_DIR', '/tmp/altos_de_carpay_cache'), '.limites.lock')


class _ConBloqueo:
    def allow_request(self, request, view):
        os.makedirs(os.path.dirname(_ARCHIVO_BLOQUEO), exist_ok=True)
        with open(_ARCHIVO_BLOQUEO, 'a') as archivo:
            fcntl.flock(archivo, fcntl.LOCK_EX)
            try:
                return super().allow_request(request, view)
            finally:
                fcntl.flock(archivo, fcntl.LOCK_UN)


class LimiteAnonimo(_ConBloqueo, AnonRateThrottle):
    pass


class LimiteUsuario(_ConBloqueo, UserRateThrottle):
    pass


class LimitePorAlcance(_ConBloqueo, ScopedRateThrottle):
    pass

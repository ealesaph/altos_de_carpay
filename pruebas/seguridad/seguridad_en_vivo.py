"""PRUEBA DE SEGURIDAD EN VIVO · contra el sistema levantado con Docker (Nginx + Django + PostgreSQL).

Complementa test_seguridad_api.py revisando la configuración real del despliegue:
HTTPS, cabeceras, archivos sensibles, control de acceso a través del proxy, inyección,
fuerza bruta con IP falseada y avalancha de solicitudes.

Ejecutar:
    docker exec carpay_backend python pruebas/seguridad/seguridad_en_vivo.py

Nota: al final se envían ráfagas de solicitudes; durante ~1 minuto el límite de solicitudes
de la IP del contenedor queda activo (HTTP 429). Es lo esperado.
"""
import os
import socket
import ssl
import sys
import urllib.parse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, "/app")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from pruebas._comun import CLIENTE, WEB, Informe, http, iniciar_sesion, token_de_servidor  # noqa: E402

WEB_HTTPS = os.environ.get("PRUEBAS_WEB_HTTPS", WEB.replace("http://", "https://"))


def main():
    inf = Informe("PRUEBAS DE SEGURIDAD EN VIVO (configuración del despliegue)")
    host = urllib.parse.urlparse(WEB_HTTPS).hostname

    # --- Cifrado en tránsito ---
    print("\n  1) Cifrado en tránsito (HTTPS)")
    try:
        contexto = ssl.create_default_context()
        contexto.check_hostname, contexto.verify_mode = False, ssl.CERT_NONE
        with socket.create_connection((host, 443), timeout=5) as s, contexto.wrap_socket(s, server_hostname="localhost") as t:
            version, cifrado = t.version(), t.cipher()
        inf.caso("SEG-V01", "HTTPS disponible con TLS 1.2 o superior", version in ("TLSv1.2", "TLSv1.3"),
                 f"{version} · {cifrado[0]} ({cifrado[2]} bits)")
    except Exception as e:
        inf.caso("SEG-V01", "HTTPS disponible con TLS 1.2 o superior", False, str(e))

    r = http("GET", WEB_HTTPS + "/api/v1/sectores/")
    cab = r.cabeceras or {}
    esperadas = {"X-Content-Type-Options": "nosniff", "X-Frame-Options": "DENY", "Referrer-Policy": "same-origin"}
    faltan = [k for k, v in esperadas.items() if (cab.get(k) or "").lower() != v.lower()]
    inf.caso("SEG-V02", "Cabeceras de seguridad en HTTPS", r.codigo == 200 and not faltan,
             "; ".join(f"{k}: {cab.get(k)}" for k in esperadas) + (f" · faltan {faltan}" if faltan else ""))

    # --- Exposición de archivos y datos ---
    print("\n  2) Exposición de información")
    expuestos = []
    for ruta in ("/.env", "/backend/altosDeCarpay/.env", "/.git/config", "/docker-compose.yml", "/media/../.env"):
        r = http("GET", WEB + ruta, cabeceras={"Accept": "*/*"})
        texto = r.cuerpo if isinstance(r.cuerpo, str) else str(r.cuerpo)
        if any(clave in texto for clave in ("DJANGO_SECRET_KEY", "POSTGRES_PASSWORD", "[core]", "FIELD_ENCRYPTION_KEY")):
            expuestos.append(ruta)
    inf.caso("SEG-V03", "No se publican .env, .git ni archivos de configuración", not expuestos,
             f"expuestos: {expuestos}" if expuestos else "5 rutas sensibles probadas, ninguna expone secretos")

    r = http("GET", "/no-existe-esta-ruta/")
    texto = r.cuerpo if isinstance(r.cuerpo, str) else str(r.cuerpo)
    debug = "Traceback" in texto or "DEBUG = True" in texto or "Using the URLconf" in texto
    inf.caso("SEG-V04", "Los errores no muestran detalles internos (DEBUG desactivado)", not debug,
             "DEBUG está activo: correcto en desarrollo, en producción usar DJANGO_DEBUG=false" if debug
             else f"HTTP {r.codigo} sin detalles internos", advertencia=True)

    servidor = (http("GET", WEB + "/", cabeceras={"Accept": "text/html"}).cabeceras or {}).get("Server", "")
    con_version = any(c.isdigit() for c in servidor)
    inf.caso("SEG-V05", "El servidor web no publica su versión", not con_version,
             f"Server: {servidor} (se puede ocultar con «server_tokens off;» en Nginx)" if con_version
             else f"Server: {servidor}", advertencia=True)

    # --- Control de acceso a través del proxy ---
    print("\n  3) Control de acceso a través de Nginx")
    codigos = {ruta: http("GET", ruta).codigo for ruta in ("/reservas/", "/usuarios/", "/auditoria/logs/", "/auth/mis-datos/")}
    inf.caso("SEG-V06", "Sin sesión, las rutas privadas responden 401", all(c == 401 for c in codigos.values()), str(codigos))

    tc = token_de_servidor(CLIENTE[0]) or iniciar_sesion(CLIENTE)[0]
    if tc:
        codigos = {ruta: http("GET", ruta, tc).codigo for ruta in
                   ("/usuarios/", "/auditoria/logs/", "/reservas/exportar/", "/incidencias/", "/reservas/resumen/")}
        inf.caso("SEG-V07", "Con sesión de cliente, las rutas de administrador responden 403",
                 all(c == 403 for c in codigos.values()), str(codigos))
        r = http("GET", "/reservas/?estado=" + urllib.parse.quote("' OR '1'='1"), tc)
        inf.caso("SEG-V08", "Inyección SQL en filtros no provoca errores ni fuga de datos",
                 r.codigo == 200 and isinstance(r.cuerpo, dict), f"HTTP {r.codigo}")
    else:
        inf.caso("SEG-V07", "Rutas de administrador con sesión de cliente", False, "no se obtuvo token de cliente")

    # --- Fuerza bruta e IP falseada ---
    print("\n  4) Fuerza bruta en el inicio de sesión (consume el límite de 5 intentos/min)")
    codigos = []
    for i in range(7):
        r = http("POST", "/auth/login/", datos={"correo": f"no.existe.{i}@example.com", "password": "' OR 1=1 --"},
                 cabeceras={"X-Forwarded-For": f"203.0.113.{i}"})
        codigos.append(r.codigo)
    inf.caso("SEG-V09", "Intentos de login repetidos se bloquean aunque se falsee X-Forwarded-For",
             429 in codigos and 500 not in codigos, f"códigos: {codigos}")

    # --- Avalancha ---
    print("\n  5) Avalancha de 150 solicitudes anónimas simultáneas (límite 60/min por IP)")

    def pedir(_):
        return http("GET", "/sectores/", timeout=15).codigo

    with ThreadPoolExecutor(max_workers=20) as ex:
        conteo = Counter(ex.map(pedir, range(150)))
    inf.caso("SEG-V10", "El servidor limita la avalancha (HTTP 429) sin caerse",
             conteo.get(429, 0) > 0 and conteo.get(0, 0) == 0 and not any(c >= 500 for c in conteo),
             f"200: {conteo.get(200, 0)} · 429: {conteo.get(429, 0)} · otros: "
             f"{ {k: v for k, v in conteo.items() if k not in (200, 429)} }")
    inf.terminar()


if __name__ == "__main__":
    main()

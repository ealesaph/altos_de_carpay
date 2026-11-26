"""Utilidades compartidas por las pruebas que se ejecutan contra el sistema levantado con Docker
(sistema, rendimiento y seguridad en vivo). Solo usa la biblioteca estándar de Python.

Por defecto las pruebas entran por Nginx, igual que el navegador: http://frontend (dentro de la red
de Docker). Se puede cambiar con la variable PRUEBAS_WEB, por ejemplo PRUEBAS_WEB=http://localhost.
"""
import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.request

WEB = os.environ.get("PRUEBAS_WEB", "http://frontend").rstrip("/")
API = os.environ.get("PRUEBAS_API", WEB + "/api/v1").rstrip("/")
ADMIN = (os.environ.get("PRUEBAS_ADMIN_CORREO", "admin@altosdecarpay.cl"),
         os.environ.get("PRUEBAS_ADMIN_CLAVE", "CarpayAdmin2026!"))
CLIENTE = (os.environ.get("PRUEBAS_CLIENTE_CORREO", "camila.rojas@gmail.com"),
           os.environ.get("PRUEBAS_CLIENTE_CLAVE", "ClientePass2026!"))

_COLOR = sys.stdout.isatty() or os.environ.get("PRUEBAS_COLOR") == "1"
VERDE, ROJO, AMARILLO, CIAN, NEGRITA, FIN = (
    ("\033[92m", "\033[91m", "\033[93m", "\033[96m", "\033[1m", "\033[0m") if _COLOR else ("",) * 6
)
SIN_VERIFICAR_TLS = ssl.create_default_context()
SIN_VERIFICAR_TLS.check_hostname = False
SIN_VERIFICAR_TLS.verify_mode = ssl.CERT_NONE  # certificado autofirmado del entorno local


class Respuesta:
    def __init__(self, codigo, cuerpo, cabeceras, ms, crudo=b""):
        self.codigo, self.cuerpo, self.cabeceras, self.ms, self.crudo = codigo, cuerpo, cabeceras, ms, crudo

    def __repr__(self):
        return f"<HTTP {self.codigo} {self.ms:.0f} ms>"


def http(metodo, ruta, token=None, datos=None, base=None, cabeceras=None, timeout=30):
    """Hace una solicitud y devuelve Respuesta (código, JSON o texto, cabeceras, milisegundos)."""
    url = ruta if ruta.startswith("http") else (base or API) + ruta
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    req = urllib.request.Request(url, data=cuerpo, method=metodo)
    req.add_header("Accept", "application/json")
    if cuerpo is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    for k, v in (cabeceras or {}).items():
        req.add_header(k, v)
    contexto = SIN_VERIFICAR_TLS if url.startswith("https") else None
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=contexto) as r:
            codigo, crudo, cab = r.status, r.read(), r.headers
    except urllib.error.HTTPError as e:
        codigo, crudo, cab = e.code, e.read(), e.headers
    except Exception as e:  # sin conexión, tiempo agotado, etc.
        return Respuesta(0, str(e), {}, (time.perf_counter() - t0) * 1000)
    ms = (time.perf_counter() - t0) * 1000
    try:
        datos_resp = json.loads(crudo.decode() or "null")
    except Exception:
        datos_resp = crudo.decode(errors="replace")
    return Respuesta(codigo, datos_resp, cab, ms, crudo)


def lista(datos):
    """Resultados de una respuesta paginada ({"results": [...]}) o de una lista simple."""
    if isinstance(datos, dict):
        return datos.get("results", [])
    return datos or []


def token_de_servidor(correo):
    """Genera un JWT en el propio servidor (sin contraseña). Solo funciona dentro de carpay_backend.
    Se usa cuando el límite de intentos de inicio de sesión (5 por minuto) ya se alcanzó."""
    try:
        sys.path.insert(0, "/app")
        os.environ.setdefault("DJANGO_SETTINGS_MODULE", "altosDeCarpay.settings")
        import django
        django.setup()
        from rest_framework_simplejwt.tokens import RefreshToken
        from usuariosApp.models import Usuario
        return str(RefreshToken.for_user(Usuario.objects.get(correo=correo)).access_token)
    except Exception:
        return None


def iniciar_sesion(credenciales):
    """Inicia sesión por la API. Devuelve (token, nota)."""
    r = http("POST", "/auth/login/", datos={"correo": credenciales[0], "password": credenciales[1]})
    if r.codigo == 200 and isinstance(r.cuerpo, dict) and r.cuerpo.get("access"):
        return r.cuerpo["access"], f"login HTTP 200 ({r.ms:.0f} ms)"
    if r.codigo == 429:
        token = token_de_servidor(credenciales[0])
        if token:
            return token, "límite de intentos alcanzado (429): token generado en el servidor"
    return None, f"login HTTP {r.codigo}: {str(r.cuerpo)[:150]}"


class Informe:
    """Lleva la cuenta de casos aprobados/fallidos y escribe un resumen al final."""

    def __init__(self, titulo):
        self.titulo, self.casos, self.inicio = titulo, [], time.time()
        print(f"\n{NEGRITA}{CIAN}{'=' * 88}\n  {titulo}\n  Web: {WEB}   API: {API}\n{'=' * 88}{FIN}")

    def caso(self, codigo, descripcion, aprobado, detalle="", advertencia=False):
        if advertencia and not aprobado:
            estado = f"{AMARILLO}[ADVERTENCIA]{FIN}"
        else:
            estado = f"{VERDE}[APROBADO]{FIN}" if aprobado else f"{ROJO}[FALLIDO]{FIN}"
        print(f"  {estado} {NEGRITA}{codigo}{FIN} {descripcion}")
        if detalle:
            print(f"             {detalle}")
        self.casos.append((codigo, descripcion, bool(aprobado) or advertencia))
        return aprobado

    def terminar(self):
        total = len(self.casos)
        ok = sum(1 for c in self.casos if c[2])
        color = VERDE if ok == total else ROJO
        print(f"\n{NEGRITA}{color}  RESULTADO: {ok} de {total} casos aprobados "
              f"({time.time() - self.inicio:.1f} s){FIN}")
        fallidos = [c for c in self.casos if not c[2]]
        for c in fallidos:
            print(f"   - Falló {c[0]}: {c[1]}")
        print("=" * 88)
        sys.exit(0 if not fallidos else 1)

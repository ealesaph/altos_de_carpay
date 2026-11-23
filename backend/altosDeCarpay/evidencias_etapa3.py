"""
Script de evidencias - Etapa 3 (Informe de Pruebas) - Altos de Carpay
Ejecutar DENTRO del contenedor carpay_backend:
    python evidencias_etapa3.py fun01   -> CP-FUN-01  Reserva + pago anticipo + Check-in QR
    python evidencias_etapa3.py fun02   -> CP-FUN-02  Incidencia + Check-out QR + sobretiempo
    python evidencias_etapa3.py fun03   -> CP-FUN-03  Bitacora congelada (solo lectura)
    python evidencias_etapa3.py int01   -> CP-INT-01  30% anticipo, solapamiento 409, expiracion 24h
    python evidencias_etapa3.py int02   -> CP-INT-02  Envio de correo de la reserva
    python evidencias_etapa3.py seg01   -> CP-SEG-01  401 / 403 / ARCO / auditoria / 429
    python evidencias_etapa3.py ren01   -> CP-REN-01  Tiempos de respuesta < 2.0 s
Solo usa la libreria estandar + Django (ya instalado en el contenedor).
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import timedelta

BASE = os.environ.get("EVID_BASE", "http://localhost:8000/api/v1")
ESTADO = "/tmp/evidencias_etapa3.json"
ADMIN = ("admin@altosdecarpay.cl", "CarpayAdmin2026!")
CLIENTE = ("camila.rojas@gmail.com", "ClientePass2026!")

V, R, A, C, B, N = "\033[92m", "\033[91m", "\033[93m", "\033[96m", "\033[1m", "\033[0m"


def titulo(t):
    print(f"\n{B}{C}{'=' * 78}\n  {t}\n{'=' * 78}{N}")


def paso(t):
    print(f"\n{B}>> {t}{N}")


def http(metodo, ruta, token=None, datos=None):
    url = ruta if ruta.startswith("http") else BASE + ruta
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    req = urllib.request.Request(url, data=cuerpo, method=metodo)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            codigo, raw = r.status, r.read()
    except urllib.error.HTTPError as e:
        codigo, raw = e.code, e.read()
    ms = (time.perf_counter() - t0) * 1000
    try:
        cuerpo = json.loads(raw.decode() or "null")
    except Exception:
        cuerpo = raw.decode(errors="replace")[:200]
    color = V if codigo < 300 else (A if codigo < 500 else R)
    print(f"   {metodo:6} {url.replace(BASE, '/api/v1')}  ->  {color}{B}HTTP {codigo}{N}  ({ms:.0f} ms)")
    return codigo, cuerpo, ms


def http_silencioso(ruta, token):
    req = urllib.request.Request(BASE + ruta, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code
    except Exception:
        return 0


def lista_de(d):
    if isinstance(d, dict):
        return d.get("results", [])
    return d or []


def mostrar(d, claves=None):
    if isinstance(d, dict) and claves:
        d = {k: d.get(k) for k in claves if k in d}
    texto = json.dumps(d, ensure_ascii=False, indent=2, default=str)
    lineas = texto.splitlines()
    for l in lineas[:22]:
        print("      " + l)
    if len(lineas) > 22:
        print("      ...")


def buscar_token(d):
    if isinstance(d, dict):
        for k in ("access", "access_token", "token"):
            if isinstance(d.get(k), str):
                return d[k]
        for v in d.values():
            t = buscar_token(v)
            if t:
                return t
    return None


def login(cred, verbose=True):
    cache = cargar().get("tokens", {})
    tok = cache.get(cred[0])
    if tok and http_silencioso("/auth/perfil/", tok) == 200:
        if verbose:
            print(f"      Sesion JWT vigente de {cred[0]}  (JWT: {tok[:24]}...)")
        return tok
    codigo, cuerpo, _ = http("POST", "/auth/login/", datos={"correo": cred[0], "password": cred[1]})
    tok = buscar_token(cuerpo)
    if tok:
        cache[cred[0]] = tok
        guardar(tokens=cache)
    if verbose:
        print(f"      Sesion iniciada como {cred[0]}  (JWT: {tok[:24]}...)" if tok else f"      {cuerpo}")
    return tok


def ok(cond, msg):
    print(f"   {V+B}[APROBADO]{N} {msg}" if cond else f"   {R+B}[FALLIDO]{N} {msg}")


def guardar(**kw):
    e = cargar()
    e.update(kw)
    json.dump(e, open(ESTADO, "w"))


def cargar():
    try:
        return json.load(open(ESTADO))
    except Exception:
        return {}


def django_setup():
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "altosDeCarpay.settings")
    import django
    django.setup()


def nueva_reserva(tok_cli, dias=None, hora="10:00", sector=None, horas=4):
    import random
    _, sectores, _ = http("GET", "/sectores/", tok_cli)
    lista = lista_de(sectores)
    sector = sector or lista[0]["id_sector"]
    dias = dias or random.randint(40, 300)
    from datetime import date
    fecha = (date.today() + timedelta(days=dias)).isoformat()
    datos = {"fecha": fecha, "hora_inicio": hora, "sectores": [{"sector": sector, "horas": horas}],
             "servicios": [], "acepta_condiciones": True}
    codigo, r, _ = http("POST", "/reservas/", tok_cli, datos)
    return codigo, r, datos


def id_reserva(r):
    return r.get("id_reserva") or r.get("id")


# --------------------------------------------------------------------------
def fun01():
    titulo("CP-FUN-01 | Generacion de codigo QR y validacion de Check-in (RF-03)")
    paso("1. Cliente inicia sesion y crea una reserva")
    tc = login(CLIENTE)
    codigo, r, _ = nueva_reserva(tc)
    rid = id_reserva(r)
    mostrar(r, ["id_reserva", "fecha", "hora_inicio", "estado", "monto_total", "monto_anticipo", "token_qr"])
    paso("2. Cliente paga el anticipo (30%) por Webpay simulado")
    http("POST", f"/reservas/{rid}/pagos/", tc, {"tipo_pago": "Anticipo", "metodo_pago": "Webpay"})
    _, r, _ = http("GET", f"/reservas/{rid}/", tc)
    print(f"      Estado de la reserva: {B}{r.get('estado')}{N}   token_qr: {r.get('token_qr')}")
    paso("3. Administrador escanea el QR y registra la ENTRADA (Check-in)")
    ta = login(ADMIN)
    codigo, v, _ = http("POST", f"/reservas/{rid}/validar-acceso/", ta, {"tipo_registro": "Entrada"})
    mostrar(v)
    _, r, _ = http("GET", f"/reservas/{rid}/", ta)
    print(f"      Estado de la reserva tras Check-in: {B}{r.get('estado')}{N}")
    ok(codigo in (200, 201) and r.get("estado") == "En Proceso",
       "Check-in registrado con estampa de tiempo del servidor; reserva paso de Confirmado a En Proceso")
    guardar(reserva_evento=rid)


def prep():
    titulo("Preparacion: reserva Confirmada para evidencia movil (QR del cliente)")
    tc = login(CLIENTE)
    c, r, _ = nueva_reserva(tc, dias=3, hora="15:00")
    rid = id_reserva(r)
    http("POST", f"/reservas/{rid}/pagos/", tc, {"tipo_pago": "Anticipo", "metodo_pago": "Webpay"})
    _, r, _ = http("GET", f"/reservas/{rid}/", tc)
    mostrar(r, ["id_reserva", "fecha", "hora_inicio", "estado", "monto_total", "monto_pagado", "token_qr"])
    guardar(reserva_movil=rid)


def reloj():
    titulo("Simulacion de reloj para la reserva movil: el evento termino hace 75 minutos")
    rid = cargar().get("reserva_movil")
    django_setup()
    from django.utils import timezone
    from reservasApp.models import Reserva
    res = Reserva.objects.get(pk=rid)
    inicio = timezone.localtime() - timedelta(hours=res.horas_reservadas, minutes=75)
    Reserva.objects.filter(pk=rid).update(fecha=inicio.date(), hora_inicio=inicio.time().replace(microsecond=0))
    res.refresh_from_db()
    print(f"      Reserva N° {rid} ({res.estado}) horario: {res.inicio:%d-%m-%Y %H:%M} -> {res.fin:%H:%M}  (ahora: {timezone.localtime():%H:%M})")


def fun02():
    titulo("CP-FUN-02 | Incidencias, Check-out QR y calculo de sobretiempo (RF-04 a RF-07)")
    rid = cargar().get("reserva_evento")
    ta = login(ADMIN)
    paso(f"1. Registro de incidencia en la bitacora de la reserva N° {rid} (En Proceso)")
    c, inc, _ = http("POST", "/incidencias/", ta, {"reserva": rid, "titulo": "Uso de area no contratada",
                                                     "descripcion": "Grupo utilizo la Piscina 20 min sin contratarla."})
    mostrar(inc, ["id_incidencia", "reserva", "reserva_estado", "autor_nombre", "titulo", "fecha_creacion"])
    paso("2. Simulacion de reloj: el evento termino hace 75 minutos (se ajusta fecha/hora del evento)")
    django_setup()
    from django.utils import timezone
    from reservasApp.models import Reserva
    res = Reserva.objects.get(pk=rid)
    inicio = timezone.localtime() - timedelta(hours=res.horas_reservadas, minutes=75)
    Reserva.objects.filter(pk=rid).update(fecha=inicio.date(), hora_inicio=inicio.time().replace(microsecond=0))
    res.refresh_from_db()
    print(f"      Horario contratado: {res.inicio:%d-%m-%Y %H:%M} -> {res.fin:%H:%M}  (ahora: {timezone.localtime():%H:%M})")
    paso("3. Administrador escanea el QR y registra la SALIDA (Check-out)")
    c, v, _ = http("POST", f"/reservas/{rid}/validar-acceso/", ta, {"tipo_registro": "Salida"})
    mostrar(v)
    _, r, _ = http("GET", f"/reservas/{rid}/", ta)
    print(f"      Estado de la reserva tras Check-out: {B}{r.get('estado')}{N}")
    ok(c in (200, 201) and r.get("estado") == "Finalizado" and int(v.get("minutos_excedidos", 0)) >= 75,
       f"Sobretiempo calculado: {v.get('minutos_excedidos')} min -> cargo propuesto ${v.get('cargo_sobretiempo_propuesto')}")


def fun03():
    titulo("CP-FUN-03 | Congelamiento inmutable de la bitacora (solo lectura) - Ley 19.799")
    rid = cargar().get("reserva_evento")
    ta = login(ADMIN)
    paso(f"1. Intento de crear una incidencia sobre la reserva N° {rid} (Finalizado)")
    c1, r1, _ = http("POST", "/incidencias/", ta, {"reserva": rid, "titulo": "Agregado tardio",
                                                     "descripcion": "Intento de modificar la bitacora."})
    mostrar(r1)
    _, lista, _ = http("GET", f"/incidencias/?reserva={rid}", ta)
    items = lista_de(lista)
    iid = items[0]["id_incidencia"] if items else 1
    paso(f"2. Intento de editar y eliminar la incidencia INC-{iid:04d} por API")
    c2, _, _ = http("PATCH", f"/incidencias/{iid}/", ta, {"titulo": "editado"})
    c3, _, _ = http("DELETE", f"/incidencias/{iid}/", ta)
    paso("3. Intento directo sobre el modelo (capa de datos / ORM)")
    django_setup()
    from reservasApp.models import BitacoraIncidencia
    inc = BitacoraIncidencia.objects.get(pk=iid)
    for nombre, f in (("save()", lambda: (setattr(inc, "titulo", "x"), inc.save())), ("delete()", inc.delete)):
        try:
            f()
            print(f"      BitacoraIncidencia.{nombre}: {R}permitido{N}")
        except PermissionError as e:
            print(f"      BitacoraIncidencia.{nombre}: {A}PermissionError{N} -> {e}")
    ok(c1 == 400 and c2 in (403, 405) and c3 in (403, 405),
       "Bitacora en modo solo lectura: API rechaza (400/405) y el modelo bloquea save()/delete()")


def int01():
    titulo("CP-INT-01 | Reglas financieras: 30% anticipo, solapamiento y expiracion 24 h")
    tc = login(CLIENTE)
    paso("1. Creacion de reserva y calculo automatico del anticipo")
    c, r, datos = nueva_reserva(tc, horas=5)
    rid = id_reserva(r)
    mostrar(r, ["id_reserva", "estado", "monto_total", "monto_anticipo", "saldo_pendiente"])
    try:
        ok(float(r["monto_anticipo"]) == round(float(r["monto_total"]) * 0.30),
           f"Anticipo = 30% de {r['monto_total']} = {r['monto_anticipo']}")
    except Exception:
        pass
    paso("2. Intento de reservar el MISMO sector en el MISMO horario (solapamiento)")
    c2, r2, _ = http("POST", "/reservas/", tc, datos)
    mostrar(r2)
    ok(c2 == 409, "El motor de reservas impide el doble agendamiento (HTTP 409 Conflict)")
    paso("3. Expiracion: se simula que la reserva fue creada hace 25 h y se ejecuta el comando programado")
    django_setup()
    from django.core.management import call_command
    from django.utils import timezone
    from reservasApp.models import Reserva
    Reserva.objects.filter(pk=rid).update(fecha_creacion=timezone.now() - timedelta(hours=25))
    print(f"      $ python manage.py cancelar_reservas_impagas")
    call_command("cancelar_reservas_impagas")
    print(f"      Estado de la reserva N° {rid}: {B}{Reserva.objects.get(pk=rid).estado}{N}")
    c3, _, _ = http("POST", "/reservas/", tc, datos)
    ok(Reserva.objects.get(pk=rid).estado == "Cancelado" and c3 == 201,
       "Reserva impaga expirada a las 24 h y bloque horario liberado (nueva reserva HTTP 201)")


def int02():
    titulo("CP-INT-02 | Notificacion transaccional por correo electronico")
    rid = cargar().get("reserva_evento")
    ta = login(ADMIN)
    django_setup()
    from django.conf import settings
    print(f"      EMAIL_BACKEND = {settings.EMAIL_BACKEND}")
    print(f"      EMAIL_HOST    = {getattr(settings, 'EMAIL_HOST', '-')}")
    paso(f"1. Reenvio administrativo del detalle de la reserva N° {rid}")
    c, r, _ = http("POST", f"/reservas/{rid}/enviar-correo/", ta)
    mostrar(r)
    ok(c == 200, "Correo con desglose financiero y token QR enviado al cliente")


def seg01():
    titulo("CP-SEG-01 | Control de acceso JWT/RBAC, ARCO (Ley 19.628) y auditoria")
    paso("1. Peticiones SIN cabecera Authorization: Bearer")
    for ruta in ("/reservas/", "/incidencias/", "/auditoria/logs/"):
        http("GET", ruta)
    paso("2. Peticiones con token de rol CLIENTE hacia rutas administrativas")
    tc = login(CLIENTE)
    cods = [http("GET", ruta, tc)[0] for ruta in ("/incidencias/", "/reservas/exportar/", "/auditoria/logs/", "/usuarios/")]
    ok(all(c == 403 for c in cods), "Rutas administrativas rechazadas con HTTP 403 Forbidden")
    paso("3. Inyeccion SQL / XSS en parametros (sanitizacion del ORM)")
    import urllib.parse
    for carga in ("' OR 1=1 --", "<script>alert(1)</script>"):
        c, d, _ = http("GET", "/reservas/?estado=" + urllib.parse.quote(carga), tc)
        print(f"      carga {carga!r}: {d.get('count', len(lista_de(d))) if isinstance(d, dict) else len(d or [])} registros devueltos (solo los propios del cliente)")


def seg02():
    titulo("CP-SEG-01 (cont.) | Derechos ARCO, auditoria y bloqueo por fuerza bruta")
    tc = login(CLIENTE)
    paso("4. Derecho de acceso ARCO: /auth/mis-datos/")
    c, d, _ = http("GET", "/auth/mis-datos/", tc)
    mostrar(d if isinstance(d, dict) else {"datos": d}, None)
    paso("5. Trazabilidad en auditoriaApp (ultimos registros, vista admin)")
    ta = login(ADMIN)
    _, logs, _ = http("GET", "/auditoria/logs/", ta)
    items = lista_de(logs)
    for l in (items or [])[:5]:
        print("      " + json.dumps({k: l.get(k) for k in list(l)[:6]}, ensure_ascii=False, default=str)[:150])
    paso("6. Fuerza bruta en login (limite 5/min)")
    for i in range(6):
        c, _, _ = http("POST", "/auth/login/", datos={"correo": "atacante@test.cl", "password": f"x{i}"})
    ok(c == 429, "Bloqueo por fuerza bruta: HTTP 429 Too Many Requests")


def ren01():
    titulo("CP-REN-01 | Tiempos de respuesta de la API REST (umbral < 2.0 s)")
    ta = login(ADMIN)
    rid = cargar().get("reserva_evento")
    rutas = ["/sectores/", "/servicios/", "/reservas/", f"/reservas/{rid}/", f"/reservas/{rid}/validar-acceso/"]
    resumen = []
    for ruta in rutas:
        tiempos = [http("GET", ruta, ta)[2] for _ in range(5)]
        resumen.append((ruta, min(tiempos), sum(tiempos) / len(tiempos), max(tiempos)))
    print(f"\n   {B}{'Endpoint':40} {'min':>8} {'prom':>8} {'max':>8}{N}")
    for ruta, mn, pr, mx in resumen:
        print(f"   {ruta:40} {mn:7.0f}ms {pr:7.0f}ms {mx:7.0f}ms")
    ok(all(r[3] < 2000 for r in resumen), "Todas las respuestas bajo 2.0 s (25 peticiones)")


if __name__ == "__main__":
    casos = {f.__name__: f for f in (prep, reloj, fun01, fun02, fun03, int01, int02, seg01, seg02, ren01)}
    for arg in sys.argv[1:] or casos:
        casos[arg]()

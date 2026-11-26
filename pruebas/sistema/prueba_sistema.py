"""PRUEBA DE SISTEMA (extremo a extremo) · Altos de Carpay

Recorre el sistema completo tal como lo usan las personas, entrando por Nginx (la web) y pasando
por la API Django y PostgreSQL. No usa atajos internos salvo para "adelantar el reloj" del evento
(el check-in solo se permite desde 1 hora antes del inicio), lo que se informa en pantalla.

Ejecutar (con el sistema levantado):
    docker exec carpay_backend python pruebas/sistema/prueba_sistema.py

Variables opcionales:
    PRUEBAS_WEB=http://frontend          URL de la web (por defecto, Nginx dentro de Docker)
    PRUEBAS_CORREO=mi@correo.cl          correo del cliente de prueba (para recibir los correos reales)

Deja en la base un cliente de prueba y una reserva finalizada (sirven de evidencia).
"""
import os
import random
import re
import sys
import time
from datetime import date, timedelta

sys.path.insert(0, "/app")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from pruebas._comun import ADMIN, CLIENTE, WEB, Informe, http, iniciar_sesion, lista  # noqa: E402


def reloj_disponible():
    try:
        os.environ.setdefault("DJANGO_SETTINGS_MODULE", "altosDeCarpay.settings")
        import django
        django.setup()
        return True
    except Exception:
        return False


def mover_evento(id_reserva, minutos_desde_ahora):
    """Cambia la fecha/hora de la reserva para que el evento empiece en `minutos_desde_ahora`."""
    from django.utils import timezone
    from reservasApp.models import Reserva
    inicio = timezone.localtime() + timedelta(minutes=minutos_desde_ahora)
    Reserva.objects.filter(pk=id_reserva).update(fecha=inicio.date(),
                                                 hora_inicio=inicio.time().replace(second=0, microsecond=0))
    return inicio


def main():
    inf = Informe("PRUEBA DE SISTEMA · flujo completo cliente + administrador")
    marca = time.strftime("%Y%m%d%H%M%S")

    # ---------------- 1. La web pública ----------------
    print("\n  1) Web pública (Nginx + React)")
    r = http("GET", WEB + "/", cabeceras={"Accept": "text/html"})
    html = r.cuerpo if isinstance(r.cuerpo, str) else ""
    inf.caso("SIS-01", "La portada responde", r.codigo == 200 and 'id="root"' in html, f"HTTP {r.codigo} ({r.ms:.0f} ms)")

    recursos = re.findall(r'(?:src|href)="(/assets/[^"]+)"', html)
    malos = [x for x in recursos if http("GET", WEB + x, cabeceras={"Accept": "*/*"}).codigo != 200]
    inf.caso("SIS-02", "Los archivos JS/CSS de la web cargan", recursos and not malos,
             f"{len(recursos)} archivos, {len(malos)} con error {malos[:3] if malos else ''}")

    rutas = ["/login", "/registro", "/reservar/espacio", "/mis-reservas", "/mi-cuenta", "/panel",
             "/condiciones", "/privacidad"]
    fallan = [ruta for ruta in rutas if http("GET", WEB + ruta, cabeceras={"Accept": "text/html"}).codigo != 200]
    inf.caso("SIS-03", "Las páginas de la web se pueden recargar (rutas SPA)", not fallan,
             f"{len(rutas)} rutas probadas" + (f"; fallan: {fallan}" if fallan else ""))

    r_sec = http("GET", "/sectores/")
    r_srv = http("GET", "/servicios/")
    sectores = [s for s in lista(r_sec.cuerpo) if s.get("estado") == "Disponible"]
    inf.caso("SIS-04", "Catálogo público de espacios y servicios (sin sesión)",
             r_sec.codigo == 200 and r_srv.codigo == 200 and sectores,
             f"{len(lista(r_sec.cuerpo))} espacios ({len(sectores)} disponibles), {len(lista(r_srv.cuerpo))} servicios")
    r = http("GET", "/auth/politica-privacidad/")
    inf.caso("SIS-05", "Política de privacidad pública (Ley 21.719)", r.codigo == 200, f"HTTP {r.codigo}")
    if not sectores:
        inf.terminar()

    # ---------------- 2. Cliente ----------------
    print("\n  2) Cliente: registro, sesión, reserva y pago")
    correo = os.environ.get("PRUEBAS_CORREO") or f"cliente.sistema.{marca}@example.com"
    if "+" not in correo and os.environ.get("PRUEBAS_CORREO"):
        usuario, dominio = correo.split("@")
        correo = f"{usuario}+{marca}@{dominio}"
    cuerpo = random.randint(5_000_000, 25_000_000)
    suma, factor = 0, 2
    for d in reversed(str(cuerpo)):
        suma += int(d) * factor
        factor = 2 if factor == 7 else factor + 1
    dv = {11: "0", 10: "K"}.get(11 - suma % 11, str(11 - suma % 11))
    clave = "SistemaPrueba2026!"
    r = http("POST", "/auth/registro/", datos={
        "correo": correo, "rut": f"{cuerpo}-{dv}", "nombre": "Cliente", "apellido_paterno": "Prueba",
        "apellido_materno": "Sistema", "telefono": "+56912345678", "password": clave, "acepta_politica_datos": True,
    })
    if r.codigo == 429:
        inf.caso("SIS-06", "Registro de cliente nuevo", False,
                 "Límite de 5 registros por hora alcanzado: se usa la cuenta de demostración", advertencia=True)
        cred = CLIENTE
    else:
        inf.caso("SIS-06", "Registro de cliente nuevo con RUT y consentimiento", r.codigo == 201,
                 f"HTTP {r.codigo} · {correo}" if r.codigo == 201 else f"HTTP {r.codigo}: {str(r.cuerpo)[:200]}")
        cred = (correo, clave)

    tc, nota = iniciar_sesion(cred)
    inf.caso("SIS-07", "Inicio de sesión del cliente (JWT)", bool(tc), nota)
    if not tc:
        inf.terminar()
    r = http("GET", "/auth/perfil/", tc)
    inf.caso("SIS-08", "Perfil del cliente", r.codigo == 200 and r.cuerpo.get("correo") == cred[0], f"HTTP {r.codigo}")

    sector = sectores[0]
    servicios = lista(r_srv.cuerpo)
    datos = None
    for _ in range(6):
        fecha = (date.today() + timedelta(days=random.randint(60, 700))).isoformat()
        datos = {"fecha": fecha, "hora_inicio": "13:00", "acepta_condiciones": True,
                 "sectores": [{"sector": sector["id_sector"], "horas": 5}],
                 "servicios": [{"servicio": servicios[0]["id_servicio"], "cantidad": 1}] if servicios else []}
        r = http("POST", "/reservas/", tc, datos)
        if r.codigo != 409:
            break
    reserva = r.cuerpo if r.codigo == 201 else {}
    rid = reserva.get("id_reserva")
    total = float(reserva.get("monto_total", 0) or 0)
    anticipo_ok = rid and round(total * 0.30) == round(float(reserva.get("monto_anticipo", -1)))
    inf.caso("SIS-09", "Crear reserva (espacio + servicio) con anticipo del 30 %",
             r.codigo == 201 and reserva.get("estado") == "Pendiente" and anticipo_ok,
             f"HTTP {r.codigo} · N° {rid} · {sector['nombre']} {datos['fecha']} 13:00–18:00 · total ${total:,.0f} · "
             f"anticipo ${float(reserva.get('monto_anticipo', 0) or 0):,.0f}" if rid else str(r.cuerpo)[:200])
    if not rid:
        inf.terminar()

    r = http("POST", "/reservas/", tc, datos)
    inf.caso("SIS-10", "El mismo espacio y horario no se puede reservar dos veces", r.codigo == 409, f"HTTP {r.codigo}")
    r = http("POST", "/reservas/", tc, {**datos, "hora_inicio": "16:00", "sectores": [{"sector": sector["id_sector"], "horas": 3}]})
    inf.caso("SIS-11", "Horario fuera de bloque (16:00–19:00) es rechazado", r.codigo == 400, f"HTTP {r.codigo}")

    r = http("POST", f"/reservas/{rid}/pagos/", tc, {"tipo_pago": "Anticipo", "metodo_pago": "Webpay"})
    r2 = http("GET", f"/reservas/{rid}/", tc)
    inf.caso("SIS-12", "Pago del anticipo (simulador Webpay) confirma la reserva",
             r.codigo == 201 and r2.cuerpo.get("estado") == "Confirmado",
             f"pago HTTP {r.codigo} · estado {r2.cuerpo.get('estado')} · saldo ${float(r2.cuerpo.get('saldo_pendiente', 0) or 0):,.0f}")
    r = http("GET", "/reservas/", tc)
    inf.caso("SIS-13", "La reserva aparece en «Mis reservas»",
             r.codigo == 200 and any(x.get("id_reserva") == rid for x in lista(r.cuerpo)), f"HTTP {r.codigo}")

    r = http("POST", "/consultas/", tc, {"asunto": f"RES-{rid:04d}: consulta de prueba de sistema",
                                         "mensaje": "¿Puedo llegar 30 minutos antes para decorar?"})
    id_consulta = r.cuerpo.get("id_consulta") if isinstance(r.cuerpo, dict) else None
    inf.caso("SIS-14", "El cliente envía una consulta desde la web", r.codigo == 201, f"HTTP {r.codigo}")

    # ---------------- 3. Administrador ----------------
    print("\n  3) Administrador: panel, consultas, control de acceso y bitácora")
    ta, nota = iniciar_sesion(ADMIN)
    inf.caso("SIS-15", "Inicio de sesión del administrador", bool(ta), nota)
    if not ta:
        inf.terminar()
    r = http("GET", "/reservas/resumen/", ta)
    r2 = http("GET", f"/reservas/{rid}/", ta)
    inf.caso("SIS-16", "El panel muestra el resumen y la reserva del cliente",
             r.codigo == 200 and r2.codigo == 200, f"resumen HTTP {r.codigo} · reserva HTTP {r2.codigo}")

    if id_consulta:
        r = http("POST", f"/consultas/{id_consulta}/responder/", ta, {"respuesta": "Sí, desde 1 hora antes."})
        r2 = http("GET", f"/consultas/{id_consulta}/", tc)
        inf.caso("SIS-17", "El administrador responde y el cliente ve la respuesta",
                 r.codigo == 200 and r2.cuerpo.get("estado") == "Respondida", f"HTTP {r.codigo} · {r2.cuerpo.get('estado')}")

    r = http("POST", f"/reservas/{rid}/validar-acceso/", ta, {"tipo_registro": "Entrada"})
    inf.caso("SIS-18", "No se registra la entrada de un evento futuro (regla de 1 hora antes)",
             r.codigo == 409, f"HTTP {r.codigo}")

    if reloj_disponible():
        inicio = mover_evento(rid, 30)
        print(f"             (reloj simulado: el evento ahora empieza a las {inicio:%H:%M})")
        r = http("POST", f"/reservas/{rid}/validar-acceso/", ta, {"tipo_registro": "Entrada"})
        r2 = http("GET", f"/reservas/{rid}/", ta)
        inf.caso("SIS-19", "Check-in por QR: la reserva pasa a «En Proceso»",
                 r.codigo == 201 and r2.cuerpo.get("estado") == "En Proceso", f"HTTP {r.codigo} · {r2.cuerpo.get('estado')}")
        r = http("POST", "/incidencias/", ta, {"reserva": rid, "titulo": "Prueba de sistema",
                                               "descripcion": "Incidencia registrada por la prueba de sistema."})
        inf.caso("SIS-20", "Registro de incidencia en la bitácora durante el evento", r.codigo == 201, f"HTTP {r.codigo}")

        inicio = mover_evento(rid, -(5 * 60 + 30))
        print(f"             (reloj simulado: el evento terminó hace 30 minutos)")
        r = http("POST", f"/reservas/{rid}/validar-acceso/", ta, {"tipo_registro": "Salida"})
        minutos = int(r.cuerpo.get("minutos_excedidos", 0) or 0) if isinstance(r.cuerpo, dict) else 0
        inf.caso("SIS-21", "Check-out: reserva «Finalizado» y cálculo de sobretiempo",
                 r.codigo == 201 and 29 <= minutos <= 32,
                 f"HTTP {r.codigo} · {minutos} min excedidos · cargo ${float(r.cuerpo.get('cargo_sobretiempo_propuesto', 0) or 0):,.0f}"
                 if isinstance(r.cuerpo, dict) else f"HTTP {r.codigo}")
        r = http("POST", "/incidencias/", ta, {"reserva": rid, "titulo": "Tardía", "descripcion": "No debe guardarse."})
        inf.caso("SIS-22", "La bitácora queda en solo lectura al finalizar", r.codigo == 400, f"HTTP {r.codigo}")
    else:
        inf.caso("SIS-19", "Check-in / incidencias / check-out", False,
                 "Omitido: requiere ejecutarse dentro de carpay_backend para simular el reloj", advertencia=True)

    r = http("GET", "/reservas/exportar/?formato=xlsx", ta, cabeceras={"Accept": "*/*"})
    tipo = r.cabeceras.get("Content-Type", "") if r.cabeceras else ""
    inf.caso("SIS-23", "Exportación de reservas a Excel", r.codigo == 200 and "spreadsheet" in tipo,
             f"HTTP {r.codigo} · {tipo} · {len(r.crudo)} bytes")

    # ---------------- 4. Cierre de sesión ----------------
    r = http("POST", "/auth/login/", datos={"correo": cred[0], "password": cred[1]})
    if r.codigo == 200:
        refresh = r.cuerpo["refresh"]
        r = http("POST", "/auth/logout/", r.cuerpo["access"], {"refresh": refresh})
        r2 = http("POST", "/auth/refresh/", datos={"refresh": refresh})
        inf.caso("SIS-24", "Cerrar sesión invalida el token de renovación",
                 r.codigo == 204 and r2.codigo == 401, f"logout HTTP {r.codigo} · refresh HTTP {r2.codigo}")
    inf.terminar()


if __name__ == "__main__":
    main()

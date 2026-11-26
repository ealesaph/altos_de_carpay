"""PRUEBA DE RENDIMIENTO · Tiempos de respuesta (RNF-03: responder en menos de 2 segundos).

Mide, entrando por Nginx como lo hace el navegador, el tiempo de respuesta de la web y de los
endpoints más usados. Solo hace lecturas (no modifica datos).

Ejecutar:
    docker exec carpay_backend python pruebas/rendimiento/tiempos_respuesta.py
Variables opcionales: PRUEBAS_REPETICIONES (por defecto 20), PRUEBAS_UMBRAL_MS (por defecto 2000)
"""
import os
import statistics
import sys

sys.path.insert(0, "/app")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from pruebas._comun import ADMIN, NEGRITA, FIN, WEB, Informe, http, iniciar_sesion, lista, token_de_servidor  # noqa: E402

REPETICIONES = int(os.environ.get("PRUEBAS_REPETICIONES", "20"))
UMBRAL_MS = float(os.environ.get("PRUEBAS_UMBRAL_MS", "2000"))


def percentil(valores, p):
    v = sorted(valores)
    return v[min(len(v) - 1, int(round(p / 100 * (len(v) - 1))))]


def main():
    inf = Informe(f"PRUEBA DE RENDIMIENTO · tiempos de respuesta ({REPETICIONES} solicitudes por recurso, "
                  f"umbral {UMBRAL_MS:.0f} ms)")
    token = token_de_servidor(ADMIN[0]) or iniciar_sesion(ADMIN)[0]
    reservas = lista(http("GET", "/reservas/", token).cuerpo) if token else []
    id_reserva = reservas[0]["id_reserva"] if reservas else None

    recursos = [
        ("Portada web (HTML)", WEB + "/", None),
        ("Catálogo de espacios", "/sectores/", token),
        ("Servicios extra", "/servicios/", token),
        ("Listado de reservas (panel)", "/reservas/", token),
        ("Próximas reservas", "/reservas/?vista=proximas", token),
        ("Resumen del panel", "/reservas/resumen/", token),
        ("Consultas (tickets)", "/consultas/", token),
    ]
    if id_reserva:
        recursos.append((f"Detalle de reserva N° {id_reserva}", f"/reservas/{id_reserva}/", token))

    print(f"\n  {NEGRITA}{'Recurso':34} {'mín':>7} {'prom':>7} {'p95':>7} {'máx':>7}  errores{FIN}")
    filas = []
    for nombre, ruta, tok in recursos:
        tiempos, errores = [], 0
        for _ in range(REPETICIONES):
            r = http("GET", ruta, tok, cabeceras={"Accept": "text/html"} if ruta.startswith("http") else None)
            tiempos.append(r.ms)
            errores += r.codigo != 200
        fila = (nombre, min(tiempos), statistics.mean(tiempos), percentil(tiempos, 95), max(tiempos), errores)
        filas.append(fila)
        print(f"  {fila[0]:34} {fila[1]:6.0f}ms {fila[2]:6.0f}ms {fila[3]:6.0f}ms {fila[4]:6.0f}ms  {fila[5]}")
    print()
    for i, (nombre, _, prom, p95, _, errores) in enumerate(filas, 1):
        inf.caso(f"REN-{i:02d}", f"{nombre}: p95 bajo {UMBRAL_MS:.0f} ms y sin errores",
                 p95 < UMBRAL_MS and errores == 0, f"promedio {prom:.0f} ms · p95 {p95:.0f} ms · errores {errores}")
    inf.terminar()


if __name__ == "__main__":
    main()

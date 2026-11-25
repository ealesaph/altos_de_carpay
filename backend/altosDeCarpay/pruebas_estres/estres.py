"""Prueba de estrés de la API REST de Altos de Carpay.

Simula usuarios concurrentes (hilos) que consultan los endpoints que usa la app móvil.
Cada etapa aumenta la concurrencia. Por cada etapa se mide: solicitudes, errores,
rendimiento (solicitudes por segundo) y percentiles de tiempo de respuesta.
Solo se usan operaciones de lectura para no alterar los datos.
"""
import json, os, statistics, sys, time, urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, '/app')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'altosDeCarpay.settings')
import django; django.setup()
from rest_framework_simplejwt.tokens import RefreshToken
from usuariosApp.models import Usuario
from reservasApp.models import Reserva

BASE = os.environ.get('ESTRES_BASE', 'http://127.0.0.1:8001/api/v1')
ETAPAS = [int(x) for x in os.environ.get('ESTRES_ETAPAS', '10,25,50,100,200').split(',')]
SOLICITUDES_POR_USUARIO = int(os.environ.get('ESTRES_POR_USUARIO', '10'))

admin = Usuario.objects.filter(rol='Admin', estado='Activo').order_by('id_usuario').first()
TOKEN = str(RefreshToken.for_user(admin).access_token)   # token generado en el servidor, sin contraseñas
id_res = Reserva.objects.order_by('-id_reserva').values_list('id_reserva', flat=True).first()
ENDPOINTS = [
    ('/sectores/', False), ('/servicios/', False),
    ('/reservas/?vista=proximas', True), ('/reservas/resumen/', True), (f'/reservas/{id_res}/', True),
]

def pedir(i):
    ruta, auth = ENDPOINTS[i % len(ENDPOINTS)]
    req = urllib.request.Request(BASE + ruta)
    if auth:
        req.add_header('Authorization', 'Bearer ' + TOKEN)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            r.read(); cod = r.status
    except urllib.error.HTTPError as e:
        cod = e.code
    except Exception:
        cod = 0
    return cod, (time.perf_counter() - t0) * 1000

def usuario(u):
    return [pedir(u * SOLICITUDES_POR_USUARIO + k) for k in range(SOLICITUDES_POR_USUARIO)]

def pct(v, p):
    v = sorted(v); return v[min(len(v) - 1, int(round(p / 100 * (len(v) - 1))))]

print('=' * 92)
print(f' PRUEBA DE ESTRÉS · API Altos de Carpay · {BASE}')
print(f' Servidor: Gunicorn 3 workers + PostgreSQL 16 · {SOLICITUDES_POR_USUARIO} solicitudes por usuario · 5 endpoints de la app')
print('=' * 92)
print(f"{'Usuarios':>8} {'Solicit.':>8} {'Errores':>8} {'% error':>8} {'Sol/s':>8} {'Prom ms':>8} {'p50 ms':>8} {'p95 ms':>8} {'p99 ms':>8} {'Máx ms':>8}")
resultados = []
for n in ETAPAS:
    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=n) as ex:
        datos = [x for lote in ex.map(usuario, range(n)) for x in lote]
    dur = time.perf_counter() - t0
    tiempos = [t for _, t in datos]
    err = sum(1 for c, _ in datos if c != 200)
    fila = dict(usuarios=n, solicitudes=len(datos), errores=err, pct_error=round(100 * err / len(datos), 2),
                rps=round(len(datos) / dur, 1), prom=round(statistics.mean(tiempos)), p50=round(pct(tiempos, 50)),
                p95=round(pct(tiempos, 95)), p99=round(pct(tiempos, 99)), max=round(max(tiempos)),
                codigos=sorted({c for c, _ in datos}))
    resultados.append(fila)
    print(f"{n:>8} {fila['solicitudes']:>8} {err:>8} {fila['pct_error']:>7}% {fila['rps']:>8} {fila['prom']:>8} {fila['p50']:>8} {fila['p95']:>8} {fila['p99']:>8} {fila['max']:>8}")
    time.sleep(2)
os.makedirs('/app/_evidencia', exist_ok=True)
json.dump(resultados, open('/app/_evidencia/estres.json', 'w'), indent=1)
tot = sum(r['solicitudes'] for r in resultados); terr = sum(r['errores'] for r in resultados)
print('-' * 92)
print(f' Total: {tot} solicitudes, {terr} errores. Resultados guardados en _evidencia/estres.json')

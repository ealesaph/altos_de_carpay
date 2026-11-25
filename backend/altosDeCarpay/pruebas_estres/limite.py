"""Prueba de protección: ráfaga de solicitudes anónimas contra el servidor normal (puerto 8000).
Verifica que el límite de 60 solicitudes por minuto por IP responda HTTP 429 ante una avalancha."""
import time, urllib.request, urllib.error, collections
from concurrent.futures import ThreadPoolExecutor
URL = 'http://127.0.0.1:8000/api/v1/sectores/'
def pedir(_):
    try:
        with urllib.request.urlopen(URL, timeout=15) as r: return r.status
    except urllib.error.HTTPError as e: return e.code
    except Exception: return 0
t0 = time.time()
with ThreadPoolExecutor(max_workers=20) as ex: cod = list(ex.map(pedir, range(150)))
c = collections.Counter(cod)
print('=' * 92)
print(f' PRUEBA DE PROTECCIÓN ANTE AVALANCHA · 150 solicitudes anónimas en {time.time()-t0:.1f} s · límite 60/min por IP')
print(f'   HTTP 200 (atendidas): {c.get(200,0)}   HTTP 429 (bloqueadas por el límite): {c.get(429,0)}   Otros: {sum(v for k,v in c.items() if k not in (200,429))}')
print('   Resultado:', 'APROBADO: el servidor limita la avalancha sin caerse' if c.get(429,0) > 0 and c.get(0,0) == 0 else 'REVISAR')
print('=' * 92)

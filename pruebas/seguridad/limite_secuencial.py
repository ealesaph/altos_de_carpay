"""Diagnóstico: las mismas solicitudes anónimas, pero de a una (sin concurrencia)."""
import urllib.request, urllib.error, collections, time
URL = 'http://127.0.0.1:8000/api/v1/servicios/'
cod = []
for _ in range(80):
    try:
        with urllib.request.urlopen(URL, timeout=15) as r: cod.append(r.status)
    except urllib.error.HTTPError as e: cod.append(e.code)
c = collections.Counter(cod)
primer = cod.index(429) + 1 if 429 in cod else None
print(f' DIAGNÓSTICO SECUENCIAL · 80 solicitudes de a una: HTTP 200={c.get(200,0)}  HTTP 429={c.get(429,0)}  primer 429 en la solicitud N° {primer}')

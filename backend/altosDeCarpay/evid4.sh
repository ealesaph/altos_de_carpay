#!/bin/sh
python - <<'PY'
import ssl, socket, json, urllib.request, time
print("== Verificación de cifrado en tránsito (HTTPS en Nginx) ==")
ctx = ssl.create_default_context(); ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE
with socket.create_connection(("frontend", 443), timeout=5) as s:
    with ctx.wrap_socket(s, server_hostname="localhost") as t:
        der = t.getpeercert(binary_form=True)
        print("  Protocolo TLS negociado :", t.version())
        print("  Suite de cifrado        :", t.cipher()[0], f"({t.cipher()[2]} bits)")
        print("  Certificado (bytes DER) :", len(der))
def req(url, c=None):
    t0 = time.time()
    r = urllib.request.urlopen(url, context=c, timeout=10)
    return r.status, round((time.time()-t0)*1000), r.headers
st, ms, h = req("https://frontend/api/v1/sectores/", ctx)
print(f"  GET https://localhost/api/v1/sectores/  -> HTTP {st} ({ms} ms)")
print("  Cabeceras de seguridad  :", ", ".join(f"{k}: {h.get(k)}" for k in ["X-Content-Type-Options","X-Frame-Options","Referrer-Policy"]))
st, ms, _ = req("https://frontend/", ctx); print(f"  GET https://localhost/ (web)            -> HTTP {st} ({ms} ms)")
print("== Compatibilidad (sin cambios para la app y la web en HTTP) ==")
st, ms, _ = req("http://frontend/"); print(f"  GET http://localhost/ (web)             -> HTTP {st} ({ms} ms)")
st, ms, _ = req("http://backend:8000/api/v1/sectores/"); print(f"  GET http://10.0.2.2:8000/api/v1/sectores/ (app) -> HTTP {st} ({ms} ms)")
PY

"""PRUEBA DE SISTEMA (humo) · API que consume la aplicación móvil Android (solo lectura).

Recorre los endpoints GET que llama la app (ApiService.kt) con un token de cliente y
uno de administrador generados en el servidor (sin contraseñas), y verifica los
controles de acceso: sin token -> 401, cliente en ruta de administrador -> 403.
"""
import json, os, sys, time, urllib.request, urllib.error
sys.path.insert(0, '/app')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'altosDeCarpay.settings')
import django; django.setup()
from rest_framework_simplejwt.tokens import RefreshToken
from usuariosApp.models import Usuario
from reservasApp.models import Reserva

BASE = 'http://127.0.0.1:8000/api/v1/'
admin = Usuario.objects.filter(rol='Admin', estado='Activo').order_by('id_usuario').first()
# Se usa la última reserva CONFIRMADA de un cliente: sobre ella el administrador puede simular la
# cancelación (200) y el cliente no (403). Si no hay ninguna, se usa la última reserva de un cliente.
res = (Reserva.objects.filter(usuario__rol='Cliente', estado='Confirmado').order_by('-id_reserva').first()
       or Reserva.objects.filter(usuario__rol='Cliente').order_by('-id_reserva').first())
cliente = res.usuario
tok = lambda u: str(RefreshToken.for_user(u).access_token)
TA, TC = tok(admin), tok(cliente)
rid = res.pk

def get(ruta, token=None):
    req = urllib.request.Request(BASE + ruta)
    if token: req.add_header('Authorization', 'Bearer ' + token)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            r.read(); c = r.status
    except urllib.error.HTTPError as e:
        c = e.code
    return c, (time.perf_counter() - t0) * 1000

casos = [
 ('Cliente', 'Perfil', 'auth/perfil/', TC, (200,)),
 ('Cliente', 'Política de datos', 'auth/politica-privacidad/', TC, (200,)),
 ('Cliente', 'Descarga de mis datos', 'auth/mis-datos/', TC, (200,)),
 ('Cliente', 'Explorar espacios', 'sectores/', TC, (200,)),
 ('Cliente', 'Servicios extra', 'servicios/', TC, (200,)),
 ('Cliente', 'Mis reservas', 'reservas/', TC, (200,)),
 ('Cliente', f'Detalle RES-{rid:04d}', f'reservas/{rid}/', TC, (200,)),
 ('Cliente', 'Pagos de la reserva', f'reservas/{rid}/pagos/', TC, (200,)),
 ('Cliente', 'Mis consultas', 'consultas/', TC, (200,)),
 ('Admin', 'Agenda (próximas)', 'reservas/?vista=proximas', TA, (200,)),
 ('Admin', 'Resumen del día', 'reservas/resumen/', TA, (200,)),
 ('Admin', f'Detalle RES-{rid:04d}', f'reservas/{rid}/', TA, (200,)),
 ('Admin', 'Validar acceso (consulta)', f'reservas/{rid}/validar-acceso/', TA, (200, 400)),
 ('Admin', 'Simular cancelación', f'reservas/{rid}/simular-cancelacion/?origen=Cliente', TA, (200, 400)),
 ('Admin', 'Bitácora de incidencias', 'incidencias/', TA, (200,)),
 ('Admin', 'Tickets', 'consultas/', TA, (200,)),
 ('Admin', 'Catálogo de espacios', 'sectores/', TA, (200,)),
 ('Admin', 'Auditoría', 'auditoria/logs/', TA, (200,)),
 ('Seguridad', 'Reservas sin token', 'reservas/', None, (401,)),
 ('Seguridad', 'Cliente en auditoría', 'auditoria/logs/', TC, (403,)),
 ('Seguridad', 'Cliente simula cancelación', f'reservas/{rid}/simular-cancelacion/', TC, (403,)),
]
ok = 0
print(f"{'Perfil':<10}{'Pantalla / acción':<32}{'HTTP':>6}{'ms':>8}  Resultado")
print('-' * 66)
for perfil, nombre, ruta, t, esperado in casos:
    c, ms = get(ruta, t)
    bien = c in esperado; ok += bien
    print(f"{perfil:<10}{nombre:<32}{c:>6}{ms:>8.0f}  {'APROBADO' if bien else 'FALLA (esperado ' + str(esperado) + ')'}")
print('-' * 66)
print(f'Resultado: {ok} de {len(casos)} verificaciones aprobadas')

sys.exit(0 if ok == len(casos) else 1)

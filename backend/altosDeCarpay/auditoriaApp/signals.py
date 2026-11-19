import json
from decimal import Decimal
from .contexto import obtener_usuario_actual
from .models import LogModificacion

MODELOS_AUDITADOS = (
    "reservasApp.Sector",
    "reservasApp.Servicio",
    "reservasApp.Reserva",
    "reservasApp.Pago",
    "reservasApp.ValidacionAcceso",
    "reservasApp.BitacoraIncidencia",
)


def _datos(instancia):
    datos = {}
    for campo in instancia._meta.concrete_fields:
        valor = getattr(instancia, campo.attname)
        if isinstance(valor, Decimal):
            valor = str(valor.quantize(Decimal("0.01")))
        elif valor is not None and not isinstance(valor, (int, bool)):
            valor = str(valor)
        datos[campo.attname] = valor
    return datos


def _json(valor):
    return json.dumps(valor, ensure_ascii=False)


def guardar_estado_previo(sender, instance, **kwargs):
    previo = sender.objects.filter(pk=instance.pk).first() if instance.pk else None
    instance._estado_previo = _datos(previo) if previo else None


def registrar_guardado(sender, instance, created, **kwargs):
    nuevo = _datos(instance)
    previo = getattr(instance, "_estado_previo", None)
    if created or previo is None:
        valor_previo, valor_nuevo = None, nuevo
    else:
        cambios = {k: v for k, v in nuevo.items() if previo.get(k) != v}
        if not cambios:
            return
        valor_previo, valor_nuevo = {k: previo.get(k) for k in cambios}, cambios
    LogModificacion.objects.create(
        usuario=obtener_usuario_actual(),
        tabla_modificada=sender._meta.db_table,
        id_registro=instance.pk,
        valor_previo=_json(valor_previo) if valor_previo is not None else None,
        valor_nuevo=_json(valor_nuevo),
    )


def registrar_eliminacion(sender, instance, **kwargs):
    LogModificacion.objects.create(
        usuario=obtener_usuario_actual(),
        tabla_modificada=sender._meta.db_table,
        id_registro=instance.pk,
        valor_previo=_json(_datos(instance)),
        valor_nuevo=None,
    )
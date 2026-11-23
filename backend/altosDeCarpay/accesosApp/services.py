from django.db import transaction
from rest_framework.exceptions import ValidationError
from altosDeCarpayApp.excepciones import ConflictoDeNegocio
from reservasApp.models import Reserva
from .models import ValidacionAcceso


@transaction.atomic
def registrar_acceso(*, token_qr, tipo_registro, validador):
    try:
        reserva = Reserva.objects.select_for_update().get(token_qr=token_qr)
    except Reserva.DoesNotExist:
        raise ValidationError({"token_qr": "Código QR no válido."})

    if tipo_registro == ValidacionAcceso.Tipo.ENTRADA:
        if reserva.estado != Reserva.Estado.CONFIRMADO:
            raise ConflictoDeNegocio("Solo las reservas confirmadas pueden registrar ingreso.")
        reserva.estado = Reserva.Estado.EN_PROCESO
        reserva.save(update_fields=["estado"])
    else:
        if reserva.estado != Reserva.Estado.EN_PROCESO:
            raise ConflictoDeNegocio("No existe un ingreso registrado para esta reserva.")

    validacion = ValidacionAcceso.objects.create(
        reserva=reserva, token_qr=token_qr, tipo_registro=tipo_registro, validado_por=validador,
    )
    minutos_excedidos = 0
    if tipo_registro == ValidacionAcceso.Tipo.SALIDA:
        exceso = validacion.fecha_hora - reserva.fin
        minutos_excedidos = max(0, int(exceso.total_seconds() // 60))
    return validacion, minutos_excedidos

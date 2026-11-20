import logging
import uuid
from datetime import datetime, timedelta
from decimal import Decimal
from django.conf import settings
from django.db import transaction
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied, ValidationError
from altosDeCarpayApp.excepciones import ConflictoDeNegocio
from .emails import enviar_correo_reserva
from .models import Pago, Reserva, ReservaSector, ReservaServicio, Sector, ValidacionAcceso

logger = logging.getLogger("reservas")

ESTADOS_QUE_BLOQUEAN = (Reserva.Estado.PENDIENTE, Reserva.Estado.CONFIRMADO, Reserva.Estado.EN_PROCESO)

TRANSICIONES_PERMITIDAS = {
    Reserva.Estado.PENDIENTE: (Reserva.Estado.CONFIRMADO, Reserva.Estado.CANCELADO),
    Reserva.Estado.CONFIRMADO: (Reserva.Estado.EN_PROCESO, Reserva.Estado.CANCELADO),
    Reserva.Estado.EN_PROCESO: (Reserva.Estado.FINALIZADO,),
    Reserva.Estado.FINALIZADO: (),
    Reserva.Estado.CANCELADO: (),
}

def _intervalo(fecha, hora_inicio, horas):
    inicio = timezone.make_aware(datetime.combine(fecha, hora_inicio))
    return inicio, inicio + timedelta(hours=horas)

def sector_ocupado(sector, inicio, fin):
    candidatas = ReservaSector.objects.select_related("reserva").filter(
        sector=sector,
        reserva__estado__in=ESTADOS_QUE_BLOQUEAN,
        reserva__fecha__range=(inicio.date() - timedelta(days=1), fin.date()),
    )
    for existente in candidatas:
        reserva = existente.reserva
        if reserva.estado == Reserva.Estado.PENDIENTE and reserva.plazo_anticipo_vencido:
            continue
        if existente.inicio < fin and existente.fin > inicio:
            return True
    return False

@transaction.atomic
def crear_reserva(*, usuario, fecha, hora_inicio, sectores, servicios):
    horas_totales = max(item["horas"] for item in sectores)
    inicio, fin = _intervalo(fecha, hora_inicio, horas_totales)
    if inicio <= timezone.now():
        raise ValidationError({"fecha": "La reserva debe ser para una fecha y hora futura."})
    
    ids = sorted(item["sector"].pk for item in sectores)
    bloqueados = {s.pk: s for s in Sector.objects.select_for_update().filter(pk__in=ids).order_by("pk")}

    total = Decimal("0.00")
    lineas_sector = []
    for item in sectores:
        sector = bloqueados[item["sector"].pk]
        if sector.estado != Sector.Estado.DISPONIBLE:
            raise ConflictoDeNegocio(f"El sector «{sector.nombre}» no está disponible.")
        _, fin_sector = _intervalo(fecha, hora_inicio, item["horas"])
        if sector_ocupado(sector, inicio, fin_sector):
            raise ConflictoDeNegocio(f"El sector «{sector.nombre}» ya está reservado en ese horario.")
        total += sector.valor_hora * item["horas"]
        lineas_sector.append((sector, item["horas"]))
    reserva = Reserva.objects.create(
        usuario=usuario, fecha=fecha, hora_inicio=hora_inicio, horas_reservadas=horas_totales,
        monto_total=Decimal("0.00"), condiciones_aceptadas_en=timezone.now(),
    )
    ReservaSector.objects.bulk_create([
        ReservaSector(reserva=reserva, sector=sector, horas=horas, valor_hora=sector.valor_hora)
        for sector, horas in lineas_sector
    ])
    lineas_servicio = []
    for item in servicios:
        servicio = item["servicio"]
        total += servicio.valor * item["cantidad"]
        lineas_servicio.append(ReservaServicio(
            reserva=reserva, servicio=servicio, cantidad=item["cantidad"], valor_unitario=servicio.valor,
        ))
    ReservaServicio.objects.bulk_create(lineas_servicio)

    reserva.monto_total = total
    reserva.save(update_fields=["monto_total"])
    return reserva

def simular_pasarela(*, forzar_rechazo=False):
    rechazar = forzar_rechazo and settings.PAGOS_SIMULADOR_PERMITIR_RECHAZO
    estado = Pago.Estado.RECHAZADO if rechazar else Pago.Estado.APROBADO
    return estado, f"SIM-{uuid.uuid4().hex[:20].upper()}"


def _monto_a_pagar(reserva, tipo):
    pagado = reserva.monto_pagado
    if tipo == Pago.Tipo.ANTICIPO:
        if pagado > 0:
            raise ConflictoDeNegocio("El anticipo ya fue pagado. Utilice el pago del saldo pendiente.")
        return reserva.monto_anticipo
    if tipo == Pago.Tipo.SALDO:
        if not reserva.anticipo_pagado:
            raise ConflictoDeNegocio("Primero debe pagar el anticipo.")
        if reserva.saldo_pendiente <= 0:
            raise ConflictoDeNegocio("La reserva no tiene saldo pendiente.")
        return reserva.saldo_pendiente
    if pagado > 0:
        raise ConflictoDeNegocio("La reserva ya tiene pagos. Utilice el pago del saldo pendiente.")
    return reserva.monto_total

@transaction.atomic
def registrar_pago(*, reserva, usuario, tipo_pago, metodo_pago, simular_rechazo=False):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)  # Evita pagos duplicados
    if reserva.estado == Reserva.Estado.CANCELADO:
        raise ConflictoDeNegocio("La reserva está cancelada y no admite pagos.")
    if reserva.estado == Reserva.Estado.PENDIENTE and reserva.plazo_anticipo_vencido and not reserva.anticipo_pagado:
        raise ConflictoDeNegocio("El plazo para pagar el anticipo venció; la reserva será cancelada.")
    if metodo_pago != Pago.Metodo.WEBPAY and not usuario.es_admin:
        raise PermissionDenied("Solo un administrador puede registrar pagos por transferencia o efectivo.")

    monto = _monto_a_pagar(reserva, tipo_pago)
    if metodo_pago == Pago.Metodo.WEBPAY:
        estado, transaccion = simular_pasarela(forzar_rechazo=simular_rechazo)
    else:
        estado, transaccion = Pago.Estado.APROBADO, None

    pago = Pago.objects.create(
        reserva=reserva, monto=monto, tipo_pago=tipo_pago, metodo_pago=metodo_pago,
        transaccion_id=transaccion, estado_pago=estado,
    )
    if estado == Pago.Estado.APROBADO:
        if reserva.estado == Reserva.Estado.PENDIENTE and reserva.anticipo_pagado:
            reserva.estado = Reserva.Estado.CONFIRMADO
            reserva.save(update_fields=["estado"])
            transaction.on_commit(lambda: enviar_correo_reserva(reserva.pk, motivo="confirmacion"))
        elif reserva.estado in (Reserva.Estado.CONFIRMADO, Reserva.Estado.EN_PROCESO, Reserva.Estado.FINALIZADO):
            transaction.on_commit(lambda: enviar_correo_reserva(reserva.pk, motivo="pago"))
    return pago


def enviar_correo_confirmacion(reserva_id):
    return enviar_correo_reserva(reserva_id, motivo="confirmacion", fail_silently=True)

@transaction.atomic
def cancelar_reserva(*, reserva, solicitante):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    if reserva.estado in (Reserva.Estado.CANCELADO, Reserva.Estado.EN_PROCESO):
        raise ConflictoDeNegocio(f"Una reserva en estado «{reserva.estado}» no puede cancelarse.")
    if not solicitante.es_admin and reserva.estado != Reserva.Estado.PENDIENTE:
        raise PermissionDenied("Para cancelar una reserva confirmada contacte al administrador.")
    reserva.estado = Reserva.Estado.CANCELADO
    reserva.save(update_fields=["estado"])
    return reserva

@transaction.atomic
def cambiar_estado(*, reserva, nuevo_estado):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    if nuevo_estado not in TRANSICIONES_PERMITIDAS[reserva.estado]:
        raise ConflictoDeNegocio(f"No se puede pasar de «{reserva.estado}» a «{nuevo_estado}».")
    reserva.estado = nuevo_estado
    reserva.save(update_fields=["estado"])
    return reserva


@transaction.atomic
def registrar_validacion_acceso(*, reserva, tipo_registro, validado_por):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    minutos_excedidos = 0
    cargo_propuesto = Decimal("0.00")

    if tipo_registro == ValidacionAcceso.TipoRegistro.ENTRADA:
        if reserva.estado != Reserva.Estado.CONFIRMADO:
            raise ConflictoDeNegocio(f"Solo puede registrar entrada para reservas confirmadas (estado actual: {reserva.estado}).")
        reserva.estado = Reserva.Estado.EN_PROCESO
        reserva.save(update_fields=["estado"])
    elif tipo_registro == ValidacionAcceso.TipoRegistro.SALIDA:
        if reserva.estado != Reserva.Estado.EN_PROCESO:
            raise ConflictoDeNegocio(f"Solo puede registrar salida para reservas en proceso (estado actual: {reserva.estado}).")
        ahora = timezone.now()
        if ahora > reserva.fin:
            minutos_excedidos = int((ahora - reserva.fin).total_seconds() // 60)
            cargo_propuesto = Decimal(minutos_excedidos) * Decimal("500.00")
        reserva.estado = Reserva.Estado.FINALIZADO
        reserva.save(update_fields=["estado"])
    else:
        raise ValidationError({"tipo_registro": "Tipo de registro inválido."})

    return ValidacionAcceso.objects.create(
        reserva=reserva,
        token_qr=reserva.token_qr,
        tipo_registro=tipo_registro,
        validado_por=validado_por,
        minutos_excedidos=minutos_excedidos,
        cargo_sobretiempo_propuesto=cargo_propuesto,
    )

def cancelar_reservas_impagas():
    limite = timezone.now() - timedelta(hours=settings.PLAZO_PAGO_ANTICIPO_HORAS)
    ids = Reserva.objects.filter(estado=Reserva.Estado.PENDIENTE, fecha_creacion__lt=limite) \
        .values_list("pk", flat=True)
    canceladas = 0
    for pk in list(ids):
        with transaction.atomic():
            reserva = Reserva.objects.select_for_update().get(pk=pk)
            if reserva.estado == Reserva.Estado.PENDIENTE and not reserva.anticipo_pagado:
                reserva.estado = Reserva.Estado.CANCELADO
                reserva.save(update_fields=["estado"])
                canceladas += 1
    return canceladas
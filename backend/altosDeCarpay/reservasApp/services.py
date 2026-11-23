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

# La cancelación no figura aquí: pasa siempre por cancelar_reserva(), que calcula la devolución.
TRANSICIONES_PERMITIDAS = {
    Reserva.Estado.PENDIENTE: (Reserva.Estado.CONFIRMADO,),
    Reserva.Estado.CONFIRMADO: (Reserva.Estado.EN_PROCESO, Reserva.Estado.NO_SE_PRESENTO),
    Reserva.Estado.EN_PROCESO: (Reserva.Estado.FINALIZADO,),
    Reserva.Estado.FINALIZADO: (),
    Reserva.Estado.CANCELADO: (),
    Reserva.Estado.NO_SE_PRESENTO: (),
}


def validar_bloque_horario(hora_inicio, horas):
    """Solo se arrienda en horas completas dentro de un bloque: diurno 13–18 o vespertino 19–00."""
    if hora_inicio.minute or hora_inicio.second:
        raise ValidationError({"hora_inicio": "La reserva debe comenzar en una hora en punto (por ejemplo, 13:00)."})
    inicio = hora_inicio.hour
    for desde, hasta in settings.BLOQUES_HORARIOS:
        if desde <= inicio < hasta:
            if inicio + horas > hasta:
                raise ValidationError({"horas": (
                    f"El bloque de {desde:02d}:00 a {hasta % 24:02d}:00 permite como máximo "
                    f"{hasta - inicio} hora(s) desde las {inicio:02d}:00."
                )})
            return
    bloques = " y ".join(f"{d:02d}:00–{h % 24:02d}:00" for d, h in settings.BLOQUES_HORARIOS)
    raise ValidationError({"hora_inicio": f"El recinto se arrienda solo en los bloques {bloques}."})


def ventana_checkin(reserva):
    """El acceso se registra el mismo día del evento, desde una hora antes del inicio hasta el término."""
    return reserva.inicio - timedelta(minutes=settings.CHECKIN_MINUTOS_ANTES), reserva.fin


def _validar_ventana_checkin(reserva):
    desde, hasta = ventana_checkin(reserva)
    ahora = timezone.now()
    if ahora < desde:
        raise ConflictoDeNegocio(
            f"El acceso de la reserva N° {reserva.pk} se habilita el {timezone.localtime(desde):%d/%m/%Y} "
            f"desde las {timezone.localtime(desde):%H:%M}."
        )
    if ahora > hasta:
        raise ConflictoDeNegocio(f"El horario de la reserva N° {reserva.pk} ya terminó; no se puede registrar la entrada.")

def _intervalo(fecha, hora_inicio, horas):
    inicio = timezone.make_aware(datetime.combine(fecha, hora_inicio))
    return inicio, inicio + timedelta(hours=horas)

def sector_ocupado(sector, inicio, fin, excluir_reserva=None):
    candidatas = ReservaSector.objects.select_related("reserva").filter(
        sector=sector,
        reserva__estado__in=ESTADOS_QUE_BLOQUEAN,
        reserva__fecha__range=(inicio.date() - timedelta(days=1), fin.date()),
    )
    if excluir_reserva is not None:
        candidatas = candidatas.exclude(reserva_id=excluir_reserva)
    for existente in candidatas:
        reserva = existente.reserva
        if reserva.estado == Reserva.Estado.PENDIENTE and reserva.plazo_anticipo_vencido:
            continue
        if existente.inicio < fin and existente.fin > inicio:
            return True
    return False

def _preparar_lineas(*, fecha, hora_inicio, sectores, excluir_reserva=None):
    horas_totales = max(item["horas"] for item in sectores)
    validar_bloque_horario(hora_inicio, horas_totales)
    inicio, fin = _intervalo(fecha, hora_inicio, horas_totales)
    if inicio <= timezone.now():
        raise ValidationError({"fecha": "La reserva debe ser para una fecha y hora futura."})

    ids = sorted(item["sector"].pk for item in sectores)
    bloqueados = {s.pk: s for s in Sector.objects.select_for_update().filter(pk__in=ids).order_by("pk")}

    lineas_sector = []
    for item in sectores:
        sector = bloqueados[item["sector"].pk]
        if sector.estado != Sector.Estado.DISPONIBLE:
            raise ConflictoDeNegocio(f"El sector «{sector.nombre}» no está disponible.")
        _, fin_sector = _intervalo(fecha, hora_inicio, item["horas"])
        if sector_ocupado(sector, inicio, fin_sector, excluir_reserva=excluir_reserva):
            raise ConflictoDeNegocio(f"El sector «{sector.nombre}» ya está reservado en ese horario.")
        lineas_sector.append((sector, item["horas"]))
    return horas_totales, lineas_sector


def _guardar_lineas(reserva, lineas_sector, servicios):
    total = Decimal("0.00")
    ReservaSector.objects.bulk_create([
        ReservaSector(reserva=reserva, sector=sector, horas=horas, valor_hora=sector.valor_hora)
        for sector, horas in lineas_sector
    ])
    total += sum((sector.valor_hora * horas for sector, horas in lineas_sector), Decimal("0.00"))
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


@transaction.atomic
def crear_reserva(*, usuario, fecha, hora_inicio, sectores, servicios):
    horas_totales, lineas_sector = _preparar_lineas(fecha=fecha, hora_inicio=hora_inicio, sectores=sectores)
    reserva = Reserva.objects.create(
        usuario=usuario, fecha=fecha, hora_inicio=hora_inicio, horas_reservadas=horas_totales,
        monto_total=Decimal("0.00"), condiciones_aceptadas_en=timezone.now(),
    )
    return _guardar_lineas(reserva, lineas_sector, servicios)


@transaction.atomic
def modificar_reserva(*, reserva, solicitante, fecha, hora_inicio, sectores, servicios):
    """«Editar selección»: cambia la misma reserva (sin crear otra) mientras no tenga pagos."""
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    if reserva.estado != Reserva.Estado.PENDIENTE:
        raise ConflictoDeNegocio("Solo se puede editar una reserva pendiente de pago.")
    if reserva.monto_pagado > 0:
        raise ConflictoDeNegocio("La reserva ya tiene pagos registrados; para cambiarla contacte al administrador.")
    if not solicitante.es_admin and reserva.usuario_id != solicitante.pk:
        raise PermissionDenied("No puede modificar reservas de otro usuario.")
    horas_totales, lineas_sector = _preparar_lineas(
        fecha=fecha, hora_inicio=hora_inicio, sectores=sectores, excluir_reserva=reserva.pk,
    )
    reserva.sectores_reservados.all().delete()
    reserva.servicios_reservados.all().delete()
    reserva.fecha, reserva.hora_inicio, reserva.horas_reservadas = fecha, hora_inicio, horas_totales
    reserva.save(update_fields=["fecha", "hora_inicio", "horas_reservadas"])
    return _guardar_lineas(reserva, lineas_sector, servicios)


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
    if reserva.estado in (Reserva.Estado.CANCELADO, Reserva.Estado.NO_SE_PRESENTO):
        raise ConflictoDeNegocio(f"La reserva está en estado «{reserva.estado}» y no admite pagos.")
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

def calcular_devolucion(reserva, origen, ahora=None):
    """Condiciones de arriendo del cliente:
    - Si cancela Altos de Carpay, se devuelve todo lo pagado.
    - Si cancela el arrendatario con más de 7 días de anticipación, se devuelve todo salvo el anticipo.
    - Si cancela el arrendatario con 7 días o menos, no se devuelve nada.
    """
    ahora = ahora or timezone.now()
    pagado = reserva.monto_pagado
    if origen == Reserva.OrigenCancelacion.RECINTO:
        return pagado, "Cancela Altos de Carpay: se devuelve el total pagado."
    if reserva.inicio - ahora > timedelta(days=settings.DIAS_DEVOLUCION_PARCIAL):
        return (max(pagado - min(pagado, reserva.monto_anticipo), Decimal("0.00")),
                f"Cancela el arrendatario con más de {settings.DIAS_DEVOLUCION_PARCIAL} días de anticipación: "
                "se devuelve lo pagado menos el anticipo.")
    return (Decimal("0.00"),
            f"Cancela el arrendatario con {settings.DIAS_DEVOLUCION_PARCIAL} días o menos de anticipación: "
            "no corresponde devolución.")


def _validar_cancelable(reserva, solicitante):
    if reserva.estado in (Reserva.Estado.CANCELADO, Reserva.Estado.EN_PROCESO,
                          Reserva.Estado.FINALIZADO, Reserva.Estado.NO_SE_PRESENTO):
        raise ConflictoDeNegocio(f"Una reserva en estado «{reserva.estado}» no puede cancelarse.")
    if not solicitante.es_admin and reserva.estado != Reserva.Estado.PENDIENTE:
        raise PermissionDenied("Para cancelar una reserva confirmada contacte al administrador.")


def _origen_para(solicitante, origen):
    if not solicitante.es_admin:
        return Reserva.OrigenCancelacion.CLIENTE  # Un cliente solo puede cancelar a nombre propio
    return origen or Reserva.OrigenCancelacion.CLIENTE


def simular_cancelacion(*, reserva, solicitante, origen=None):
    _validar_cancelable(reserva, solicitante)
    origen = _origen_para(solicitante, origen)
    monto, regla = calcular_devolucion(reserva, origen)
    return {"origen": origen, "monto_pagado": reserva.monto_pagado, "monto_devolucion": monto, "regla": regla}


@transaction.atomic
def cancelar_reserva(*, reserva, solicitante, origen=None):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    _validar_cancelable(reserva, solicitante)
    origen = _origen_para(solicitante, origen)
    reserva.monto_devolucion, _ = calcular_devolucion(reserva, origen)
    reserva.estado = Reserva.Estado.CANCELADO
    reserva.cancelada_por = origen
    reserva.fecha_cancelacion = timezone.now()
    reserva.save(update_fields=["estado", "cancelada_por", "fecha_cancelacion", "monto_devolucion"])
    return reserva


@transaction.atomic
def registrar_devolucion(*, reserva):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    if reserva.estado != Reserva.Estado.CANCELADO or reserva.monto_devolucion <= 0:
        raise ConflictoDeNegocio("Esta reserva no tiene una devolución pendiente.")
    if reserva.devolucion_realizada_en:
        raise ConflictoDeNegocio("La devolución ya fue registrada.")
    reserva.devolucion_realizada_en = timezone.now()
    reserva.save(update_fields=["devolucion_realizada_en"])
    return reserva

@transaction.atomic
def cambiar_estado(*, reserva, nuevo_estado):
    reserva = Reserva.objects.select_for_update().get(pk=reserva.pk)
    if nuevo_estado == Reserva.Estado.CANCELADO:
        raise ConflictoDeNegocio("Para cancelar use la opción «Cancelar», que calcula la devolución.")
    if nuevo_estado not in TRANSICIONES_PERMITIDAS[reserva.estado]:
        raise ConflictoDeNegocio(f"No se puede pasar de «{reserva.estado}» a «{nuevo_estado}».")
    if nuevo_estado == Reserva.Estado.EN_PROCESO:
        _validar_ventana_checkin(reserva)
    if nuevo_estado == Reserva.Estado.NO_SE_PRESENTO and timezone.now() < reserva.inicio:
        raise ConflictoDeNegocio("Solo puede marcar «No se presentó» una vez iniciado el horario de la reserva.")
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
        _validar_ventana_checkin(reserva)
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

def cerrar_reservas_vencidas():
    """Cierre automático:
    - Confirmadas cuyo horario terminó sin registrar entrada → «No se presentó» (se conserva el anticipo).
    - En proceso que siguen abiertas 2 horas después del término → «Finalizado».
    """
    ahora = timezone.now()
    no_show = finalizadas = 0
    candidatas = Reserva.objects.filter(
        estado__in=(Reserva.Estado.CONFIRMADO, Reserva.Estado.EN_PROCESO),
        fecha__lte=timezone.localdate(),
    ).values_list("pk", flat=True)
    for pk in list(candidatas):
        with transaction.atomic():
            reserva = Reserva.objects.select_for_update().get(pk=pk)
            if reserva.estado == Reserva.Estado.CONFIRMADO and ahora > reserva.fin:
                reserva.estado = Reserva.Estado.NO_SE_PRESENTO
                reserva.save(update_fields=["estado"])
                no_show += 1
            elif (reserva.estado == Reserva.Estado.EN_PROCESO
                  and ahora > reserva.fin + timedelta(hours=settings.CIERRE_EN_PROCESO_HORAS)):
                reserva.estado = Reserva.Estado.FINALIZADO
                reserva.save(update_fields=["estado"])
                finalizadas += 1
    return no_show, finalizadas

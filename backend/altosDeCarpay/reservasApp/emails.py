import logging
from django.conf import settings
from django.core.mail import EmailMultiAlternatives
from django.template.loader import render_to_string
from django.utils import timezone
from .models import Pago, Reserva

logger = logging.getLogger("reservas")


def _formato_clp(valor):
    return f"{valor:,.0f}".replace(",", ".")


def _obtener_textos_motivo(reserva, motivo):
    if motivo == "confirmacion":
        return (
            f"Altos de Carpay - Reserva N° {reserva.pk} confirmada",
            "¡Tu reserva ha sido confirmada!",
            f"Registramos tu pago inicial y tu reserva para el {reserva.fecha:%d/%m/%Y} quedó confirmada.",
        )
    if motivo == "pago":
        return (
            f"Altos de Carpay - Comprobante de pago Reserva N° {reserva.pk}",
            "Pago registrado en tu reserva",
            "Hemos recibido un nuevo pago asociado a tu reserva. Aquí tienes el detalle actualizado.",
        )
    return (
        f"Altos de Carpay - Detalles de Reserva N° {reserva.pk}",
        "Detalles de tu reserva",
        "Te compartimos el resumen completo y el código de acceso de tu reserva en Altos de Carpay.",
    )


def enviar_correo_reserva(reserva_id, *, motivo="detalle", fail_silently=True):
    """Envía por correo los detalles completos de una reserva (texto plano + HTML)."""
    try:
        reserva = (
            Reserva.objects.select_related("usuario")
            .prefetch_related(
                "sectores_reservados__sector",
                "servicios_reservados__servicio",
                "pagos",
            )
            .get(pk=reserva_id)
        )
        asunto, titulo, mensaje_intro = _obtener_textos_motivo(reserva, motivo)

        sectores = [
            {
                "nombre": item.sector.nombre,
                "cantidad": item.horas,
                "valor_unitario": _formato_clp(item.valor_hora),
                "subtotal": _formato_clp(item.subtotal),
            }
            for item in reserva.sectores_reservados.all()
        ]
        servicios = [
            {
                "nombre": item.servicio.nombre,
                "cantidad": item.cantidad,
                "valor_unitario": _formato_clp(item.valor_unitario),
                "subtotal": _formato_clp(item.subtotal),
            }
            for item in reserva.servicios_reservados.all()
        ]
        pagos = [
            {
                "tipo": pago.get_tipo_pago_display(),
                "metodo": pago.get_metodo_pago_display(),
                "fecha": timezone.localtime(pago.fecha_pago).strftime("%d/%m/%Y %H:%M"),
                "monto": _formato_clp(pago.monto),
            }
            for pago in reserva.pagos.filter(estado_pago=Pago.Estado.APROBADO)
        ]

        contexto = {
            "asunto": asunto,
            "titulo": titulo,
            "mensaje_intro": mensaje_intro,
            "reserva": reserva,
            "hora_fin": timezone.localtime(reserva.fin).time(),
            "sectores": sectores,
            "servicios": servicios,
            "pagos": pagos,
            "total_formateado": _formato_clp(reserva.monto_total),
            "pagado_formateado": _formato_clp(reserva.monto_pagado),
            "saldo_formateado": _formato_clp(reserva.saldo_pendiente),
            "anticipo_porcentaje": settings.ANTICIPO_PORCENTAJE,
            "plazo_horas": settings.PLAZO_PAGO_ANTICIPO_HORAS,
        }

        cuerpo_texto = render_to_string("reservasApp/emails/detalle_reserva.txt", contexto)
        cuerpo_html = render_to_string("reservasApp/emails/detalle_reserva.html", contexto)

        correo = EmailMultiAlternatives(
            subject=asunto,
            body=cuerpo_texto,
            from_email=settings.DEFAULT_FROM_EMAIL,
            to=[reserva.usuario.correo],
        )
        correo.attach_alternative(cuerpo_html, "text/html")
        correo.send(fail_silently=False)
        return True
    except Exception:
        logger.exception("No se pudo enviar el correo de la reserva %s", reserva_id)
        if not fail_silently:
            raise
        return False

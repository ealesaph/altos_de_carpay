import logging
from django.conf import settings
from django.core.mail import send_mail

logger = logging.getLogger("reservas")


def notificar_respuesta(consulta):
    """Envía la respuesta del recinto al correo del cliente (o al de contacto si escribió sin cuenta)."""
    destino = consulta.correo_remitente
    if not destino:
        return False
    cuerpo = (
        f"Hola {consulta.nombre_remitente}:\n\n"
        f"Respondimos tu consulta «{consulta.asunto}» en Altos de Carpay.\n\n"
        f"Tu mensaje:\n{consulta.mensaje}\n\n"
        f"Nuestra respuesta:\n{consulta.respuesta}\n\n"
        "Si tienes cuenta, también puedes verla en Mi cuenta → Mis consultas.\n\n"
        "Altos de Carpay · La Reina, Santiago"
    )
    try:
        send_mail(f"Altos de Carpay - Respuesta a tu consulta N° {consulta.pk}", cuerpo,
                  settings.DEFAULT_FROM_EMAIL, [destino], fail_silently=False)
        return True
    except Exception:
        logger.exception("No se pudo enviar la respuesta de la consulta %s", consulta.pk)
        return False

"""Política de tratamiento de datos personales de Altos de Carpay.

Implementa el deber de información de la Ley N° 21.719 (vigencia plena desde el 01-12-2026):
el titular debe conocer, antes de consentir, quién trata sus datos, con qué finalidad, por cuánto
tiempo y cómo ejercer sus derechos. Cada cambio de contenido debe subir POLITICA_VERSION para que
el sistema vuelva a solicitar el consentimiento.
"""
from django.conf import settings

POLITICA_VERSION = "2026-12-01"


def politica_vigente():
    contacto = getattr(settings, "PRIVACIDAD_CONTACTO", "admin@altosdecarpay.cl")
    return {
        "version": POLITICA_VERSION,
        "titulo": "Política de tratamiento de datos personales",
        "responsable": {
            "nombre": "Altos de Carpay – Centro de eventos",
            "representante": "Lucas Sui Ziebold",
            "domicilio": "La Reina, Región Metropolitana, Chile",
            "contacto": contacto,
        },
        "secciones": [
            {
                "titulo": "Datos que tratamos",
                "contenido": "Nombre, apellidos, RUT, correo electrónico y teléfono; reservas, pagos, "
                             "registros de ingreso y salida (código QR) y observaciones de la bitácora del evento.",
            },
            {
                "titulo": "Finalidades",
                "contenido": "Gestionar su cuenta y sus reservas, controlar el acceso y el horario del evento, "
                             "respaldar el cumplimiento del contrato de arriendo y enviarle comunicaciones "
                             "asociadas a sus reservas. No usamos sus datos con fines publicitarios ni los vendemos.",
            },
            {
                "titulo": "Base de licitud",
                "contenido": "Su consentimiento libre, informado y específico, y la ejecución del contrato de "
                             "arriendo (Ley N° 21.719 y Código Civil, art. 1915 y siguientes).",
            },
            {
                "titulo": "Destinatarios",
                "contenido": "Solo el personal autorizado de Altos de Carpay. El envío de correos se realiza "
                             "mediante un proveedor SMTP que actúa como encargado del tratamiento.",
            },
            {
                "titulo": "Conservación",
                "contenido": "Mientras su cuenta esté activa. Las reservas, pagos y bitácoras de eventos "
                             "finalizados se conservan como respaldo del contrato (Ley N° 19.799) y luego se eliminan "
                             "o anonimizan.",
            },
            {
                "titulo": "Sus derechos",
                "contenido": "Acceso, rectificación, supresión, oposición y portabilidad (ARCO+). Puede descargar "
                             "sus datos desde «Mis datos», corregirlos en «Mi perfil», retirar su consentimiento y "
                             "desactivar su cuenta en «Mi cuenta», o escribir al contacto indicado. Responderemos "
                             "dentro de 30 días corridos.",
            },
            {
                "titulo": "Seguridad y brechas",
                "contenido": "Protegemos sus datos con contraseñas cifradas, acceso por roles y registros de "
                             "auditoría. Si ocurre una brecha de seguridad que lo afecte, se la informaremos a usted "
                             "y a la Agencia de Protección de Datos Personales.",
            },
        ],
    }


def requiere_consentimiento(usuario):
    return usuario.consentimiento_datos is None or usuario.consentimiento_version != POLITICA_VERSION

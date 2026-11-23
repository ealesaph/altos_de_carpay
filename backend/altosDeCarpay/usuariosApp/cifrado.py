"""Cifrado en reposo de datos personales (Tríada CID · Ley N° 21.719).

Los campos ``CampoCifrado`` se guardan en PostgreSQL cifrados con Fernet (AES-128-CBC con
autenticación HMAC-SHA256) usando la clave ``FIELD_ENCRYPTION_KEY``. Como el cifrado es
aleatorio, la unicidad y las búsquedas exactas del RUT se resuelven con una huella HMAC-SHA256
(``rut_hash``), que no permite recuperar el dato original.
"""
import hashlib
import hmac
from functools import lru_cache

from cryptography.fernet import Fernet, InvalidToken, MultiFernet
from django.conf import settings
from django.db import models

from .validators import normalizar_rut


@lru_cache(maxsize=4)
def _multifernet(principal, anteriores):
    # La primera clave cifra; todas sirven para descifrar (rotación de claves).
    return MultiFernet([Fernet(c.encode()) for c in (principal, *anteriores)])


def _cifrador():
    return _multifernet(settings.FIELD_ENCRYPTION_KEY,
                        tuple(getattr(settings, "FIELD_ENCRYPTION_KEYS_ANTERIORES", ())))


def cifrar(texto):
    return _cifrador().encrypt(str(texto).encode()).decode()


def descifrar(valor):
    try:
        return _cifrador().decrypt(str(valor).encode()).decode()
    except (InvalidToken, ValueError, TypeError):
        # Dato heredado de antes del cifrado: se devuelve tal cual y se cifra al volver a guardarse.
        return valor


def huella(texto):
    clave = hashlib.sha256(b"huella:" + settings.FIELD_ENCRYPTION_KEY.encode()).digest()
    return hmac.new(clave, str(texto).encode(), hashlib.sha256).hexdigest()


def huella_rut(rut):
    return huella(normalizar_rut(rut)) if rut else None


class CampoCifrado(models.CharField):
    """CharField que se cifra al guardar y se descifra al leer. ``max_length`` limita el texto claro."""

    description = "Texto cifrado en reposo (Fernet)"

    def from_db_value(self, value, expression, connection):
        if value in (None, ""):
            return value
        return descifrar(value)

    def get_prep_value(self, value):
        value = super().get_prep_value(value)
        if value in (None, ""):
            return value
        return cifrar(value)

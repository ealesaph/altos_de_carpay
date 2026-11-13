import re

from django.core.exceptions import ValidationError


def normalizar_rut(valor):
    return re.sub(r"[.\-\s]", "", str(valor)).upper()


def calcular_digito_verificador(cuerpo):
    suma, factor = 0, 2
    for digito in reversed(cuerpo):
        suma += int(digito) * factor
        factor = 2 if factor == 7 else factor + 1
    resto = 11 - (suma % 11)
    if resto == 11:
        return "0"
    if resto == 10:
        return "K"
    return str(resto)


def validar_rut(valor):
    rut = normalizar_rut(valor)
    if not re.fullmatch(r"\d{7,8}[\dK]", rut):
        raise ValidationError("Ingrese un RUT válido (ej.: 123456785).")
    if calcular_digito_verificador(rut[:-1]) != rut[-1]:
        raise ValidationError("El dígito verificador del RUT no es válido.")

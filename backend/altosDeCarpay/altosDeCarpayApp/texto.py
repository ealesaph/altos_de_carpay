from django.utils.html import strip_tags
from rest_framework import serializers

def limpiar_texto(valor):
    return strip_tags(valor or "").replace("\x00", "").strip()

def texto_obligatorio(valor, campo="El campo"):
    limpio = limpiar_texto(valor)
    if not limpio:
        raise serializers.ValidationError(f"{campo} no puede estar vacío.")
    return limpio
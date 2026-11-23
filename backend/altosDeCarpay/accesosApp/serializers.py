from rest_framework import serializers
from .models import ValidacionAcceso


class RegistrarAccesoSerializer(serializers.Serializer):
    token_qr = serializers.CharField(max_length=255)
    tipo_registro = serializers.ChoiceField(choices=ValidacionAcceso.Tipo.choices)


class ValidacionAccesoSerializer(serializers.ModelSerializer):
    class Meta:
        model = ValidacionAcceso
        fields = ["id_validacion", "reserva", "tipo_registro", "fecha_hora", "validado_por"]
        read_only_fields = fields

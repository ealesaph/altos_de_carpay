from rest_framework import serializers
from .models import LogModificacion


class LogModificacionSerializer(serializers.ModelSerializer):
    usuario_correo = serializers.CharField(source="usuario.correo", read_only=True, default=None)

    class Meta:
        model = LogModificacion
        fields = ["id_log", "usuario", "usuario_correo", "fecha", "tabla_modificada",
                  "id_registro", "valor_previo", "valor_nuevo"]
        read_only_fields = fields
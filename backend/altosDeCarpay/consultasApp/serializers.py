from rest_framework import serializers
from altosDeCarpayApp.texto import texto_obligatorio
from .models import Consulta

class ConsultaSerializer(serializers.ModelSerializer):
    cliente_nombre = serializers.CharField(source="cliente.nombre_completo", read_only=True)

    class Meta:
        model = Consulta
        fields = ["id_consulta", "cliente", "cliente_nombre", "asunto", "mensaje", "fecha_envio",
                  "respuesta", "fecha_respuesta", "estado"]
        read_only_fields = ["id_consulta", "cliente", "fecha_envio", "respuesta", "fecha_respuesta", "estado"]

    def validate_asunto(self, valor):
        return texto_obligatorio(valor, "El asunto")

    def validate_mensaje(self, valor):
        return texto_obligatorio(valor, "El mensaje")

class RespuestaSerializer(serializers.Serializer):
    respuesta = serializers.CharField(max_length=2000)

    def validate_respuesta(self, valor):
        return texto_obligatorio(valor, "La respuesta")
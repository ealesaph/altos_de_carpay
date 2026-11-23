from rest_framework import serializers
from altosDeCarpayApp.texto import limpiar_texto, texto_obligatorio
from .models import Consulta


class ConsultaSerializer(serializers.ModelSerializer):
    cliente_nombre = serializers.CharField(source="nombre_remitente", read_only=True)
    correo_respuesta = serializers.CharField(source="correo_remitente", read_only=True)
    acepta_privacidad = serializers.BooleanField(write_only=True, required=False, default=False)

    class Meta:
        model = Consulta
        fields = ["id_consulta", "cliente", "cliente_nombre", "correo_respuesta", "nombre_contacto",
                  "correo_contacto", "telefono_contacto", "acepta_privacidad", "asunto", "mensaje",
                  "fecha_envio", "respuesta", "fecha_respuesta", "estado"]
        read_only_fields = ["id_consulta", "cliente", "fecha_envio", "respuesta", "fecha_respuesta", "estado"]
        extra_kwargs = {"telefono_contacto": {"write_only": False}}

    def validate_asunto(self, valor):
        return texto_obligatorio(valor, "El asunto")

    def validate_mensaje(self, valor):
        return texto_obligatorio(valor, "El mensaje")

    def validate_nombre_contacto(self, valor):
        return limpiar_texto(valor)

    def validate_telefono_contacto(self, valor):
        limpio = limpiar_texto(valor)
        if limpio and not all(c.isdigit() or c in "+ -()" for c in limpio):
            raise serializers.ValidationError("Ingresa un teléfono válido (solo números, espacios y +).")
        return limpio[:20]

    def validate(self, datos):
        usuario = self.context["request"].user
        acepta = datos.pop("acepta_privacidad", False)
        if not usuario.is_authenticated:
            if not datos.get("nombre_contacto"):
                raise serializers.ValidationError({"nombre_contacto": "Indica tu nombre para poder responderte."})
            if not datos.get("correo_contacto"):
                raise serializers.ValidationError({"correo_contacto": "Indica un correo para enviarte la respuesta."})
            if not acepta:
                raise serializers.ValidationError({"acepta_privacidad": (
                    "Debes aceptar la política de privacidad para que usemos tus datos de contacto "
                    "solo para responder esta consulta."
                )})
        else:
            # Con cuenta, la respuesta llega al correo registrado: no se guardan datos de contacto duplicados.
            datos["nombre_contacto"] = datos["correo_contacto"] = datos["telefono_contacto"] = ""
        return datos


class RespuestaSerializer(serializers.Serializer):
    respuesta = serializers.CharField(max_length=2000)

    def validate_respuesta(self, valor):
        return texto_obligatorio(valor, "La respuesta")

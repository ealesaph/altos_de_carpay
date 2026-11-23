import logging
from django.contrib.auth.password_validation import validate_password
from django.contrib.auth.signals import user_logged_in
from django.core.exceptions import ValidationError as DjangoValidationError
from django.utils import timezone
from rest_framework import serializers
from rest_framework.exceptions import AuthenticationFailed
from rest_framework.validators import UniqueValidator
from rest_framework_simplejwt.exceptions import TokenError
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from rest_framework_simplejwt.tokens import RefreshToken
from .privacidad import POLITICA_VERSION, requiere_consentimiento
from .models import Usuario
from .cifrado import huella_rut
from .validators import normalizar_rut, validar_rut

seguridad = logging.getLogger("seguridad")


class RutField(serializers.CharField):
    def to_internal_value(self, data):
        valor = super().to_internal_value(data)
        try:
            validar_rut(valor)
        except DjangoValidationError as error:
            raise serializers.ValidationError(error.messages)
        return normalizar_rut(valor)


def rut_no_registrado(valor):
    """El RUT se guarda cifrado: la unicidad se verifica con su huella HMAC."""
    if Usuario.objects.filter(rut_hash=huella_rut(valor)).exists():
        raise serializers.ValidationError("Ya existe una cuenta con este RUT.")


def _validar_clave(clave, usuario=None):
    try:
        validate_password(clave, user=usuario)
    except DjangoValidationError as error:
        raise serializers.ValidationError(list(error.messages))


class RegistroSerializer(serializers.ModelSerializer):
    correo = serializers.EmailField(
        max_length=100,
        validators=[UniqueValidator(queryset=Usuario.objects.all(), lookup="iexact",
                                    message="Ya existe una cuenta con este correo.")],
    )
    rut = RutField(
        max_length=15,
        validators=[rut_no_registrado],
    )
    password = serializers.CharField(write_only=True, max_length=128, trim_whitespace=False,
                                     style={"input_type": "password"})
    acepta_politica_datos = serializers.BooleanField(write_only=True)

    class Meta:
        model = Usuario
        fields = ["correo", "rut", "nombre", "apellido_paterno", "apellido_materno",
                  "telefono", "password", "acepta_politica_datos"]

    def validate_acepta_politica_datos(self, valor):
        if valor is not True:
            raise serializers.ValidationError(
                "Debe aceptar la política de tratamiento de datos personales (Ley N° 21.719)."
            )
        return valor

    def validate(self, datos):
        usuario_tmp = Usuario(correo=datos.get("correo", ""), nombre=datos.get("nombre", ""),
                              apellido_paterno=datos.get("apellido_paterno", ""))
        try:
            _validar_clave(datos["password"], usuario_tmp)
        except serializers.ValidationError as error:
            raise serializers.ValidationError({"password": error.detail})
        return datos

    def create(self, datos):
        datos.pop("acepta_politica_datos")
        clave = datos.pop("password")
        return Usuario.objects.create_user(
            password=clave, consentimiento_datos=timezone.now(),
            consentimiento_version=POLITICA_VERSION, **datos
        )


class PerfilSerializer(serializers.ModelSerializer):
    class Meta:
        model = Usuario
        fields = ["id_usuario", "correo", "rut", "nombre", "apellido_paterno", "apellido_materno",
                  "telefono", "rol", "estado", "is_superuser", "ultima_conexion"]
        read_only_fields = ["id_usuario", "correo", "rut", "rol", "estado", "is_superuser", "ultima_conexion"]


class CrearPersonalAdminSerializer(serializers.ModelSerializer):
    correo = serializers.EmailField(
        max_length=100,
        validators=[UniqueValidator(queryset=Usuario.objects.all(), lookup="iexact",
                                    message="Ya existe una cuenta con este correo.")],
    )
    rut = RutField(
        max_length=15,
        validators=[rut_no_registrado],
    )
    password = serializers.CharField(write_only=True, max_length=128, trim_whitespace=False,
                                     style={"input_type": "password"})

    class Meta:
        model = Usuario
        fields = ["correo", "rut", "nombre", "apellido_paterno", "apellido_materno", "telefono", "password"]

    def validate(self, datos):
        usuario_tmp = Usuario(correo=datos.get("correo", ""), nombre=datos.get("nombre", ""),
                              apellido_paterno=datos.get("apellido_paterno", ""))
        try:
            _validar_clave(datos["password"], usuario_tmp)
        except serializers.ValidationError as error:
            raise serializers.ValidationError({"password": error.detail})
        return datos

    def create(self, datos):
        clave = datos.pop("password")
        return Usuario.objects.create_user(
            password=clave,
            rol=Usuario.Rol.ADMIN,
            is_superuser=False,
            consentimiento_datos=timezone.now(),
            **datos,
        )


class UsuarioAdminSerializer(PerfilSerializer):
    class Meta(PerfilSerializer.Meta):
        read_only_fields = ["id_usuario", "correo", "rut", "nombre", "apellido_paterno",
                            "apellido_materno", "telefono", "is_superuser", "ultima_conexion"]

    def validate(self, datos):
        solicitante = self.context["request"].user
        if self.instance:
            if self.instance.is_superuser:
                if datos.get("estado") == Usuario.Estado.INACTIVO or datos.get("rol") == Usuario.Rol.CLIENTE:
                    raise serializers.ValidationError(
                        "No se puede desactivar ni quitar el rol al superusuario principal."
                    )
            if self.instance.pk == solicitante.pk:
                if datos.get("estado") == Usuario.Estado.INACTIVO or datos.get("rol") == Usuario.Rol.CLIENTE:
                    raise serializers.ValidationError(
                        "No puede desactivarse ni quitarse el rol de administrador a sí mismo."
                    )
            if not solicitante.is_superuser and (
                "rol" in datos or self.instance.es_admin
            ):
                raise serializers.ValidationError(
                    "Solo el superusuario puede asignar o modificar cuentas de personal administrador."
                )
        return datos


class CambioPasswordSerializer(serializers.Serializer):
    password_actual = serializers.CharField(write_only=True, trim_whitespace=False)
    password_nueva = serializers.CharField(write_only=True, max_length=128, trim_whitespace=False)

    def validate_password_actual(self, valor):
        if not self.context["request"].user.check_password(valor):
            raise serializers.ValidationError("La contraseña actual no es correcta.")
        return valor

    def validate_password_nueva(self, valor):
        _validar_clave(valor, self.context["request"].user)
        return valor


class LoginSerializer(TokenObtainPairSerializer):
    @classmethod
    def get_token(cls, usuario):
        token = super().get_token(usuario)
        token["rol"] = usuario.rol
        token["nombre"] = usuario.nombre
        token["is_superuser"] = usuario.is_superuser
        return token

    def validate(self, atributos):
        peticion = self.context.get("request")
        try:
            datos = super().validate(atributos)
        except AuthenticationFailed:
            ip = peticion.META.get("REMOTE_ADDR", "desconocida") if peticion else "desconocida"
            seguridad.warning("Intento de acceso fallido desde la IP %s", ip)
            raise
        user_logged_in.send(sender=type(self.user), request=peticion, user=self.user)
        datos["usuario"] = {
            "id_usuario": self.user.pk,
            "nombre": self.user.nombre,
            "rol": self.user.rol,
            "is_superuser": self.user.is_superuser,
            "requiere_consentimiento": requiere_consentimiento(self.user),
        }
        return datos


class LogoutSerializer(serializers.Serializer):
    refresh = serializers.CharField(write_only=True)

    def save(self, **kwargs):
        try:
            RefreshToken(self.validated_data["refresh"]).blacklist()
        except TokenError:
            raise serializers.ValidationError({"refresh": "Token inválido o ya expirado."})


class ConsentimientoSerializer(serializers.Serializer):
    """Registra el consentimiento informado de un titular con cuenta existente (Ley N° 21.719)."""
    acepta = serializers.BooleanField()
    version = serializers.CharField(max_length=20)

    def validate_acepta(self, valor):
        if valor is not True:
            raise serializers.ValidationError(
                "Para continuar debe aceptar la política de tratamiento de datos personales. "
                "Si no está de acuerdo, puede desactivar su cuenta."
            )
        return valor

    def validate_version(self, valor):
        if valor != POLITICA_VERSION:
            raise serializers.ValidationError(
                f"La política vigente es la versión {POLITICA_VERSION}. Vuelva a leerla antes de aceptar."
            )
        return valor

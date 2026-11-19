from django.db.models import Q
from rest_framework import generics, mixins, status, viewsets
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken, OutstandingToken
from rest_framework_simplejwt.views import TokenObtainPairView
from altosDeCarpayApp.permissions import EsAdministrador
from .models import Usuario
from .serializers import (
    CambioPasswordSerializer,
    LoginSerializer,
    LogoutSerializer,
    PerfilSerializer,
    RegistroSerializer,
    UsuarioAdminSerializer,
)


class RegistroView(generics.CreateAPIView):
    serializer_class = RegistroSerializer
    permission_classes = [AllowAny]
    authentication_classes = []
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "registro"

    def create(self, request, *args, **kwargs):
        serializador = self.get_serializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        usuario = serializador.save()
        return Response(PerfilSerializer(usuario).data, status=status.HTTP_201_CREATED)


class LoginView(TokenObtainPairView):
    serializer_class = LoginSerializer
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "login"


class LogoutView(APIView):
    def post(self, request):
        serializador = LogoutSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        serializador.save()
        return Response(status=status.HTTP_204_NO_CONTENT)


class PerfilView(generics.RetrieveUpdateAPIView):
    serializer_class = PerfilSerializer
    http_method_names = ["get", "patch", "head", "options"]

    def get_object(self):
        return self.request.user


class CambiarPasswordView(APIView):
    def post(self, request):
        serializador = CambioPasswordSerializer(data=request.data, context={"request": request})
        serializador.is_valid(raise_exception=True)
        usuario = request.user
        usuario.set_password(serializador.validated_data["password_nueva"])
        usuario.save(update_fields=["password"])
        for token in OutstandingToken.objects.filter(user=usuario):
            BlacklistedToken.objects.get_or_create(token=token)
        return Response({"detail": "Contraseña actualizada. Vuelva a iniciar sesión."})


class MisDatosArcoView(APIView):
    """Derecho de Acceso (Ley N° 21.719): entrega copia estructurada de los datos personales del titular."""

    def get(self, request):
        usuario = request.user
        return Response({
            "perfil": PerfilSerializer(usuario).data,
            "consentimiento_datos": usuario.consentimiento_datos,
            "total_reservas": usuario.reservas.count(),
            "total_consultas": usuario.consultas.count(),
        })


class DesactivarCuentaArcoView(APIView):
    """Derecho de Cancelación/Oposición (Ley N° 21.719): desactiva la cuenta y revoca sesiones."""

    def post(self, request):
        usuario = request.user
        if usuario.es_admin:
            return Response(
                {"detail": "Un administrador no puede auto-desactivar su cuenta por esta vía."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        usuario.estado = Usuario.Estado.INACTIVO
        usuario.save(update_fields=["estado"])
        for token in OutstandingToken.objects.filter(user=usuario):
            BlacklistedToken.objects.get_or_create(token=token)
        return Response({"detail": "Cuenta desactivada y sesiones cerradas conforme a la Ley N° 21.719."})


class UsuarioAdminViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin,
                          mixins.UpdateModelMixin, viewsets.GenericViewSet):

    serializer_class = UsuarioAdminSerializer
    permission_classes = [EsAdministrador]
    http_method_names = ["get", "patch", "head", "options"]

    def get_queryset(self):
        consulta = Usuario.objects.all().order_by("id_usuario")
        params = self.request.query_params
        if params.get("rol") in Usuario.Rol.values:
            consulta = consulta.filter(rol=params["rol"])
        if params.get("estado") in Usuario.Estado.values:
            consulta = consulta.filter(estado=params["estado"])
        texto = params.get("q", "").strip()
        if texto:
            consulta = consulta.filter(
                Q(correo__icontains=texto) | Q(nombre__icontains=texto)
                | Q(apellido_paterno__icontains=texto) | Q(rut__icontains=texto)
            )
        return consulta
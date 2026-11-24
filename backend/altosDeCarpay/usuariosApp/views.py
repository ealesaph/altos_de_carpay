from django.db.models import Count, Q
from django.http import HttpResponse
from rest_framework.decorators import action
from altosDeCarpayApp.exportacion import respuesta_exportacion
from rest_framework import generics, mixins, status, viewsets
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken, OutstandingToken
from rest_framework_simplejwt.views import TokenObtainPairView
from altosDeCarpayApp.permissions import EsAdministrador, EsSuperusuario
from django.utils import timezone
from .cifrado import huella_rut
from .models import Usuario
from .pdf import generar_pdf_mis_datos
from .privacidad import POLITICA_VERSION, politica_vigente, requiere_consentimiento
from .serializers import (
    CambioPasswordSerializer,
    ConsentimientoSerializer,
    CrearPersonalAdminSerializer,
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


class PoliticaPrivacidadView(APIView):
    """Deber de información (Ley N° 21.719): política pública y versionada, legible antes de registrarse."""
    permission_classes = [AllowAny]
    authentication_classes = []

    def get(self, request):
        return Response(politica_vigente())


class ConsentimientoView(APIView):
    """Consulta y registro del consentimiento informado del titular autenticado (Ley N° 21.719)."""

    def get(self, request):
        usuario = request.user
        return Response({
            "consentimiento_datos": usuario.consentimiento_datos,
            "consentimiento_version": usuario.consentimiento_version or None,
            "version_vigente": POLITICA_VERSION,
            "requiere_consentimiento": requiere_consentimiento(usuario),
        })

    def post(self, request):
        serializador = ConsentimientoSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        usuario = request.user
        usuario.consentimiento_datos = timezone.now()
        usuario.consentimiento_version = POLITICA_VERSION
        usuario.save(update_fields=["consentimiento_datos", "consentimiento_version"])
        return Response({
            "detail": "Consentimiento registrado.",
            "consentimiento_datos": usuario.consentimiento_datos,
            "consentimiento_version": usuario.consentimiento_version,
            "requiere_consentimiento": False,
        })


class MisDatosArcoView(APIView):
    """Derechos de Acceso y Portabilidad (Ley N° 21.719): copia estructurada y reutilizable (JSON)
    de los datos personales del titular y de las operaciones asociadas a su cuenta."""

    def get(self, request):
        usuario = request.user
        reservas = [
            {
                "id_reserva": r.id_reserva,
                "fecha": r.fecha,
                "hora_inicio": r.hora_inicio,
                "horas_reservadas": r.horas_reservadas,
                "estado": r.estado,
                "monto_total": r.monto_total,
                "fecha_creacion": r.fecha_creacion,
            }
            for r in usuario.reservas.order_by("-fecha")
        ]
        consultas = [
            {
                "id_consulta": c.id_consulta,
                "asunto": c.asunto,
                "mensaje": c.mensaje,
                "fecha_envio": c.fecha_envio,
                "estado": c.estado,
            }
            for c in usuario.consultas.all()
        ]
        datos = {
            "fecha_emision": timezone.now(),
            "perfil": PerfilSerializer(usuario).data,
            "consentimiento_datos": usuario.consentimiento_datos,
            "consentimiento_version": usuario.consentimiento_version or None,
            "total_reservas": len(reservas),
            "total_consultas": len(consultas),
            "reservas": reservas,
            "consultas": consultas,
        }
        if request.query_params.get("formato") == "pdf":
            contenido_pdf = generar_pdf_mis_datos(datos)
            fecha_archivo = timezone.localdate().isoformat()
            respuesta = HttpResponse(contenido_pdf, content_type="application/pdf")
            respuesta["Content-Disposition"] = (
                f'attachment; filename="mis-datos-altos-de-carpay-{fecha_archivo}.pdf"'
            )
            return respuesta
        return Response(datos)


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
                          mixins.CreateModelMixin, mixins.UpdateModelMixin,
                          viewsets.GenericViewSet):

    serializer_class = UsuarioAdminSerializer
    http_method_names = ["get", "post", "patch", "head", "options"]

    def get_permissions(self):
        if self.action == "create":
            return [EsSuperusuario()]
        return [EsAdministrador()]

    def get_serializer_class(self):
        if self.action == "create":
            return CrearPersonalAdminSerializer
        return UsuarioAdminSerializer

    def create(self, request, *args, **kwargs):
        serializador = self.get_serializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        usuario = serializador.save()
        return Response(UsuarioAdminSerializer(usuario).data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["get"], url_path="exportar")
    def exportar(self, request):
        """HU-08: reporte de usuarios (CSV o ?formato=xlsx). El RUT y el teléfono se descifran solo aquí."""
        filas = (
            (u.pk, u.nombre, u.apellido_paterno, u.apellido_materno, u.correo, u.rut, u.telefono, u.rol,
             u.estado, u.consentimiento_datos, u.consentimiento_version or "", u.total_reservas)
            for u in self.get_queryset().annotate(total_reservas=Count("reservas"))
        )
        return respuesta_exportacion(
            "usuarios_altos_de_carpay", "Usuarios",
            ["ID", "Nombre", "Apellido paterno", "Apellido materno", "Correo", "RUT", "Teléfono", "Rol",
             "Estado", "Consentimiento de datos", "Versión de la política", "N° de reservas"],
            filas, request.query_params.get("formato"),
        )

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
                | Q(apellido_paterno__icontains=texto) | Q(rut_hash=huella_rut(texto))
            )
        return consulta
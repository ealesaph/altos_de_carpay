from django.utils import timezone
from rest_framework import mixins, status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import PermissionDenied
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from altosDeCarpayApp.excepciones import ConflictoDeNegocio
from altosDeCarpayApp.permissions import EsAdministrador
from .emails import notificar_respuesta
from .models import Consulta
from .serializers import ConsultaSerializer, RespuestaSerializer

class ConsultaViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin, mixins.CreateModelMixin,
                      viewsets.GenericViewSet):
    serializer_class = ConsultaSerializer

    def get_permissions(self):
        if self.action == "create":
            return [AllowAny()]  # La consulta de la portada no exige cuenta
        if self.action == "responder":
            return [EsAdministrador()]
        return [IsAuthenticated()]

    def get_throttles(self):
        if self.action == "create":
            self.throttle_scope = "consulta"
            return [ScopedRateThrottle()]
        return super().get_throttles()

    def get_queryset(self):
        consulta = Consulta.objects.select_related("cliente")
        usuario = self.request.user
        if usuario.es_admin:
            if self.request.query_params.get("estado") in Consulta.Estado.values:
                consulta = consulta.filter(estado=self.request.query_params["estado"])
            return consulta
        return consulta.filter(cliente=usuario)

    def perform_create(self, serializador):
        usuario = self.request.user
        if usuario.is_authenticated and usuario.es_admin:
            raise PermissionDenied("El personal responde consultas; no las envía.")
        serializador.save(cliente=usuario if usuario.is_authenticated else None)

    @action(detail=True, methods=["post"])
    def responder(self, request, pk=None):
        consulta = self.get_object()
        if consulta.estado == Consulta.Estado.RESPONDIDA:
            raise ConflictoDeNegocio("La consulta ya fue respondida.")
        serializador = RespuestaSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        consulta.respuesta = serializador.validated_data["respuesta"]
        consulta.fecha_respuesta = timezone.now()
        consulta.respondida_por = request.user
        consulta.estado = Consulta.Estado.RESPONDIDA
        consulta.save()
        notificar_respuesta(consulta)
        return Response(self.get_serializer(consulta).data, status=status.HTTP_200_OK)
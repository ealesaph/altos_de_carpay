from rest_framework import mixins, status, viewsets
from rest_framework.response import Response
from altosDeCarpayApp.permissions import EsAdministrador
from .models import ValidacionAcceso
from .serializers import RegistrarAccesoSerializer, ValidacionAccesoSerializer
from .services import registrar_acceso


class ValidacionAccesoViewSet(mixins.ListModelMixin, viewsets.GenericViewSet):

    serializer_class = ValidacionAccesoSerializer
    permission_classes = [EsAdministrador]

    def get_queryset(self):
        consulta = ValidacionAcceso.objects.select_related("reserva", "validado_por")
        if self.request.query_params.get("reserva", "").isdigit():
            consulta = consulta.filter(reserva_id=int(self.request.query_params["reserva"]))
        return consulta

    def create(self, request):
        entrada = RegistrarAccesoSerializer(data=request.data)
        entrada.is_valid(raise_exception=True)
        validacion, minutos = registrar_acceso(validador=request.user, **entrada.validated_data)
        datos = ValidacionAccesoSerializer(validacion).data
        datos["minutos_excedidos"] = minutos
        return Response(datos, status=status.HTTP_201_CREATED)
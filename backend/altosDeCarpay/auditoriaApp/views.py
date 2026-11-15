from rest_framework import viewsets
from altosDeCarpayApp.permissions import EsAdministrador
from .models import LogModificacion
from .serializers import LogModificacionSerializer


class LogModificacionViewSet(viewsets.ReadOnlyModelViewSet):

    serializer_class = LogModificacionSerializer
    permission_classes = [EsAdministrador]

    def get_queryset(self):
        consulta = LogModificacion.objects.select_related("usuario")
        params = self.request.query_params
        if params.get("tabla"):
            consulta = consulta.filter(tabla_modificada=params["tabla"])
        if params.get("id_registro", "").isdigit():
            consulta = consulta.filter(id_registro=int(params["id_registro"]))
        return consulta
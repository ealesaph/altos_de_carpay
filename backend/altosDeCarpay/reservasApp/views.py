import csv
from decimal import Decimal
from django.db.models import DecimalField, Q, Sum, Value
from django.db.models.functions import Coalesce
from django.http import HttpResponse
from rest_framework import mixins, status, viewsets
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from altosDeCarpayApp.permissions import EsAdministrador
from . import services
from .models import Pago, Reserva, Review, Sector, Servicio
from .serializers import (
    CambioEstadoSerializer,
    PagoCrearSerializer,
    PagoSerializer,
    ReservaCrearSerializer,
    ReservaDetalleSerializer,
    ReviewSerializer,
    SectorSerializer,
    ServicioSerializer,
)

class CatalogoViewSet(viewsets.ModelViewSet):
    http_method_names = ["get", "post", "patch", "head", "options"]

    def get_permissions(self):
        if self.action in ("list", "retrieve"):
            return [AllowAny()]
        return [EsAdministrador()]

class SectorViewSet(CatalogoViewSet):
    serializer_class = SectorSerializer

    def get_queryset(self):
        consulta = Sector.objects.all()
        usuario = self.request.user
        if not (usuario.is_authenticated and usuario.es_admin):
            consulta = consulta.exclude(estado=Sector.Estado.INACTIVO)
        return consulta

class ServicioViewSet(CatalogoViewSet):
    serializer_class = ServicioSerializer

    def get_queryset(self):
        consulta = Servicio.objects.all()
        usuario = self.request.user
        if not (usuario.is_authenticated and usuario.es_admin):
            consulta = consulta.filter(estado=Servicio.Estado.ACTIVO)
        return consulta

def _celda_segura(valor):
    texto = "" if valor is None else str(valor)
    return "'" + texto if texto[:1] in ("=", "+", "-", "@", "\t", "\r") else texto

class ReservaViewSet(
    mixins.CreateModelMixin,
    mixins.ListModelMixin,
    mixins.RetrieveModelMixin,
    viewsets.GenericViewSet,
):
    serializer_class = ReservaDetalleSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        usuario = self.request.user
        consulta = (
            Reserva.objects.select_related("usuario")
            .prefetch_related("sectores_reservados__sector", "servicios_reservados__servicio", "pagos")
            .annotate(total_pagado=Coalesce(
                Sum("pagos__monto", filter=Q(pagos__estado_pago=Pago.Estado.APROBADO)),
                Value(Decimal("0.00")),
                output_field=DecimalField(max_digits=12, decimal_places=2),
            ))
            .order_by("-fecha", "-hora_inicio", "-id_reserva")
        )
        if not usuario.es_admin:
            return consulta.filter(usuario=usuario)  # Un cliente jamás ve reservas ajenas
        params = self.request.query_params
        if params.get("estado") in Reserva.Estado.values:
            consulta = consulta.filter(estado=params["estado"])
        if params.get("cliente", "").isdigit():
            consulta = consulta.filter(usuario_id=int(params["cliente"]))
        if params.get("desde"):
            consulta = consulta.filter(fecha__gte=params["desde"])
        if params.get("hasta"):
            consulta = consulta.filter(fecha__lte=params["hasta"])
        return consulta

    def create(self, request):
        serializador = ReservaCrearSerializer(data=request.data, context={"request": request})
        serializador.is_valid(raise_exception=True)
        reserva = serializador.save()
        reserva = self.get_queryset().get(pk=reserva.pk)
        return Response(self.get_serializer(reserva).data, status=status.HTTP_201_CREATED)

    @action(detail=True, methods=["post"])
    def cancelar(self, request, pk=None):
        reserva = services.cancelar_reserva(reserva=self.get_object(), solicitante=request.user)
        return Response(self.get_serializer(self.get_queryset().get(pk=reserva.pk)).data)

    @action(detail=True, methods=["post"], url_path="estado", permission_classes=[EsAdministrador])
    def cambiar_estado(self, request, pk=None):
        serializador = CambioEstadoSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        reserva = services.cambiar_estado(reserva=self.get_object(),
                                          nuevo_estado=serializador.validated_data["estado"])
        return Response(self.get_serializer(self.get_queryset().get(pk=reserva.pk)).data)

    @action(detail=True, methods=["get", "post"], url_path="pagos")
    def pagos(self, request, pk=None):
        reserva = self.get_object()
        if request.method == "GET":
            return Response(PagoSerializer(reserva.pagos.all(), many=True).data)
        serializador = PagoCrearSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        pago = services.registrar_pago(reserva=reserva, usuario=request.user, **serializador.validated_data)
        return Response(PagoSerializer(pago).data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["get"], url_path="exportar", permission_classes=[EsAdministrador])
    def exportar(self, request):
        respuesta = HttpResponse(content_type="text/csv; charset=utf-8")
        respuesta["Content-Disposition"] = 'attachment; filename="reservas_altos_de_carpay.csv"'
        respuesta.write("\ufeff")
        escritor = csv.writer(respuesta, delimiter=";")
        escritor.writerow(["N° reserva", "Cliente", "Correo", "Fecha", "Hora inicio", "Horas", "Estado",
                           "Monto total", "Monto pagado", "Saldo pendiente"])
        for r in self.get_queryset():
            escritor.writerow([_celda_segura(v) for v in (
                r.pk, r.usuario.nombre_completo, r.usuario.correo, r.fecha,
                r.hora_inicio.strftime("%H:%M") if r.hora_inicio else "",
                r.horas_reservadas, r.estado, r.monto_total, r.monto_pagado, r.saldo_pendiente)])
        return respuesta

class ReviewViewSet(mixins.ListModelMixin, mixins.CreateModelMixin, viewsets.GenericViewSet):
    serializer_class = ReviewSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        usuario = self.request.user
        consulta = Review.objects.select_related("reserva", "evaluador", "evaluado")
        if usuario.es_admin:
            return consulta
        return consulta.filter(Q(evaluador=usuario) | Q(evaluado=usuario))

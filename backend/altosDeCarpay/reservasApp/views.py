import os
import uuid
from decimal import Decimal
from django.conf import settings
from django.core.files.storage import default_storage
from django.db.models import DecimalField, Q, Sum, Value
from django.utils import timezone
from django.db.models.functions import Coalesce
from django.http import HttpResponse
from rest_framework import mixins, status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from altosDeCarpayApp.exportacion import respuesta_exportacion
from altosDeCarpayApp.permissions import EsAdministrador
from . import services
from .models import BitacoraIncidencia, Pago, Reserva, Review, Sector, Servicio
from .serializers import (
    BitacoraIncidenciaSerializer,
    CambioEstadoSerializer,
    CancelacionSerializer,
    PagoCrearSerializer,
    PagoSerializer,
    ReservaCrearSerializer,
    ReservaDetalleSerializer,
    ReservaModificarSerializer,
    ReviewSerializer,
    SectorSerializer,
    ServicioSerializer,
    ValidacionAccesoCrearSerializer,
    ValidacionAccesoSerializer,
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

    @action(
        detail=True,
        methods=["post"],
        url_path="subir-imagen",
        permission_classes=[EsAdministrador],
        parser_classes=[MultiPartParser, FormParser],
    )
    def subir_imagen(self, request, pk=None):
        sector = self.get_object()
        archivo = request.FILES.get("imagen")
        if not archivo:
            return Response({"detail": "Adjunte un archivo en el campo 'imagen'."}, status=status.HTTP_400_BAD_REQUEST)
        tipos_permitidos = {
            "image/jpeg": ".jpg",
            "image/png": ".png",
            "image/webp": ".webp",
        }
        extension = tipos_permitidos.get(archivo.content_type)
        if not extension:
            return Response(
                {"detail": "Formato no permitido. Use JPG, PNG o WEBP."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if archivo.size > 5 * 1024 * 1024:
            return Response(
                {"detail": "La imagen no puede superar los 5 MB."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        nombre_archivo = os.path.join("sectores", f"sector_{sector.pk}_{uuid.uuid4().hex[:10]}{extension}")
        ruta_guardada = default_storage.save(nombre_archivo, archivo)
        url_publica = f"{settings.MEDIA_URL}{ruta_guardada.replace(os.sep, '/')}"
        sector.fotografias = [url_publica]
        sector.save(update_fields=["fotografias"])
        return Response(self.get_serializer(sector).data, status=status.HTTP_200_OK)

class ServicioViewSet(CatalogoViewSet):
    serializer_class = ServicioSerializer

    def get_queryset(self):
        consulta = Servicio.objects.all()
        usuario = self.request.user
        if not (usuario.is_authenticated and usuario.es_admin):
            consulta = consulta.filter(estado=Servicio.Estado.ACTIVO)
        return consulta

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
        if params.get("vista") == "proximas":
            consulta = consulta.filter(fecha__gte=timezone.localdate(), estado__in=services.ESTADOS_QUE_BLOQUEAN) \
                .order_by("fecha", "hora_inicio", "id_reserva")
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
        serializador = CancelacionSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        reserva = services.cancelar_reserva(reserva=self.get_object(), solicitante=request.user,
                                            origen=serializador.validated_data.get("origen"))
        return Response(self.get_serializer(self.get_queryset().get(pk=reserva.pk)).data)

    @action(detail=True, methods=["get"], url_path="simular-cancelacion")
    def simular_cancelacion(self, request, pk=None):
        """Muestra antes de confirmar cuánto se devolvería según las condiciones de arriendo."""
        serializador = CancelacionSerializer(data=request.query_params)
        serializador.is_valid(raise_exception=True)
        datos = services.simular_cancelacion(reserva=self.get_object(), solicitante=request.user,
                                             origen=serializador.validated_data.get("origen"))
        return Response({**datos, "monto_pagado": str(datos["monto_pagado"]),
                         "monto_devolucion": str(datos["monto_devolucion"])})

    @action(detail=True, methods=["post"], url_path="registrar-devolucion", permission_classes=[EsAdministrador])
    def registrar_devolucion(self, request, pk=None):
        reserva = services.registrar_devolucion(reserva=self.get_object())
        return Response(self.get_serializer(self.get_queryset().get(pk=reserva.pk)).data)

    @action(detail=True, methods=["post"])
    def modificar(self, request, pk=None):
        """«Editar selección»: actualiza la misma reserva pendiente en lugar de crear una nueva."""
        reserva = self.get_object()
        serializador = ReservaModificarSerializer(data=request.data, context={"request": request})
        serializador.is_valid(raise_exception=True)
        datos = serializador.validated_data
        services.modificar_reserva(
            reserva=reserva, solicitante=request.user, fecha=datos["fecha"], hora_inicio=datos["hora_inicio"],
            sectores=datos["sectores"], servicios=datos.get("servicios", []),
        )
        return Response(self.get_serializer(self.get_queryset().get(pk=reserva.pk)).data)

    @action(detail=False, methods=["get"], permission_classes=[EsAdministrador])
    def resumen(self, request):
        """Métricas del panel calculadas sobre todas las reservas (no solo la página visible)."""
        hoy = timezone.localdate()
        vigentes = Reserva.objects.filter(estado__in=services.ESTADOS_QUE_BLOQUEAN)
        saldo = Decimal("0.00")
        con_saldo = 0
        for reserva in vigentes.annotate(total_pagado=Coalesce(
            Sum("pagos__monto", filter=Q(pagos__estado_pago=Pago.Estado.APROBADO)),
            Value(Decimal("0.00")), output_field=DecimalField(max_digits=12, decimal_places=2),
        )):
            if reserva.saldo_pendiente > 0:
                saldo += reserva.saldo_pendiente
                con_saldo += 1
        cobrado_mes = Pago.objects.filter(
            estado_pago=Pago.Estado.APROBADO, fecha_pago__year=hoy.year, fecha_pago__month=hoy.month,
        ).aggregate(t=Sum("monto"))["t"] or Decimal("0.00")
        devoluciones = Reserva.objects.filter(
            estado=Reserva.Estado.CANCELADO, monto_devolucion__gt=0, devolucion_realizada_en__isnull=True,
        )
        return Response({
            "total_reservas": Reserva.objects.count(),
            "proximas": vigentes.filter(fecha__gte=hoy).count(),
            "saldo_por_cobrar": str(saldo),
            "reservas_con_saldo": con_saldo,
            "cobrado_mes": str(cobrado_mes),
            "devoluciones_pendientes": devoluciones.count(),
            "monto_devoluciones_pendientes": str(devoluciones.aggregate(t=Sum("monto_devolucion"))["t"] or 0),
        })

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

    @action(detail=True, methods=["get", "post"], url_path="validar-acceso", permission_classes=[EsAdministrador])
    def validar_acceso(self, request, pk=None):
        reserva = self.get_object()
        if request.method == "GET":
            return Response(ValidacionAccesoSerializer(reserva.validaciones_acceso.all(), many=True).data)
        serializador = ValidacionAccesoCrearSerializer(data=request.data)
        serializador.is_valid(raise_exception=True)
        validacion = services.registrar_validacion_acceso(
            reserva=reserva,
            tipo_registro=serializador.validated_data["tipo_registro"],
            validado_por=request.user,
        )
        return Response(ValidacionAccesoSerializer(validacion).data, status=status.HTTP_201_CREATED)

    @action(detail=True, methods=["post"], url_path="enviar-correo")
    def enviar_correo(self, request, pk=None):
        reserva = self.get_object()
        try:
            services.enviar_correo_reserva(reserva.pk, motivo="detalle", fail_silently=False)
        except Exception:
            return Response(
                {"detail": "No se pudo enviar el correo en este momento. Verifica la configuración SMTP."},
                status=status.HTTP_502_BAD_GATEWAY,
            )
        return Response({"detail": f"Correo con los detalles de la reserva enviado a {reserva.usuario.correo}."})

    @action(detail=False, methods=["get"], url_path="exportar", permission_classes=[EsAdministrador])
    def exportar(self, request):
        """HU-08: reporte de reservas en CSV (por defecto) o en Excel (?formato=xlsx)."""
        filas = (
            (r.pk, r.usuario.nombre_completo, r.usuario.correo, r.fecha, r.hora_inicio, r.horas_reservadas,
             r.estado, r.monto_total, r.monto_pagado, r.saldo_pendiente)
            for r in self.get_queryset()
        )
        return respuesta_exportacion(
            "reservas_altos_de_carpay", "Reservas",
            ["N° reserva", "Cliente", "Correo", "Fecha", "Hora inicio", "Horas", "Estado",
             "Monto total", "Monto pagado", "Saldo pendiente"],
            filas, request.query_params.get("formato"),
        )

class ReviewViewSet(mixins.ListModelMixin, mixins.CreateModelMixin, viewsets.GenericViewSet):
    serializer_class = ReviewSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        usuario = self.request.user
        consulta = Review.objects.select_related("reserva", "evaluador", "evaluado")
        if usuario.es_admin:
            return consulta
        return consulta.filter(Q(evaluador=usuario) | Q(evaluado=usuario))


class BitacoraIncidenciaViewSet(
    mixins.ListModelMixin,
    mixins.RetrieveModelMixin,
    mixins.CreateModelMixin,
    viewsets.GenericViewSet,
):
    serializer_class = BitacoraIncidenciaSerializer
    permission_classes = [EsAdministrador]

    def get_queryset(self):
        consulta = BitacoraIncidencia.objects.select_related("reserva", "autor", "sector")
        reserva_id = self.request.query_params.get("reserva", "")
        if reserva_id.isdigit():
            consulta = consulta.filter(reserva_id=int(reserva_id))
        return consulta

    @action(detail=False, methods=["get"], url_path="exportar")
    def exportar(self, request):
        """HU-08: reporte de incidencias de la bitácora (CSV o ?formato=xlsx)."""
        filas = (
            (f"INC-{i.pk:04d}", i.reserva_id, i.fecha_creacion, i.autor.nombre_completo, i.titulo,
             i.descripcion, i.sector.nombre if i.sector_id else "", i.area_no_contratada)
            for i in self.get_queryset()
        )
        return respuesta_exportacion(
            "incidencias_altos_de_carpay", "Incidencias",
            ["N° incidencia", "N° reserva", "Fecha y hora", "Autor", "Título", "Descripción", "Área",
             "Área no contratada"],
            filas, request.query_params.get("formato"),
        )

from django.shortcuts import render
from rest_framework import viewsets, permissions
from rest_framework.views import APIView
from django.http import HttpResponse
import pandas as pd
from .models import ModeloUsuario,ModeloSector, ModeloServicio, ModeloReserva, ModeloReservaSector, ModeloReservaServcio, ModeloLogsModificacion, ModeloPago, ModeloValidacionAcceso, ModeloReview
from .serializers import UsuarioSerializer, SectorSerializer, ServicioSerializer, ReservaSerializer, ReservaSectorSerializer, ReservaServicioSerializer, LogsModificacionSerializer, PagoSerialize, ValidacionAccesoSerializer, ReviewSerializer

# Create your views here.
class UsuarioViewSet(viewsets.ModelViewSet):
    queryset = ModeloUsuario.objects.all()
    serializer_class = UsuarioSerializer
    permission_classes = [permissions.IsAuthenticated]

class SectorViewSet(viewsets.ModelViewSet):
    queryset = ModeloSector.objects.all()
    serializer_class = SectorSerializer
    permission_classes = [permissions.IsAuthenticated]

class ServicioViewSet(viewsets.ModelViewSet):
    queryset = ModeloServicio.objects.all()
    serializer_class = ServicioSerializer
    permission_classes = [permissions.IsAuthenticated]

class ReservaViewSet(viewsets.ModelViewSet):
    queryset = ModeloReserva.objects.all()
    serializer_class = ReservaSerializer
    permission_classes = [permissions.IsAuthenticated]

class ReservaSectorViewSet(viewsets.ModelViewSet):
    queryset = ModeloReservaSector.objects.all()
    serializer_class = ReservaSectorSerializer
    permission_classes = [permissions.IsAuthenticated]

class ReservaServicioViewSet(viewsets.ModelViewSet):
    queryset = ModeloReservaServcio.objects.all()
    serializer_class = ReservaServicioSerializer
    permission_classes = [permissions.IsAuthenticated]

class LogsModificacionViewSet(viewsets.ModelViewSet):
    queryset = ModeloLogsModificacion.objects.all()
    serializer_class = LogsModificacionSerializer
    permission_classes = [permissions.IsAuthenticated]

class PagoViewSet(viewsets.ModelViewSet):
    queryset = ModeloPago.objects.all()
    serializer_class = PagoSerialize
    permission_classes = [permissions.IsAuthenticated]

class ValidacionAccesoViewSet(viewsets.ModelViewSet):
    queryset = ModeloValidacionAcceso.objects.all()
    serializer_class = ValidacionAccesoSerializer
    permission_classes = [permissions.IsAuthenticated]

class ReviewViewSet(viewsets.ModelViewSet):
    queryset = ModeloReview.objects.all()
    serializer_class = ReviewSerializer
    permission_classes = [permissions.IsAuthenticated]

class ExportarReporteExcelView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    def get(self, request):
        reservas = ModeloReserva.objects.all().values(
            'id', 'usuario__nombre', 'usuario__rut', 'fecha'
            'horas_reservadas', 'monto_total', 'estado', 'fecha_creacion'
        )
        df = pd.DataFrame(list(reservas))
        df.rename(columns={
            'usuario__nombre': 'Cliente',
            'usuario__rut': 'RUT',
            'horas_reservadas': 'Horas',
            'monto_total': 'Monto ($)',
            'fecha_creacion': 'Fecha Creación'
        }, inplace= True)

        response = HttpResponse(
            content_type ='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        )
        response['Content-Disposition'] = 'attachment; filename="Reporte_Reservas_Altos_De_Carpay.xlsx"'

        with pd.ExcelWriter(response, engine= 'openpyxl') as writer:
            df.to_excel(writer, sheet_name= 'Reservas', index= False)
        return response
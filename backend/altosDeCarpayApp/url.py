from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    UsuarioViewSet, SectorViewSet, ServicioViewSet, ReservaViewSet, 
    ReservaSectorViewSet, ReservaServicioViewSet, LogsModificacionViewSet, 
    PagoViewSet, ValidacionAccesoViewSet, ReviewViewSet, ExportarReporteExcelView
)

router = DefaultRouter()
router.register(r'usuarios', UsuarioViewSet)
router.register(r'sectores', SectorViewSet)
router.register(r'servicios', ServicioViewSet)
router.register(r'reservas', ReservaViewSet)
router.register(r'reserva-sectores', ReservaSectorViewSet)
router.register(r'reserva-servicios', ReservaServicioViewSet)
router.register(r'logs', LogsModificacionViewSet)
router.register(r'pagos', PagoViewSet)
router.register(r'validacion-acceso', ValidacionAccesoViewSet)
router.register(r'reviews', ReviewViewSet)

urlpatterns = [
    path('', include(router.urls)),
    path('reportes/exportar-excel/', ExportarReporteExcelView.as_view(), name='exportar_excel'),
]
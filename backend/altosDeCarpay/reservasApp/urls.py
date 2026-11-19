from rest_framework.routers import SimpleRouter
from .views import BitacoraIncidenciaViewSet, ReservaViewSet, ReviewViewSet, SectorViewSet, ServicioViewSet

router = SimpleRouter()
router.register("sectores", SectorViewSet, basename="sector")
router.register("servicios", ServicioViewSet, basename="servicio")
router.register("reservas", ReservaViewSet, basename="reserva")
router.register("reviews", ReviewViewSet, basename="review")
router.register("incidencias", BitacoraIncidenciaViewSet, basename="incidencia")

urlpatterns = router.urls
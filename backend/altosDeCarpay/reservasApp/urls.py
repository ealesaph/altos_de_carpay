from rest_framework.routers import SimpleRouter
from .views import ReservaViewSet, ReviewViewSet, SectorViewSet, ServicioViewSet

router = SimpleRouter()
router.register("sectores", SectorViewSet, basename="sector")
router.register("servicios", ServicioViewSet, basename="servicio")
router.register("reservas", ReservaViewSet, basename="reserva")
router.register("reviews", ReviewViewSet, basename="review")

urlpatterns = router.urls
from rest_framework.routers import SimpleRouter
from .views import ValidacionAccesoViewSet

router = SimpleRouter()
router.register("accesos", ValidacionAccesoViewSet, basename="acceso")

urlpatterns = router.urls
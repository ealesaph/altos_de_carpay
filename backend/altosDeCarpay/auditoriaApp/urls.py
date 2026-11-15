from rest_framework.routers import SimpleRouter
from .views import LogModificacionViewSet

router = SimpleRouter()
router.register("auditoria/logs", LogModificacionViewSet, basename="log")

urlpatterns = router.urls
from rest_framework.routers import SimpleRouter
from .views import ConsultaViewSet

router = SimpleRouter()
router.register("consultas", ConsultaViewSet, basename="consulta")
urlpatterns = router.urls
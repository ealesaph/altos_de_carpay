from django.urls import path
from rest_framework.routers import SimpleRouter
from rest_framework_simplejwt.views import TokenRefreshView
from .views import (
    CambiarPasswordView,
    DesactivarCuentaArcoView,
    LoginView,
    LogoutView,
    MisDatosArcoView,
    PerfilView,
    RegistroView,
    UsuarioAdminViewSet,
)

router = SimpleRouter()
router.register("usuarios", UsuarioAdminViewSet, basename="usuario")

urlpatterns = [
    path("auth/registro/", RegistroView.as_view(), name="registro"),
    path("auth/login/", LoginView.as_view(), name="login"),
    path("auth/refresh/", TokenRefreshView.as_view(), name="token_refresh"),
    path("auth/logout/", LogoutView.as_view(), name="logout"),
    path("auth/perfil/", PerfilView.as_view(), name="perfil"),
    path("auth/cambiar-password/", CambiarPasswordView.as_view(), name="cambiar_password"),
    path("auth/mis-datos/", MisDatosArcoView.as_view(), name="mis_datos_arco"),
    path("auth/desactivar-cuenta/", DesactivarCuentaArcoView.as_view(), name="desactivar_cuenta_arco"),
] + router.urls
from rest_framework_simplejwt.authentication import JWTAuthentication

from auditoriaApp.contexto import fijar_usuario_actual


class JWTAuthenticationAuditada(JWTAuthentication):
    def authenticate(self, request):
        resultado = super().authenticate(request)
        if resultado is not None:
            fijar_usuario_actual(resultado[0])
        return resultado

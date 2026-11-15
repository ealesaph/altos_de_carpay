from .contexto import fijar_usuario_actual, limpiar_usuario_actual


class UsuarioActualMiddleware:
    
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        usuario = getattr(request, "user", None)
        if usuario is not None and usuario.is_authenticated:
            fijar_usuario_actual(usuario)
        try:
            return self.get_response(request)
        finally:
            limpiar_usuario_actual()
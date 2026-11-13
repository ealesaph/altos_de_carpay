from rest_framework.permissions import BasePermission

class EsAdministrador(BasePermission):
    message = "Se requieren permisos de administrador."

    def has_permission(self, request, view):
        usuario = request.user
        return bool(usuario and usuario.is_authenticated and usuario.es_admin)

class EsCliente(BasePermission):
    message = "Esta operación está disponible solo para clientes."

    def has_permission(self, request, view):
        usuario = request.user
        return bool(usuario and usuario.is_authenticated and usuario.es_cliente)
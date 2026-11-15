from django.contrib import admin
from .models import LogModificacion


@admin.register(LogModificacion)
class LogModificacionAdmin(admin.ModelAdmin):
    list_display = ("id_log", "fecha", "usuario", "tabla_modificada", "id_registro")
    list_filter = ("tabla_modificada",)
    search_fields = ("id_registro",)

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False
from django.contrib import admin
from .models import BrechaSeguridad, LogModificacion


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


@admin.register(BrechaSeguridad)
class BrechaSeguridadAdmin(admin.ModelAdmin):
    list_display = ("id_brecha", "fecha_deteccion", "nivel_riesgo", "titulares_afectados",
                    "notificada_agencia", "notificados_titulares")
    list_filter = ("nivel_riesgo", "notificada_agencia", "notificados_titulares")
    readonly_fields = ("registrada_por", "fecha_registro")

    def save_model(self, request, obj, form, change):
        if not change:
            obj.registrada_por = request.user
        super().save_model(request, obj, form, change)

    def has_delete_permission(self, request, obj=None):
        return False

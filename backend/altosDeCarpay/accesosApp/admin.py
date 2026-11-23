from django.contrib import admin
from .models import ValidacionAcceso


@admin.register(ValidacionAcceso)
class ValidacionAccesoAdmin(admin.ModelAdmin):
    list_display = ("id_validacion", "reserva", "tipo_registro", "fecha_hora", "validado_por")
    list_filter = ("tipo_registro",)

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False
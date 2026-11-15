from django.contrib import admin
from .models import Consulta

@admin.register(Consulta)
class ConsultaAdmin(admin.ModelAdmin):
    list_display = ("id_consulta", "cliente", "asunto", "estado", "fecha_envio")
    list_filter = ("estado",)
    search_fields = ("asunto", "cliente__correo")
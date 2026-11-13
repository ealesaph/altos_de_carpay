from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from django.contrib.auth.forms import UserChangeForm, UserCreationForm

from .models import Usuario


class UsuarioCreationForm(UserCreationForm):
    class Meta:
        model = Usuario
        fields = ("correo", "rut", "nombre", "apellido_paterno", "apellido_materno", "rol")


class UsuarioChangeForm(UserChangeForm):
    class Meta:
        model = Usuario
        fields = "__all__"


@admin.register(Usuario)
class UsuarioAdmin(UserAdmin):
    form = UsuarioChangeForm
    add_form = UsuarioCreationForm
    ordering = ("correo",)
    list_display = ("correo", "rut", "nombre", "apellido_paterno", "rol", "estado", "ultima_conexion")
    list_filter = ("rol", "estado")
    search_fields = ("correo", "rut", "nombre", "apellido_paterno")
    readonly_fields = ("ultima_conexion", "consentimiento_datos")
    fieldsets = (
        (None, {"fields": ("correo", "password")}),
        ("Datos personales", {"fields": ("rut", "nombre", "apellido_paterno", "apellido_materno", "telefono")}),
        ("Rol y estado", {"fields": ("rol", "estado", "is_superuser", "groups", "user_permissions")}),
        ("Trazabilidad", {"fields": ("ultima_conexion", "consentimiento_datos")}),
    )
    add_fieldsets = (
        (None, {
            "classes": ("wide",),
            "fields": ("correo", "rut", "nombre", "apellido_paterno", "apellido_materno",
                       "rol", "password1", "password2"),
        }),
    )

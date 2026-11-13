from django.apps import AppConfig


class UsuariosAppConfig(AppConfig):
    default_auto_field = "django.db.models.AutoField"
    name = "usuariosApp"
    verbose_name = "Usuarios"

    def ready(self):
        from django.contrib.auth.signals import user_logged_in

        from .signals import registrar_ultima_conexion

        user_logged_in.connect(registrar_ultima_conexion, dispatch_uid="registrar_ultima_conexion")

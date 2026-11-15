from django.apps import AppConfig


class AuditoriaAppConfig(AppConfig):
    default_auto_field = "django.db.models.AutoField"
    name = "auditoriaApp"
    verbose_name = "Auditoría"

    def ready(self):
        from django.db.models.signals import post_delete, post_save, pre_save

        from . import signals

        for etiqueta in signals.MODELOS_AUDITADOS:
            modelo = self.apps.get_model(etiqueta)
            pre_save.connect(signals.guardar_estado_previo, sender=modelo,
                             dispatch_uid=f"auditoria_pre_{etiqueta}")
            post_save.connect(signals.registrar_guardado, sender=modelo,
                              dispatch_uid=f"auditoria_post_{etiqueta}")
            post_delete.connect(signals.registrar_eliminacion, sender=modelo,
                                dispatch_uid=f"auditoria_del_{etiqueta}")
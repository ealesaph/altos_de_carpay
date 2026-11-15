from django.conf import settings
from django.db import models


class LogModificacion(models.Model):

    id_log = models.AutoField(primary_key=True)
    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL,
        related_name="logs_modificacion", db_column="id_usuario",
    )
    fecha = models.DateTimeField(auto_now_add=True)
    tabla_modificada = models.CharField(max_length=50)
    id_registro = models.IntegerField()
    valor_previo = models.TextField(null=True, blank=True)
    valor_nuevo = models.TextField(null=True, blank=True)

    class Meta:
        db_table = "logs_modificacion"
        ordering = ["-fecha", "-id_log"]
        indexes = [models.Index(fields=["tabla_modificada", "id_registro"])]

    def __str__(self):
        return f"{self.tabla_modificada}#{self.id_registro} ({self.fecha:%Y-%m-%d %H:%M})"

    def save(self, *args, **kwargs):
        if self.pk is not None:
            raise PermissionError("Los registros de auditoría no pueden modificarse.")
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise PermissionError("Los registros de auditoría no pueden eliminarse.")
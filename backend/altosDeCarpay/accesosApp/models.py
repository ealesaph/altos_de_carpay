from django.conf import settings
from django.db import models


class ValidacionAcceso(models.Model):

    class Tipo(models.TextChoices):
        ENTRADA = "Entrada", "Entrada"
        SALIDA = "Salida", "Salida"

    id_validacion = models.AutoField(primary_key=True)
    reserva = models.ForeignKey("reservasApp.Reserva", on_delete=models.PROTECT,
                                related_name="validaciones_acceso", db_column="id_reserva")
    token_qr = models.CharField(max_length=255)
    tipo_registro = models.CharField(max_length=10, choices=Tipo.choices)
    fecha_hora = models.DateTimeField(auto_now_add=True)
    validado_por = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
                                     related_name="validaciones_realizadas", db_column="validado_por")

    class Meta:
        db_table = "validacion_acceso"
        ordering = ["-fecha_hora"]
        constraints = [
            models.UniqueConstraint(fields=["token_qr", "tipo_registro"], name="uq_token_tipo"),
            models.UniqueConstraint(fields=["reserva", "tipo_registro"], name="uq_reserva_tipo"),
        ]

    def __str__(self):
        return f"{self.tipo_registro} reserva {self.reserva_id}"

    def save(self, *args, **kwargs):
        if self.pk is not None:
            raise PermissionError("Los registros de acceso no pueden modificarse.")
        super().save(*args, **kwargs)
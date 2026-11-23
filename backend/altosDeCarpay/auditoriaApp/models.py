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


class BrechaSeguridad(models.Model):
    """Registro de incidentes de seguridad que afectan datos personales (Ley N° 21.719).

    Permite documentar la detección, las medidas adoptadas y las notificaciones a la
    Agencia de Protección de Datos Personales y a los titulares afectados.
    """

    class Riesgo(models.TextChoices):
        BAJO = "Bajo", "Bajo"
        MEDIO = "Medio", "Medio"
        ALTO = "Alto", "Alto"

    id_brecha = models.AutoField(primary_key=True)
    fecha_deteccion = models.DateTimeField()
    descripcion = models.TextField(max_length=2000)
    datos_afectados = models.CharField(max_length=255, help_text="Ej.: correos y teléfonos de clientes.")
    titulares_afectados = models.PositiveIntegerField(default=0)
    nivel_riesgo = models.CharField(max_length=10, choices=Riesgo.choices, default=Riesgo.MEDIO)
    medidas_adoptadas = models.TextField(max_length=2000, blank=True, default="")
    notificada_agencia = models.BooleanField(default=False)
    fecha_notificacion_agencia = models.DateTimeField(null=True, blank=True)
    notificados_titulares = models.BooleanField(default=False)
    registrada_por = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL,
        related_name="brechas_registradas", db_column="registrada_por",
    )
    fecha_registro = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "brecha_seguridad"
        ordering = ["-fecha_deteccion"]
        verbose_name = "brecha de seguridad"
        verbose_name_plural = "brechas de seguridad"

    def __str__(self):
        return f"Brecha #{self.pk} ({self.fecha_deteccion:%Y-%m-%d}) – riesgo {self.nivel_riesgo}"

    def delete(self, *args, **kwargs):
        raise PermissionError("El registro de brechas no puede eliminarse; es evidencia ante la Agencia.")

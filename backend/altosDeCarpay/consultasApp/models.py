from django.conf import settings
from django.db import models

class Consulta(models.Model):

    class Estado(models.TextChoices):
        PENDIENTE = "Pendiente", "Pendiente"
        RESPONDIDA = "Respondida", "Respondida"

    id_consulta = models.AutoField(primary_key=True)
    cliente = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
                                related_name="consultas", db_column="id_cliente")
    asunto = models.CharField(max_length=150)
    mensaje = models.TextField(max_length=2000)
    fecha_envio = models.DateTimeField(auto_now_add=True)
    respuesta = models.TextField(max_length=2000, blank=True, default="")
    fecha_respuesta = models.DateTimeField(null=True, blank=True)
    respondida_por = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True,
                                       on_delete=models.SET_NULL, related_name="consultas_respondidas",
                                       db_column="respondida_por")
    estado = models.CharField(max_length=20, choices=Estado.choices, default=Estado.PENDIENTE)

    class Meta:
        db_table = "consulta"
        ordering = ["-fecha_envio"]

    def __str__(self):
        return f"Consulta {self.pk}: {self.asunto}"
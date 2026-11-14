import secrets
from datetime import datetime, timedelta
from decimal import ROUND_HALF_UP, Decimal
from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models
from django.db.models import CompositePrimaryKey
from django.db.models import Sum
from django.utils import timezone


def generar_token_qr():
    return secrets.token_urlsafe(32)

class Sector(models.Model):

    class Estado(models.TextChoices):
        DISPONIBLE = "Disponible", "Disponible"
        MANTENCION = "Mantencion", "Mantención"
        INACTIVO = "Inactivo", "Inactivo"

    id_sector = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=50)
    descripcion = models.TextField(blank=True, default="")
    fotografias = models.JSONField(default=list, blank=True)  # Lista de URLs (JSONB en PostgreSQL)
    valor_hora = models.DecimalField(max_digits=10, decimal_places=2,
                                     validators=[MinValueValidator(Decimal("0"))])
    estado = models.CharField(max_length=20, choices=Estado.choices, default=Estado.DISPONIBLE)

    class Meta:
        db_table = "sector"
        ordering = ["nombre"]

    def __str__(self):
        return self.nombre


class Servicio(models.Model):

    class Estado(models.TextChoices):
        ACTIVO = "Activo", "Activo"
        INACTIVO = "Inactivo", "Inactivo"

    id_servicio = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    descripcion = models.CharField(max_length=255, blank=True, default="")
    valor = models.DecimalField(max_digits=10, decimal_places=2,
                                validators=[MinValueValidator(Decimal("0"))])
    estado = models.CharField(max_length=20, choices=Estado.choices, default=Estado.ACTIVO)

    class Meta:
        db_table = "servicio"
        ordering = ["nombre"]

    def __str__(self):
        return self.nombre

class Reserva(models.Model):

    class Estado(models.TextChoices):
        PENDIENTE = "Pendiente", "Pendiente"
        EN_PROCESO = "En Proceso", "En Proceso"
        CONFIRMADO = "Confirmado", "Confirmado"
        CANCELADO = "Cancelado", "Cancelado"

    id_reserva = models.AutoField(primary_key=True)
    usuario = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
                                related_name="reservas", db_column="id_usuario")
    fecha = models.DateField()
    hora_inicio = models.TimeField(null=True, blank=True)
    horas_reservadas = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(24)])
    monto_total = models.DecimalField(max_digits=10, decimal_places=2)
    estado = models.CharField(max_length=20, choices=Estado.choices, default=Estado.PENDIENTE)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    condiciones_aceptadas_en = models.DateTimeField(null=True, blank=True, editable=False)
    token_qr = models.CharField(max_length=64, unique=True, default=generar_token_qr, editable=False)

    class Meta:
        db_table = "reserva"
        ordering = ["-fecha", "-hora_inicio"]
        indexes = [models.Index(fields=["fecha", "estado"])]

    def __str__(self):
        return f"Reserva {self.pk} - {self.fecha} ({self.estado})"

    @property
    def inicio(self):
        if self.hora_inicio is None:
            return None
        return timezone.make_aware(datetime.combine(self.fecha, self.hora_inicio))

    @property
    def fin(self):
        inicio = self.inicio
        return inicio + timedelta(hours=self.horas_reservadas) if inicio else None
    
    @property
    def monto_pagado(self):
        anotado = getattr(self, "total_pagado", None)
        if anotado is not None:
            return anotado
        total = self.pagos.filter(estado_pago=Pago.Estado.APROBADO).aggregate(t=Sum("monto"))["t"]
        return total or Decimal("0.00")

    @property
    def saldo_pendiente(self):
        return max(self.monto_total - self.monto_pagado, Decimal("0.00"))

    @property
    def monto_anticipo(self):
        bruto = self.monto_total * settings.ANTICIPO_PORCENTAJE / Decimal("100")
        return bruto.quantize(Decimal("1"), rounding=ROUND_HALF_UP)

    @property
    def anticipo_pagado(self):
        return self.monto_pagado >= self.monto_anticipo

    @property
    def plazo_anticipo_vence(self):
        return self.fecha_creacion + timedelta(hours=settings.PLAZO_PAGO_ANTICIPO_HORAS)

    @property
    def plazo_anticipo_vencido(self):
        return timezone.now() > self.plazo_anticipo_vence


class ReservaSector(models.Model):

    reserva = models.ForeignKey(Reserva, on_delete=models.CASCADE,
                                related_name="sectores_reservados", db_column="id_reserva")
    sector = models.ForeignKey(Sector, on_delete=models.CASCADE,
                               related_name="reservas_sector", db_column="id_sector")
    pk = CompositePrimaryKey("reserva", "sector")
    horas = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(24)])
    valor_hora = models.DecimalField(max_digits=10, decimal_places=2)

    class Meta:
        db_table = "reserva_sector"

    @property
    def subtotal(self):
        return self.valor_hora * self.horas

    @property
    def inicio(self):
        return self.reserva.inicio

    @property
    def fin(self):
        inicio = self.reserva.inicio
        return inicio + timedelta(hours=self.horas) if inicio else None


class ReservaServicio(models.Model):

    reserva = models.ForeignKey(Reserva, on_delete=models.CASCADE,
                                related_name="servicios_reservados", db_column="id_reserva")
    servicio = models.ForeignKey(Servicio, on_delete=models.CASCADE,
                                 related_name="reservas_servicio", db_column="id_servicio")
    pk = CompositePrimaryKey("reserva", "servicio")
    cantidad = models.IntegerField(default=1, validators=[MinValueValidator(1), MaxValueValidator(500)])
    valor_unitario = models.DecimalField(max_digits=10, decimal_places=2)

    class Meta:
        db_table = "reserva_servicio"

    @property
    def subtotal(self):
        return self.valor_unitario * self.cantidad


class Pago(models.Model):

    class Tipo(models.TextChoices):
        ANTICIPO = "Anticipo", "Anticipo"
        SALDO = "Saldo", "Saldo pendiente"
        TOTAL = "Total", "Pago total"

    class Metodo(models.TextChoices):
        WEBPAY = "Webpay", "Webpay"
        TRANSFERENCIA = "Transferencia", "Transferencia"
        EFECTIVO = "Efectivo", "Efectivo"

    class Estado(models.TextChoices):
        PENDIENTE = "Pendiente", "Pendiente"
        APROBADO = "Aprobado", "Aprobado"
        RECHAZADO = "Rechazado", "Rechazado"

    id_pago = models.AutoField(primary_key=True)
    reserva = models.ForeignKey(Reserva, on_delete=models.PROTECT,
                                related_name="pagos", db_column="id_reserva")
    monto = models.DecimalField(max_digits=10, decimal_places=2)
    tipo_pago = models.CharField(max_length=10, choices=Tipo.choices, null=True, blank=True)
    metodo_pago = models.CharField(max_length=20, choices=Metodo.choices, null=True, blank=True)
    transaccion_id = models.CharField(max_length=100, null=True, blank=True)
    estado_pago = models.CharField(max_length=20, choices=Estado.choices, default=Estado.PENDIENTE)
    fecha_pago = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "pago"
        ordering = ["-fecha_pago"]

    def __str__(self):
        return f"Pago {self.pk} - {self.monto} ({self.estado_pago})"


class Review(models.Model):

    id_review = models.AutoField(primary_key=True)
    reserva = models.ForeignKey(Reserva, on_delete=models.PROTECT,
                                related_name="reviews", db_column="id_reserva")
    evaluador = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
                                  related_name="reviews_emitidas", db_column="id_evaluador")
    evaluado = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
                                 related_name="reviews_recibidas", db_column="id_evaluado")
    calificacion = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(5)])
    comentario = models.CharField(max_length=255, blank=True, default="")
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "review"
        ordering = ["-fecha"]

    def __str__(self):
        return f"Review {self.pk} ({self.calificacion}/5)"

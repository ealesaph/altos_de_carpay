from django.db import models
from django.core.validators import MinValueValidator, MaxValueValidator

# Create your models here.
class RolUsuario(models.TextChoices):
    CLIENTE = 'Cliente', 'Cliente'
    ADMIN = 'Admin', 'Admin'

class EstadoUsuario(models.TextChoices):
    ACTIVO = 'Activo', 'Activo'
    INACTIVO = 'Inactivo', 'inactivo'

class EstadoSector(models.TextChoices):
    DISPONIBLE = 'Disponible', 'Disponible'
    MANTENCION = 'Mantención', 'Mantención'
    INACTIVO = 'Inactivo', 'Inactivo'

class EstadoServicio(models.TextChoices):
    ACTIVO = 'Activo', 'Activo'
    INACTIVO = 'Inactivo', 'Inactivo'

class EstadoReserva(models.TextChoices):
    PENDIENTE = 'Pendiente', 'Pendiente'
    EN_PROCESO = 'En Proceso', 'En Proceso'
    CONFIRMADO = 'Confirmado', 'Confirmado'
    CANCELADO = 'Cancelado', 'Cancelado'

class MetodoPago(models.TextChoices):
    WEBPAY = 'Webpay', 'Webpay'
    TRANSFERENCIA = 'Transferencia', 'Transferencia'
    EFECTIVO = 'Efectivo', 'Efectivo'

class EstadoPago(models.TextChoices):
    PENDIENTE = 'Pendiente', 'Pendiente' 
    APROBADO = 'Aprobado', 'Aprobado'
    RECHAZADO = 'Rechazado', 'Rechazado'

class TipoRegistroAcceso(models.TextChoices):
    ENTRADA = 'Entrada', 'Entrada'
    SALIDA = 'Salida', 'Salida'


class ModeloUsuario(models.Model):
    rut = models.CharField(max_length=12, unique= True, null= False, verbose_name='Rut')
    nombre = models.CharField(max_length=50, null= False, verbose_name= 'Nombre')
    apellido_paterno = models.CharField(max_length=50, null= False, verbose_name= 'Apellido Paterno')
    apellido_materno = models.CharField(max_length=50, null= False, verbose_name= 'Apellido Materno')
    correo = models.EmailField(max_length=100, unique= True, null= False, verbose_name= 'Correo')
    password = models.CharField(max_length=255, null= False, verbose_name= 'Password')
    ultima_conexion = models.DateTimeField(null= True, verbose_name= 'Ultima Conexion')
    rol = models.CharField(max_length=20, choices=RolUsuario.choices, default=RolUsuario.CLIENTE, null= False, verbose_name= 'Rol')
    estado = models.CharField(max_length=20, choices=EstadoUsuario.choices, default=EstadoUsuario.ACTIVO, verbose_name= 'Estado')

    class Meta:
        db_table = 'usuario'

    def __str__(self):
        return f"{self.nombre}, {self.apellido_paterno}, ({self.rut})"

class ModeloSector(models.Model):
    nombre = models.CharField(max_length=50, null= False, verbose_name= 'Nombre')
    valor_hora = models.DecimalField(max_digits=10, decimal_places=2, null= False, verbose_name= 'Valor Hora')
    estado =models.CharField(max_length=20, choices= EstadoSector.choices, default= EstadoSector.DISPONIBLE, null=False, verbose_name= 'Estado')

    class Meta:
        db_table = 'sector'

    def __str__(self):
        return self.nombre

class ModeloServicio(models.Model):
    nombre = models.CharField(max_length=100, null= False, verbose_name= 'Nombre')
    descripcion = models.CharField(max_length=255, null= True, blank= True, verbose_name= 'Descripcion')
    valor = models.DecimalField(max_digits=10, decimal_places=2, null= False, verbose_name= 'Valor')
    estado = models.CharField(max_length=20, choices=EstadoServicio.choices, default=EstadoServicio.ACTIVO, null= False, verbose_name= 'Estado')

    class Meta:
        db_table = 'servicio'

    def __str__(self):
        return self.nombre

class ModeloReserva(models.Model):
    id_usuario = models.ForeignKey(ModeloUsuario, on_delete= models.CASCADE, verbose_name= 'Id Usuario')
    fecha = models.DateField(null= False, verbose_name= 'Fecha')
    horas_reservadas = models.IntegerField(null= False, verbose_name= 'Horas Reservadas')
    monto_total = models.DecimalField(max_digits=10, decimal_places=2, null= False, verbose_name= 'Monto Total')
    fecha_creacion = models.DateTimeField(auto_now_add= True, verbose_name= 'Fecha Creacion')
    estado = models.CharField(max_length=20, choices=EstadoReserva.choices, default=EstadoReserva.PENDIENTE, null= False, verbose_name= 'Estado')

    class Meta:
        db_table = 'reserva'

    def __str__(self):
        return f"Reserva #{self.id} - {self.usuario.nombre}"

class ModeloReservaSector(models.Model):
    id_reserva = models.ForeignKey(ModeloReserva, on_delete= models.CASCADE, verbose_name= 'Id Reserva')
    id_sector = models.ForeignKey(ModeloSector, on_delete= models.CASCADE, verbose_name= 'Id Sector')
    horas = models.IntegerField(null= False, verbose_name= 'Horas')
    valor_hora = models.DecimalField(max_digits=10, decimal_places=2, null=False, verbose_name= 'Valor Hora')

    class Meta:
        db_table = 'reserva_sector'
        unique_together = (('reserva', 'sector'),)

    def __str__(self):
        return f"Reserva #{self.reserva.id} -> Sector: {self.sector.nombre}"

class ModeloReservaServcio(models.Model):
    id_reserva = models.ForeignKey(ModeloReserva, on_delete= models.CASCADE, verbose_name= 'Id Reserva')
    id_servicio = models.ForeignKey(ModeloServicio, on_delete= models.CASCADE, verbose_name= 'Id Servicio')
    cantidad = models.IntegerField(null= False, default=1, verbose_name= 'Cantidad')
    valor_unitario = models.DecimalField(max_digits=10, decimal_places=2, null= False, verbose_name= 'Valor Unitario')

    class Meta:
        db_table = 'reserva_servicio'
        unique_together = (('reserva', 'servicio'),)

    def __str__(self):
        return f"Reserva #{self.reserva.id} -> Servicio: {self.servicio.nombre}"

class ModeloLogsModificacion(models.Model):
    id_usuario = models.ForeignKey(ModeloUsuario, on_delete= models.CASCADE, verbose_name= 'Id Usuario')
    fecha = models.DateTimeField(auto_now_add= True, verbose_name= 'Fecha')
    tabla_modificada = models.CharField(max_length=50, null= False, verbose_name= 'Tabla Modificada')
    id_registro = models.IntegerField(null= False, verbose_name= 'Id Registro')
    valor_previo = models.TextField(null= True, blank= True, verbose_name= 'Valor Previo')
    valor_nuevo = models.TextField(null= True, blank= True, verbose_name= ' Valor Nuevo')

    class Meta:
        db_table = 'logs_modificacion'

    def __str__(self):
        return f"Log #{self.id} - {self.table_modificada} modificado por {self.usuario.nombre}"

class ModeloPago(models.Model):
    id_reserva = models.ForeignKey(ModeloReserva, on_delete= models.CASCADE, verbose_name= 'Id Reserva')
    monto = models.DecimalField(max_digits=10, decimal_places=2, null= False, verbose_name= 'Monto')
    transaccion_id = models.CharField(max_length=100, null= True, blank= True, verbose_name= 'Transaccion Id')
    fecha_pago = models.DateTimeField(auto_now_add= True, verbose_name= 'Fecha Pago')
    metodo_pago = models.CharField(max_digits=10, decimal_places=2, choices= MetodoPago.choices, null= False, verbose_name= 'Metodo Pago')
    estado_pago = models.CharField(max_length=9, choices=EstadoPago.choices, default=EstadoPago.PENDIENTE, null= False, verbose_name= 'Estado')

    class Meta:
        db_table = 'pago'

    def __str__(self):
        return f"Pago #{self.id} (Reserva #{self.reserva.id}) - {self.estado_pago}"

class ModeloValidacionAcceso(models.Model):
    id_reserva = models.ForeignKey(ModeloReserva, on_delete= models.CASCADE, verbose_name= 'Id Reserva')
    token_qr = models.CharField(max_length=255, unique= True, null= False, verbose_name= 'Token QR')
    fecha_hora = models.DateTimeField(auto_now_add= True, verbose_name= 'Fecha Hora')
    validar_por = models.ForeignKey(ModeloUsuario, on_delete=models.SET_NULL, null= True, verbose_name= 'Validar Por')
    tipo_registro = models.CharField(max_length=10, choices= TipoRegistroAcceso.choices, default= TipoRegistroAcceso.ENTRADA, null= False, verbose_name= 'Tipo Registro')

    class Meta:
        db_table = 'validacion_acceso'

    def __str__(self):
        return f"QR {self.tipo_registro} - Reserva #{self.reserva.id}"


class ModeloReview(models.Model):
    id_reserva = models.ForeignKey(ModeloReserva, on_delete= models.CASCADE, related_name= 'reviews_hechas', verbose_name= 'Id Reserva')
    id_evaluador = models.ForeignKey(ModeloUsuario, on_delete= models.CASCADE, related_name= 'reviews_recibidas', verbose_name= 'Id Evaluador')
    id_evaluado = models.ForeignKey(ModeloUsuario, on_delete= models. CASCADE, verbose_name= 'Id Evaluado')
    calificacion = models.IntegerField(validators= [MinValueValidator(1), MaxValueValidator(5)], null= False, verbose_name= 'Calificacion')
    comentario = models.CharField(max_length=255, null= True, blank= True, verbose_name= 'Comentario')
    fecha = models.DateTimeField(auto_now_add= True, verbose_name= 'Fecha')

    class Meta:
        db_table = 'review'

    def __str__(self):
        return f"Review Reserva #{self.reserva.id} - Nota: {self.calificacion}"
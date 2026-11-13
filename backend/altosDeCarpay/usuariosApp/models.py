from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.core.validators import RegexValidator
from django.db import models

from .validators import normalizar_rut, validar_rut


class UsuarioManager(BaseUserManager):
    use_in_migrations = True

    def _crear(self, correo, password, **extra):
        if not correo:
            raise ValueError("El correo electrónico es obligatorio.")
        usuario = self.model(correo=self.normalize_email(correo).lower(), **extra)
        usuario.set_password(password)
        usuario.save(using=self._db)
        return usuario

    def create_user(self, correo, password=None, **extra):
        extra.setdefault("rol", Usuario.Rol.CLIENTE)
        return self._crear(correo, password, **extra)

    def create_superuser(self, correo, password=None, **extra):
        extra.setdefault("rol", Usuario.Rol.ADMIN)
        extra["is_superuser"] = True
        return self._crear(correo, password, **extra)


class Usuario(AbstractBaseUser, PermissionsMixin):
    class Rol(models.TextChoices):
        CLIENTE = "Cliente", "Cliente"
        ADMIN = "Admin", "Administrador"

    class Estado(models.TextChoices):
        ACTIVO = "Activo", "Activo"
        INACTIVO = "Inactivo", "Inactivo"

    id_usuario = models.AutoField(primary_key=True)
    correo = models.EmailField(max_length=100, unique=True)
    rut = models.CharField(max_length=12, unique=True, validators=[validar_rut])
    nombre = models.CharField(max_length=50)
    apellido_paterno = models.CharField(max_length=50)
    apellido_materno = models.CharField(max_length=50)
    telefono = models.CharField(
        max_length=20, blank=True, default="",
        validators=[RegexValidator(r"^\+?\d{8,15}$", "Ingrese un teléfono válido (ej.: +56912345678).")],
    )
    password = models.CharField("contraseña", max_length=255)
    rol = models.CharField(max_length=20, choices=Rol.choices, default=Rol.CLIENTE)
    estado = models.CharField(max_length=20, choices=Estado.choices, default=Estado.ACTIVO)
    ultima_conexion = models.DateTimeField(null=True, blank=True)
    # Evidencia del consentimiento informado exigido por la Ley N° 21.719.
    consentimiento_datos = models.DateTimeField(null=True, blank=True)

    last_login = None  # Se reemplaza por `ultima_conexion`.

    USERNAME_FIELD = "correo"
    REQUIRED_FIELDS = ["rut", "nombre", "apellido_paterno", "apellido_materno"]

    objects = UsuarioManager()

    class Meta:
        db_table = "usuario"
        ordering = ["id_usuario"]

    def __str__(self):
        return f"{self.nombre} {self.apellido_paterno} ({self.correo})"

    def save(self, *args, **kwargs):
        self.rut = normalizar_rut(self.rut)
        self.correo = self.correo.lower()
        super().save(*args, **kwargs)

    @property
    def is_active(self):
        return self.estado == self.Estado.ACTIVO

    @property
    def is_staff(self):
        return self.es_admin

    @property
    def es_admin(self):
        return self.is_active and self.rol == self.Rol.ADMIN

    @property
    def es_cliente(self):
        return self.is_active and self.rol == self.Rol.CLIENTE

    @property
    def nombre_completo(self):
        return f"{self.nombre} {self.apellido_paterno} {self.apellido_materno}".strip()

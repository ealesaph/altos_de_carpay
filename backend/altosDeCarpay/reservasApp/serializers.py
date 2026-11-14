from django.core.exceptions import ValidationError as DjangoValidationError
from django.core.validators import URLValidator
from django.utils import timezone
from rest_framework import serializers
from altosDeCarpayApp.texto import texto_obligatorio, limpiar_texto
from usuariosApp.models import Usuario
from . import services
from .models import Pago, Reserva, Review, Sector, Servicio

class SectorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Sector
        fields = ["id_sector", "nombre", "descripcion", "fotografias", "valor_hora", "estado"]
        read_only_fields = ["id_sector"]

    def validate_nombre(self, valor):
        return texto_obligatorio(valor, "El nombre")

    def validate_descripcion(self, valor):
        return limpiar_texto(valor)

    def validate_fotografias(self, urls):
        if not isinstance(urls, list) or len(urls) > 10:
            raise serializers.ValidationError("Envíe una lista de hasta 10 URL.")
        validador = URLValidator(schemes=["http", "https"])
        for url in urls:
            try:
                validador(url)
            except (DjangoValidationError, TypeError):
                raise serializers.ValidationError(f"URL no válida: {url}")
        return urls

class ServicioSerializer(serializers.ModelSerializer):
    class Meta:
        model = Servicio
        fields = ["id_servicio", "nombre", "descripcion", "valor", "estado"]
        read_only_fields = ["id_servicio"]

    def validate_nombre(self, valor):
        return texto_obligatorio(valor, "El nombre")

    def validate_descripcion(self, valor):
        return limpiar_texto(valor)

class SectorEntradaSerializer(serializers.Serializer):
    sector = serializers.PrimaryKeyRelatedField(queryset=Sector.objects.all())
    horas = serializers.IntegerField(min_value=1, max_value=24)

class ServicioEntradaSerializer(serializers.Serializer):
    servicio = serializers.PrimaryKeyRelatedField(queryset=Servicio.objects.filter(estado=Servicio.Estado.ACTIVO))
    cantidad = serializers.IntegerField(min_value=1, max_value=500)

class ReservaCrearSerializer(serializers.Serializer):
    fecha = serializers.DateField()
    hora_inicio = serializers.TimeField(input_formats=["%H:%M", "%H:%M:%S"])
    sectores = SectorEntradaSerializer(many=True, allow_empty=False)
    servicios = ServicioEntradaSerializer(many=True, required=False)
    acepta_condiciones = serializers.BooleanField()
    cliente = serializers.PrimaryKeyRelatedField(
        queryset=Usuario.objects.filter(rol=Usuario.Rol.CLIENTE, estado=Usuario.Estado.ACTIVO),
        required=False,
    )

    def validate_acepta_condiciones(self, valor):
        if valor is not True:
            raise serializers.ValidationError("Debe aceptar las condiciones de arriendo y la política de cancelación.")
        return valor

    def validate_fecha(self, valor):
        if valor < timezone.localdate():
            raise serializers.ValidationError("La fecha no puede estar en el pasado.")
        return valor

    def validate_sectores(self, items):
        ids = [i["sector"].pk for i in items]
        if len(ids) != len(set(ids)):
            raise serializers.ValidationError("No repita sectores en la misma reserva.")
        return items

    def validate_servicios(self, items):
        ids = [i["servicio"].pk for i in items]
        if len(ids) != len(set(ids)):
            raise serializers.ValidationError("No repita servicios en la misma reserva.")
        return items

    def validate(self, datos):
        solicitante = self.context["request"].user
        if solicitante.es_admin:
            if "cliente" not in datos:
                raise serializers.ValidationError({"cliente": "Indique el cliente al que pertenece la reserva."})
            datos["titular"] = datos.pop("cliente")
        else:
            if "cliente" in datos:
                raise serializers.ValidationError({"cliente": "No puede reservar a nombre de otro usuario."})
            datos["titular"] = solicitante
        return datos

    def create(self, datos):
        return services.crear_reserva(
            usuario=datos["titular"], fecha=datos["fecha"], hora_inicio=datos["hora_inicio"],
            sectores=datos["sectores"], servicios=datos.get("servicios", []),
        )

class PagoSerializer(serializers.ModelSerializer):
    class Meta:
        model = Pago
        fields = ["id_pago", "monto", "tipo_pago", "metodo_pago", "transaccion_id", "estado_pago", "fecha_pago"]
        read_only_fields = fields

class PagoCrearSerializer(serializers.Serializer):
    tipo_pago = serializers.ChoiceField(choices=Pago.Tipo.choices)
    metodo_pago = serializers.ChoiceField(choices=Pago.Metodo.choices, default=Pago.Metodo.WEBPAY)
    simular_rechazo = serializers.BooleanField(default=False)

    def validate_simular_rechazo(self, valor):
        from django.conf import settings
        if valor and not settings.PAGOS_SIMULADOR_PERMITIR_RECHAZO:
            raise serializers.ValidationError("El rechazo simulado solo está disponible en desarrollo.")
        return valor

class SectorReservadoSerializer(serializers.Serializer):
    id_sector = serializers.IntegerField(source="sector_id")
    nombre = serializers.CharField(source="sector.nombre")
    horas = serializers.IntegerField()
    valor_hora = serializers.DecimalField(max_digits=10, decimal_places=2)
    subtotal = serializers.DecimalField(max_digits=12, decimal_places=2)

class ServicioReservadoSerializer(serializers.Serializer):
    id_servicio = serializers.IntegerField(source="servicio_id")
    nombre = serializers.CharField(source="servicio.nombre")
    cantidad = serializers.IntegerField()
    valor_unitario = serializers.DecimalField(max_digits=10, decimal_places=2)
    subtotal = serializers.DecimalField(max_digits=12, decimal_places=2)

class ReservaDetalleSerializer(serializers.ModelSerializer):
    cliente = serializers.CharField(source="usuario.nombre_completo", read_only=True)
    correo_cliente = serializers.EmailField(source="usuario.correo", read_only=True)
    hora_fin = serializers.SerializerMethodField()
    monto_anticipo = serializers.DecimalField(max_digits=10, decimal_places=2, read_only=True)
    monto_pagado = serializers.DecimalField(max_digits=12, decimal_places=2, read_only=True)
    saldo_pendiente = serializers.DecimalField(max_digits=12, decimal_places=2, read_only=True)
    plazo_anticipo_vence = serializers.DateTimeField(read_only=True)
    sectores = SectorReservadoSerializer(source="sectores_reservados", many=True, read_only=True)
    servicios = ServicioReservadoSerializer(source="servicios_reservados", many=True, read_only=True)
    pagos = PagoSerializer(many=True, read_only=True)

    class Meta:
        model = Reserva
        fields = ["id_reserva", "usuario", "cliente", "correo_cliente", "fecha", "hora_inicio", "hora_fin",
                  "horas_reservadas", "estado", "monto_total", "monto_anticipo", "monto_pagado",
                  "saldo_pendiente", "plazo_anticipo_vence", "fecha_creacion", "token_qr",
                  "sectores", "servicios", "pagos"]
        read_only_fields = fields

    def get_hora_fin(self, reserva):
        return timezone.localtime(reserva.fin).strftime("%H:%M") if reserva.fin else None

class CambioEstadoSerializer(serializers.Serializer):
    estado = serializers.ChoiceField(choices=Reserva.Estado.choices)

class ReviewSerializer(serializers.ModelSerializer):

    class Meta:
        model = Review
        fields = ["id_review", "reserva", "evaluador", "evaluado", "calificacion", "comentario", "fecha"]
        read_only_fields = ["id_review", "evaluador", "fecha"]

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        peticion = self.context.get("request")
        if peticion is not None and not peticion.user.es_admin:
            self.fields["reserva"].queryset = Reserva.objects.filter(usuario=peticion.user)

    def validate_comentario(self, valor):
        return limpiar_texto(valor)

    def validate(self, datos):
        usuario = self.context["request"].user
        reserva, evaluado = datos["reserva"], datos["evaluado"]
        if usuario.es_admin:
            if evaluado.pk != reserva.usuario_id:
                raise serializers.ValidationError({"evaluado": "El administrador solo puede evaluar al cliente titular."})
        elif not evaluado.es_admin:
            raise serializers.ValidationError({"evaluado": "Solo puede evaluar a un administrador del recinto."})
        if reserva.estado not in (Reserva.Estado.CONFIRMADO, Reserva.Estado.EN_PROCESO):
            raise serializers.ValidationError("Solo se pueden evaluar reservas confirmadas.")
        if reserva.inicio is None:
            raise serializers.ValidationError("La reserva no tiene una hora de inicio registrada.")
        if reserva.inicio > timezone.now():
            raise serializers.ValidationError("La reseña puede emitirse una vez iniciado el evento.")
        if Review.objects.filter(reserva=reserva, evaluador=usuario).exists():
            raise serializers.ValidationError("Ya emitió una reseña para esta reserva.")
        return datos

    def create(self, datos):
        return Review.objects.create(evaluador=self.context["request"].user, **datos)

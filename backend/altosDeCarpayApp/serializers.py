from rest_framework import serializers
from .models import ModeloUsuario,ModeloSector, ModeloServicio, ModeloReserva, ModeloReservaSector, ModeloReservaServcio, ModeloLogsModificacion, ModeloPago, ModeloValidacionAcceso, ModeloReview

class UsuarioSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloUsuario
        fields = ['id', 'rut', 'nombre', 'apellido_paterno', 'apellido_materno', 'correo', 'rol', 'estado']
        extra_kwargs = {'password': {'write_only': True}}

class SectorSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloSector
        fields = '__all__'

class ServicioSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloServicio
        fields = '__all__'

class ReservaSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloReserva
        fields = '__all__'

class ReservaSectorSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloReservaSector
        fields = '__all__'

class ReservaServicioSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloReservaServcio
        fields = '__all__'

class LogsModificacionSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloLogsModificacion
        fields = '__all__'

class PagoSerialize(serializers.ModelSerializer):
    class Meta:
        model = ModeloPago
        fields = '__all__'

class ValidacionAccesoSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloValidacionAcceso
        fields = '__all__'

class ReviewSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModeloReview
        fields = '__all__'

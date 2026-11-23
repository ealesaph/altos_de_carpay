import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'altosDeCarpay.settings')
django.setup()

from usuariosApp.models import Usuario
from reservasApp.models import Sector, Servicio
from decimal import Decimal
from django.utils import timezone
from usuariosApp.privacidad import POLITICA_VERSION

def seed():
    print("--- Verificando datos semilla ---")
    
    # 1. Usuarios
    if not Usuario.objects.filter(correo='admin@altosdecarpay.cl').exists():
        admin = Usuario.objects.create_superuser(
            correo='admin@altosdecarpay.cl',
            password='CarpayAdmin2026!',
            rut='11.111.111-1',
            nombre='Lucas',
            apellido_paterno='Ziebold',
            apellido_materno='Máximo',
            telefono='+56912345678'
        )
        print("✓ Usuario Admin creado: admin@altosdecarpay.cl (Clave: CarpayAdmin2026!)")
    else:
        print("✓ Usuario Admin ya existe.")

    if not Usuario.objects.filter(correo='camila.rojas@gmail.com').exists():
        cliente = Usuario.objects.create_user(
            correo='camila.rojas@gmail.com',
            password='ClientePass2026!',
            rut='22.222.222-2',
            nombre='Camila',
            apellido_paterno='Rojas',
            apellido_materno='Pérez',
            telefono='+56987654321',
            consentimiento_datos=timezone.now(),
            consentimiento_version=POLITICA_VERSION,
        )
        print("✓ Usuario Cliente creado: camila.rojas@gmail.com (Clave: ClientePass2026!)")
    else:
        print("✓ Usuario Cliente ya existe.")

    # 2. Sectores
    sectores_data = [
        ("Quincho Los Aromos", "Espacio campestre techado con parrilla, mesas y vista al valle.", Decimal("35000.00")),
        ("Terraza superior", "Terraza al aire libre con vista panorámica, ideal para recepciones.", Decimal("45000.00")),
        ("Mirador", "Área elevada rodeada de naturaleza con vista al atardecer.", Decimal("25000.00")),
        ("Piscina", "Piscina recreativa con reposeras y áreas verdes.", Decimal("30000.00")),
        ("Terraza inferior", "Zona íntima al lado de los jardines.", Decimal("20000.00")),
    ]
    for nombre, desc, valor in sectores_data:
        s, created = Sector.objects.get_or_create(
            nombre=nombre,
            defaults={'descripcion': desc, 'valor_hora': valor, 'estado': Sector.Estado.DISPONIBLE}
        )
        if created:
            print(f"✓ Sector creado: {nombre}")

    # 3. Servicios
    servicios_data = [
        ("Uso de Piscina", "Acceso exclusivo para el grupo a la piscina durante el evento", Decimal("30000.00")),
        ("Extensión Horaria", "Hora extra posterior al término programado", Decimal("35000.00")),
        ("Bebida 1.5 L", "Bebida desechable surtida", Decimal("2500.00")),
        ("Bolsa de hielo 2 kg", "Bolsa de hielo en cubos", Decimal("2000.00")),
        ("Agua mineral 1.5 L", "Agua mineral sin gas", Decimal("1500.00")),
        ("Carbón 3 kg", "Bolsa de carbón vegetal para parrilla", Decimal("4500.00")),
    ]
    for nombre, desc, valor in servicios_data:
        srv, created = Servicio.objects.get_or_create(
            nombre=nombre,
            defaults={'descripcion': desc, 'valor': valor, 'estado': Servicio.Estado.ACTIVO}
        )
        if created:
            print(f"✓ Servicio creado: {nombre}")

    print("--- Datos semilla listos ---")

if __name__ == '__main__':
    seed()

# Cifrado en reposo del RUT y del teléfono (Ley N° 21.719 · Tríada CID).
import django.core.validators
from django.db import migrations, models

import usuariosApp.cifrado
import usuariosApp.validators


class Migration(migrations.Migration):

    dependencies = [
        ('usuariosApp', '0002_usuario_consentimiento_version'),
    ]

    operations = [
        migrations.AlterField(
            model_name='usuario',
            name='rut',
            field=usuariosApp.cifrado.CampoCifrado(max_length=255, validators=[usuariosApp.validators.validar_rut]),
        ),
        migrations.AlterField(
            model_name='usuario',
            name='telefono',
            field=usuariosApp.cifrado.CampoCifrado(blank=True, default='', max_length=255, validators=[django.core.validators.RegexValidator('^\\+?\\d{8,15}$', 'Ingrese un teléfono válido (ej.: +56912345678).')]),
        ),
        migrations.AddField(
            model_name='usuario',
            name='rut_hash',
            field=models.CharField(editable=False, max_length=64, null=True, unique=True),
        ),
    ]

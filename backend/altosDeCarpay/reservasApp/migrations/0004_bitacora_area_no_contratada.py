# HU-05: la incidencia registra el área afectada y marca si no estaba contratada.
import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('reservasApp', '0003_alter_reserva_estado_bitacoraincidencia_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='bitacoraincidencia',
            name='area_no_contratada',
            field=models.BooleanField(default=False),
        ),
        migrations.AddField(
            model_name='bitacoraincidencia',
            name='sector',
            field=models.ForeignKey(blank=True, db_column='id_sector', null=True, on_delete=django.db.models.deletion.PROTECT, related_name='incidencias', to='reservasApp.sector'),
        ),
    ]

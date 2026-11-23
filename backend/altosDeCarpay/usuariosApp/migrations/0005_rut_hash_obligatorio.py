from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('usuariosApp', '0004_cifrar_registros_existentes'),
    ]

    operations = [
        migrations.AlterField(
            model_name='usuario',
            name='rut_hash',
            field=models.CharField(editable=False, max_length=64, unique=True),
        ),
    ]

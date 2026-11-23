# Cifra los RUT y teléfonos ya registrados y calcula su huella (migración de datos).
from django.db import migrations


def cifrar_existentes(apps, schema_editor):
    from usuariosApp.cifrado import huella_rut
    from usuariosApp.validators import normalizar_rut

    Usuario = apps.get_model('usuariosApp', 'Usuario')
    for usuario in Usuario.objects.all():
        # Al leer, el campo devuelve el texto claro heredado; al guardar, lo cifra.
        usuario.rut = normalizar_rut(usuario.rut)
        usuario.rut_hash = huella_rut(usuario.rut)
        usuario.save(update_fields=['rut', 'telefono', 'rut_hash'])


def descifrar_existentes(apps, schema_editor):
    Usuario = apps.get_model('usuariosApp', 'Usuario')
    with schema_editor.connection.cursor() as cursor:
        for usuario in Usuario.objects.all():
            cursor.execute(
                'UPDATE usuario SET rut = %s, telefono = %s WHERE id_usuario = %s',
                [usuario.rut, usuario.telefono or '', usuario.pk],
            )


class Migration(migrations.Migration):

    dependencies = [
        ('usuariosApp', '0003_cifrado_datos_personales'),
    ]

    operations = [
        migrations.RunPython(cifrar_existentes, descifrar_existentes),
    ]

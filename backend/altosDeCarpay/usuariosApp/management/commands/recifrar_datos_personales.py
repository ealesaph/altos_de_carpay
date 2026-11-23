"""Vuelve a cifrar el RUT y el teléfono con la clave vigente (rotación de FIELD_ENCRYPTION_KEY)."""
from django.core.management.base import BaseCommand
from django.db import transaction

from usuariosApp.models import Usuario


class Command(BaseCommand):
    help = "Cifra nuevamente los datos personales con FIELD_ENCRYPTION_KEY y recalcula la huella del RUT."

    def handle(self, *args, **opciones):
        total = 0
        with transaction.atomic():
            for usuario in Usuario.objects.select_for_update().all():
                usuario.save(update_fields=["rut", "telefono"])  # save() recalcula rut_hash
                total += 1
        self.stdout.write(self.style.SUCCESS(f"{total} usuario(s) cifrados con la clave vigente."))

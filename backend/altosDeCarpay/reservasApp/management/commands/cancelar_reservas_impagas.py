from django.core.management.base import BaseCommand

from reservasApp.services import cancelar_reservas_impagas


class Command(BaseCommand):
    help = "Cancela las reservas pendientes que no pagaron el anticipo dentro del plazo (ej.: cada hora)."

    def handle(self, *args, **options):
        total = cancelar_reservas_impagas()
        self.stdout.write(self.style.SUCCESS(f"Reservas canceladas por impago: {total}"))

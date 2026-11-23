from django.core.management.base import BaseCommand

from reservasApp.services import cancelar_reservas_impagas, cerrar_reservas_vencidas


class Command(BaseCommand):
    help = ("Tareas periódicas del recinto (el contenedor las ejecuta cada 15 minutos): libera las reservas "
            "sin anticipo pagado a tiempo y cierra las reservas cuyo horario ya terminó.")

    def handle(self, *args, **options):
        canceladas = cancelar_reservas_impagas()
        no_show, finalizadas = cerrar_reservas_vencidas()
        self.stdout.write(self.style.SUCCESS(
            f"Reservas canceladas por impago: {canceladas} · marcadas «No se presentó»: {no_show} · "
            f"finalizadas automáticamente: {finalizadas}"
        ))

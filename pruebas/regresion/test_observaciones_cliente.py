"""Pruebas de las correcciones acordadas con el cliente en la revisión del prototipo (23-11-2026).

Cada prueba indica el número de observación de la bitácora y el hallazgo del informe de pruebas web.
"""
from datetime import datetime, time, timedelta
from decimal import Decimal

from django.core import mail
from django.core.cache import cache
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework.test import APIClient

from consultasApp.models import Consulta
from usuariosApp.models import Usuario
from reservasApp.models import Pago, Reserva, ReservaSector, Sector
from reservasApp.services import cerrar_reservas_vencidas


@override_settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend")
class ObservacionesClienteTests(TestCase):
    def setUp(self):
        cache.clear()
        self.api = APIClient()
        self.admin = Usuario.objects.create_user(
            correo="admin@altosdecarpay.cl", password="ClaveSegura2026!", rut="22222222-2",
            nombre="Joaquín", apellido_paterno="Domínguez", apellido_materno="Ratto", rol=Usuario.Rol.ADMIN,
        )
        self.cliente = Usuario.objects.create_user(
            correo="cliente@example.com", password="ClaveSegura2026!", rut="11111111-1",
            nombre="Camila", apellido_paterno="Rojas", apellido_materno="Díaz",
        )
        self.jardin = Sector.objects.create(nombre="Jardín", valor_hora=Decimal("20000.00"))
        self.quincho = Sector.objects.create(nombre="Quincho", valor_hora=Decimal("10000.00"))
        self.fecha = timezone.localdate() + timedelta(days=10)

    def _post_reserva(self, hora="13:00", horas=5, sector=None, fecha=None):
        self.api.force_authenticate(user=self.cliente)
        return self.api.post("/api/v1/reservas/", {
            "fecha": (fecha or self.fecha).isoformat(), "hora_inicio": hora, "acepta_condiciones": True,
            "sectores": [{"sector": (sector or self.jardin).pk, "horas": horas}],
        }, format="json")

    def _reserva(self, *, inicio, horas=4, estado=Reserva.Estado.CONFIRMADO, pagado=Decimal("0")):
        inicio = timezone.localtime(inicio)
        reserva = Reserva.objects.create(
            usuario=self.cliente, fecha=inicio.date(), hora_inicio=inicio.time().replace(second=0, microsecond=0),
            horas_reservadas=horas, monto_total=Decimal("100000.00"), estado=estado,
        )
        ReservaSector.objects.create(reserva=reserva, sector=self.jardin, horas=horas, valor_hora=Decimal("25000.00"))
        if pagado:
            Pago.objects.create(reserva=reserva, monto=pagado, tipo_pago=Pago.Tipo.ANTICIPO,
                                metodo_pago=Pago.Metodo.TRANSFERENCIA, estado_pago=Pago.Estado.APROBADO)
        return reserva

    # Observaciones 4 y 5 (B1, B4): horas enteras y solo los bloques 13–18 y 19–00.
    def test_obs5_solo_se_reserva_dentro_de_los_bloques(self):
        self.assertEqual(self._post_reserva("13:00", 5).status_code, 201)
        self.assertEqual(self._post_reserva("19:00", 5, sector=self.quincho).status_code, 201)
        self.assertEqual(self._post_reserva("22:00", 6).status_code, 400)  # 22:00–04:00 pasa la medianoche
        self.assertEqual(self._post_reserva("16:00", 3).status_code, 400)  # 16:00–19:00 cruza los bloques
        self.assertEqual(self._post_reserva("10:00", 2).status_code, 400)  # antes de abrir
        self.assertEqual(self._post_reserva("13:30", 2).status_code, 400)  # solo horas en punto

    # Observación 6 (M5): «Editar selección» cambia la misma reserva y no se bloquea a sí misma.
    def test_obs6_editar_seleccion_modifica_la_misma_reserva(self):
        reserva_id = self._post_reserva("13:00", 5).data["id_reserva"]
        r = self.api.post(f"/api/v1/reservas/{reserva_id}/modificar/", {
            "fecha": self.fecha.isoformat(), "hora_inicio": "14:00", "acepta_condiciones": True,
            "sectores": [{"sector": self.jardin.pk, "horas": 3}],
        }, format="json")
        self.assertEqual(r.status_code, 200, r.data)
        self.assertEqual(Reserva.objects.count(), 1)
        reserva = Reserva.objects.get(pk=reserva_id)
        self.assertEqual((reserva.hora_inicio, reserva.horas_reservadas, reserva.monto_total),
                         (time(14, 0), 3, Decimal("60000.00")))

    # Observación 8 (B3): el plazo del saldo es 24 horas antes del inicio.
    def test_obs8_plazo_del_saldo_24_horas_antes(self):
        r = self._post_reserva("19:00", 3)
        inicio = timezone.make_aware(datetime.combine(self.fecha, time(19, 0)))
        vence = datetime.fromisoformat(r.data["plazo_saldo_vence"])
        self.assertEqual(vence, inicio - timedelta(hours=24))

    # Observaciones 9 (M8) y 2 (M9): consulta sin cuenta y respuesta por correo y en «Mis consultas».
    def test_obs2_consulta_sin_cuenta_y_respuesta_por_correo(self):
        anonimo = APIClient()
        sin_datos = anonimo.post("/api/v1/consultas/", {"asunto": "Fecha", "mensaje": "¿Está libre?"}, format="json")
        self.assertEqual(sin_datos.status_code, 400)
        r = anonimo.post("/api/v1/consultas/", {
            "asunto": "Disponibilidad", "mensaje": "¿Está libre el 20?", "nombre_contacto": "Pedro Soto",
            "correo_contacto": "pedro@example.com", "telefono_contacto": "+56 9 1234 5678",
            "acepta_privacidad": True,
        }, format="json")
        self.assertEqual(r.status_code, 201, r.data)
        consulta = Consulta.objects.get(pk=r.data["id_consulta"])
        self.assertIsNone(consulta.cliente)
        self.api.force_authenticate(user=self.admin)
        self.api.post(f"/api/v1/consultas/{consulta.pk}/responder/", {"respuesta": "Sí, está disponible."},
                      format="json")
        self.assertEqual(len(mail.outbox), 1)
        self.assertEqual(mail.outbox[0].to, ["pedro@example.com"])

    def test_obs9_cliente_ve_sus_consultas_respondidas(self):
        self.api.force_authenticate(user=self.cliente)
        consulta_id = self.api.post("/api/v1/consultas/", {"asunto": "Parrilla", "mensaje": "¿Incluye carbón?"},
                                    format="json").data["id_consulta"]
        self.api.force_authenticate(user=self.admin)
        self.api.post(f"/api/v1/consultas/{consulta_id}/responder/", {"respuesta": "No, se cobra aparte."},
                      format="json")
        self.api.force_authenticate(user=self.cliente)
        lista = self.api.get("/api/v1/consultas/").data["results"]
        self.assertEqual(lista[0]["respuesta"], "No, se cobra aparte.")
        self.assertEqual(mail.outbox[0].to, ["cliente@example.com"])

    # Observaciones 11 y 12 (M1): métricas solo de reservas vigentes, próximas en orden ascendente.
    def test_obs11_saldo_por_cobrar_excluye_canceladas_y_suma_cobrado_del_mes(self):
        manana = timezone.now() + timedelta(days=2)
        self._reserva(inicio=manana, pagado=Decimal("30000"))
        self._reserva(inicio=manana + timedelta(days=1), estado=Reserva.Estado.CANCELADO)
        self.api.force_authenticate(user=self.admin)
        resumen = self.api.get("/api/v1/reservas/resumen/").data
        self.assertEqual(Decimal(resumen["saldo_por_cobrar"]), Decimal("70000.00"))
        self.assertEqual(Decimal(resumen["cobrado_mes"]), Decimal("30000.00"))

    def test_obs12_proximas_reservas_en_orden_ascendente(self):
        lejana = self._reserva(inicio=timezone.now() + timedelta(days=20))
        cercana = self._reserva(inicio=timezone.now() + timedelta(days=3))
        self._reserva(inicio=timezone.now() + timedelta(days=5), estado=Reserva.Estado.CANCELADO)
        self.api.force_authenticate(user=self.admin)
        ids = [r["id_reserva"] for r in self.api.get("/api/v1/reservas/?vista=proximas").data["results"]]
        self.assertEqual(ids, [cercana.pk, lejana.pk])

    # Observación 13 (M2): cierre automático y estado «No se presentó».
    def test_obs13_reservas_vencidas_se_cierran_solas(self):
        hace_un_rato = timezone.now() - timedelta(hours=8)
        sin_llegar = self._reserva(inicio=hace_un_rato, horas=2)
        en_curso = self._reserva(inicio=hace_un_rato, horas=2, estado=Reserva.Estado.EN_PROCESO)
        self.assertEqual(cerrar_reservas_vencidas(), (1, 1))
        sin_llegar.refresh_from_db()
        en_curso.refresh_from_db()
        self.assertEqual(sin_llegar.estado, Reserva.Estado.NO_SE_PRESENTO)
        self.assertEqual(en_curso.estado, Reserva.Estado.FINALIZADO)

    # Observación 15 (A5, M6): devolución según quién cancela y con cuánta anticipación.
    def test_obs15_devolucion_segun_condiciones_de_arriendo(self):
        self.api.force_authenticate(user=self.admin)
        lejana = self._reserva(inicio=timezone.now() + timedelta(days=20), pagado=Decimal("100000"))
        Reserva.objects.filter(pk=lejana.pk).update(monto_total=Decimal("100000.00"))
        r = self.api.post(f"/api/v1/reservas/{lejana.pk}/cancelar/", {"origen": "Cliente"}, format="json")
        self.assertEqual(Decimal(r.data["monto_devolucion"]), Decimal("70000.00"))  # todo menos el anticipo (30%)

        cercana = self._reserva(inicio=timezone.now() + timedelta(days=3), pagado=Decimal("100000"))
        simulacion = self.api.get(f"/api/v1/reservas/{cercana.pk}/simular-cancelacion/?origen=Cliente").data
        self.assertEqual(Decimal(simulacion["monto_devolucion"]), Decimal("0"))
        self.assertEqual(Reserva.objects.get(pk=cercana.pk).estado, Reserva.Estado.CONFIRMADO)  # solo simula

        r = self.api.post(f"/api/v1/reservas/{cercana.pk}/cancelar/", {"origen": "Recinto"}, format="json")
        self.assertEqual(Decimal(r.data["monto_devolucion"]), Decimal("100000.00"))
        self.assertEqual(r.data["cancelada_por"], "Recinto")

    # Observación 16 (A3): la entrada solo se registra el día del evento, desde 1 hora antes.
    def test_obs16_check_in_solo_el_dia_del_evento(self):
        self.api.force_authenticate(user=self.admin)
        futura = self._reserva(inicio=timezone.now() + timedelta(days=12))
        r = self.api.post(f"/api/v1/reservas/{futura.pk}/validar-acceso/", {"tipo_registro": "Entrada"}, format="json")
        self.assertEqual(r.status_code, 409)
        en_media_hora = self._reserva(inicio=timezone.now() + timedelta(minutes=30))
        r = self.api.post(f"/api/v1/reservas/{en_media_hora.pk}/validar-acceso/", {"tipo_registro": "Entrada"},
                          format="json")
        self.assertEqual(r.status_code, 201)


class BloqueoInicioSesionTests(TestCase):
    """Hallazgo A1: el bloqueo por intentos fallidos no se evita cambiando X-Forwarded-For."""

    def setUp(self):
        cache.clear()
        Usuario.objects.create_user(
            correo="cliente@example.com", password="ClaveSegura2026!", rut="11111111-1",
            nombre="Camila", apellido_paterno="Rojas", apellido_materno="Díaz",
        )

    def test_cuenta_bloqueada_tras_cinco_intentos_aunque_cambie_la_ip(self):
        api = APIClient()
        for intento in range(5):
            api.post("/api/v1/auth/login/", {"correo": "cliente@example.com", "password": "mala"},
                     format="json", HTTP_X_FORWARDED_FOR=f"10.0.0.{intento}")
        r = api.post("/api/v1/auth/login/", {"correo": "cliente@example.com", "password": "ClaveSegura2026!"},
                     format="json", HTTP_X_FORWARDED_FOR="10.0.0.99")
        self.assertEqual(r.status_code, 429)
        self.assertIn("bloqueada", str(r.data["detail"]))

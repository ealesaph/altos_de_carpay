from datetime import date, time, timedelta
from decimal import Decimal
from django.core import mail
from django.test import TestCase, override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from usuariosApp.models import Usuario
from reservasApp.emails import enviar_correo_reserva
from reservasApp.models import Pago, Reserva, ReservaSector, ReservaServicio, Sector, Servicio


@override_settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend")
class ReservaCorreoTests(TestCase):
    def setUp(self):
        self.cliente = Usuario.objects.create_user(
            correo="cliente@altosdecarpay.cl",
            password="PasswordSegura123!",
            rut="11111111-1",
            nombre="Camila",
            apellido_paterno="Rojas",
            apellido_materno="Diaz",
        )
        self.otro_cliente = Usuario.objects.create_user(
            correo="otro@altosdecarpay.cl",
            password="PasswordSegura123!",
            rut="22222222-2",
            nombre="Pedro",
            apellido_paterno="Soto",
            apellido_materno="Lopez",
        )
        self.sector = Sector.objects.create(nombre="Quincho Principal", valor_hora=Decimal("25000.00"))
        self.servicio = Servicio.objects.create(nombre="Vajilla", valor=Decimal("5000.00"))
        self.reserva = Reserva.objects.create(
            usuario=self.cliente,
            fecha=date.today() + timedelta(days=5),
            hora_inicio=time(14, 0),
            horas_reservadas=4,
            monto_total=Decimal("110000.00"),
            estado=Reserva.Estado.CONFIRMADO,
        )
        ReservaSector.objects.create(
            reserva=self.reserva,
            sector=self.sector,
            horas=4,
            valor_hora=self.sector.valor_hora,
        )
        ReservaServicio.objects.create(
            reserva=self.reserva,
            servicio=self.servicio,
            cantidad=2,
            valor_unitario=self.servicio.valor,
        )
        Pago.objects.create(
            reserva=self.reserva,
            monto=Decimal("33000.00"),
            tipo_pago=Pago.Tipo.ANTICIPO,
            metodo_pago=Pago.Metodo.WEBPAY,
            estado_pago=Pago.Estado.APROBADO,
        )

    def test_enviar_correo_reserva_incluye_texto_y_html(self):
        enviado = enviar_correo_reserva(self.reserva.pk, motivo="confirmacion", fail_silently=False)
        self.assertTrue(enviado)
        self.assertEqual(len(mail.outbox), 1)

        correo = mail.outbox[0]
        self.assertEqual(correo.to, [self.cliente.correo])
        self.assertIn(f"Reserva N° {self.reserva.pk}", correo.subject)
        self.assertIn("Quincho Principal", correo.body)
        self.assertIn("Vajilla", correo.body)
        self.assertIn(self.reserva.token_qr, correo.body)
        self.assertEqual(len(correo.alternatives), 1)
        html, mimetype = correo.alternatives[0]
        self.assertEqual(mimetype, "text/html")
        self.assertIn("Quincho Principal", html)
        self.assertIn("33.000", html)

    def test_endpoint_enviar_correo_solo_dueno_o_admin(self):
        api = APIClient()
        url = reverse("reserva-enviar-correo", kwargs={"pk": self.reserva.pk})

        api.force_authenticate(user=self.otro_cliente)
        resp_ajeno = api.post(url)
        self.assertEqual(resp_ajeno.status_code, status.HTTP_404_NOT_FOUND)

        api.force_authenticate(user=self.cliente)
        resp_dueno = api.post(url)
        self.assertEqual(resp_dueno.status_code, status.HTTP_200_OK)
        self.assertEqual(len(mail.outbox), 1)

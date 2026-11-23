"""Pruebas de trazabilidad: completan la cobertura del plan de pruebas sobre las historias de usuario.

Cada prueba indica la HU y el criterio de aceptación que verifica (informe «Plan de Pruebas»).
"""
from datetime import timedelta
from decimal import Decimal

from django.core.cache import cache
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework.test import APIClient

from usuariosApp.models import Usuario
from .models import Reserva, ReservaSector, Sector, Servicio


@override_settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend")
class TrazabilidadHistoriasUsuarioTests(TestCase):
    def setUp(self):
        cache.clear()
        self.api = APIClient()
        self.admin = Usuario.objects.create_user(
            correo="admin@altosdecarpay.cl", password="ClaveSegura2026!", rut="22222222-2",
            nombre="Lucas", apellido_paterno="Ziebold", apellido_materno="Maximo", rol=Usuario.Rol.ADMIN,
        )
        self.cliente = Usuario.objects.create_user(
            correo="cliente@example.com", password="ClaveSegura2026!", rut="11111111-1",
            nombre="=HYPERLINK(\"x\")", apellido_paterno="Rojas", apellido_materno="Diaz",
        )
        self.jardin = Sector.objects.create(nombre="Jardín", valor_hora=Decimal("20000.00"))
        self.balcon = Sector.objects.create(nombre="Balcón", valor_hora=Decimal("15000.00"),
                                            estado=Sector.Estado.MANTENCION)
        self.cocina = Sector.objects.create(nombre="Cocina", valor_hora=Decimal("10000.00"),
                                            estado=Sector.Estado.INACTIVO)
        Servicio.objects.create(nombre="Vajilla", valor=Decimal("5000.00"))
        self.fecha = (timezone.localdate() + timedelta(days=10)).isoformat()

    def _reservar(self, sector):
        self.api.force_authenticate(user=self.cliente)
        return self.api.post("/api/v1/reservas/", {
            "fecha": self.fecha, "hora_inicio": "14:00", "acepta_condiciones": True,
            "sectores": [{"sector": sector.pk, "horas": 4}],
        }, format="json")

    def test_hu01_admin_consulta_perfiles_y_cliente_no_tiene_acceso(self):
        """HU-01: el administrador consulta los perfiles; un cliente no accede a la gestión de usuarios."""
        self.api.force_authenticate(user=self.admin)
        lista = self.api.get("/api/v1/usuarios/")
        self.assertEqual(lista.status_code, 200)
        correos = [u["correo"] for u in lista.data["results"]]
        self.assertIn("cliente@example.com", correos)
        detalle = self.api.get(f"/api/v1/usuarios/{self.cliente.pk}/")
        self.assertEqual(detalle.status_code, 200)
        self.api.force_authenticate(user=self.cliente)
        self.assertEqual(self.api.get("/api/v1/usuarios/").status_code, 403)

    def test_hu02_reserva_queda_vinculada_al_usuario_responsable(self):
        """HU-02: el evento creado queda enlazado obligatoriamente al usuario responsable."""
        r = self._reservar(self.jardin)
        self.assertEqual(r.status_code, 201)
        reserva = Reserva.objects.get(pk=r.data["id_reserva"])
        self.assertEqual(reserva.usuario, self.cliente)

    def test_hu05_area_no_habilitada_es_rechazada_y_la_contratada_queda_registrada(self):
        """HU-05: solo se asignan áreas habilitadas; cada área contratada queda registrada con su tarifa."""
        rechazo = self._reservar(self.balcon)
        self.assertEqual(rechazo.status_code, 409)
        self.assertFalse(Reserva.objects.exists())
        ok = self._reservar(self.jardin)
        self.assertEqual(ok.status_code, 201)
        linea = ReservaSector.objects.get(reserva_id=ok.data["id_reserva"])
        self.assertEqual(linea.sector, self.jardin)
        self.assertEqual(linea.valor_hora, Decimal("20000.00"))

    def test_hu08_exportacion_compatible_con_excel(self):
        """HU-08: el administrador descarga un CSV para Excel, con columnas fijas y celdas seguras."""
        self._reservar(self.jardin)
        self.api.force_authenticate(user=self.admin)
        r = self.api.get("/api/v1/reservas/exportar/")
        self.assertEqual(r.status_code, 200)
        self.assertTrue(r["Content-Type"].startswith("text/csv"))
        self.assertIn("attachment", r["Content-Disposition"])
        contenido = r.content.decode("utf-8")
        self.assertTrue(contenido.startswith("﻿"))  # BOM: Excel reconoce los acentos
        filas = [f for f in contenido.lstrip("﻿").splitlines() if f]
        self.assertEqual(len(filas[0].split(";")), 10)
        self.assertEqual(len(filas[1].split(";")), 10)
        self.assertIn("'=HYPERLINK", filas[1])  # la fórmula queda neutralizada
        self.api.force_authenticate(user=self.cliente)
        self.assertEqual(self.api.get("/api/v1/reservas/exportar/").status_code, 403)

    def test_hu09_catalogo_publico_sin_autenticacion(self):
        """HU-09: un visitante sin sesión ve la oferta del recinto; las áreas inactivas no se publican."""
        anonimo = APIClient()
        sectores = anonimo.get("/api/v1/sectores/")
        self.assertEqual(sectores.status_code, 200)
        nombres = [s["nombre"] for s in sectores.data["results"]]
        self.assertIn("Jardín", nombres)
        self.assertNotIn("Cocina", nombres)
        self.assertEqual(anonimo.get("/api/v1/servicios/").status_code, 200)

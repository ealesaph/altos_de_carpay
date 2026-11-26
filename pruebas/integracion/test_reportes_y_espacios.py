"""Mejoras derivadas de la comparación de resultados: HU-05 (alerta de área) y HU-08 (Excel)."""
import io
from datetime import date, datetime, time, timedelta
from decimal import Decimal

from django.core.cache import cache
from django.test import TestCase, override_settings
from django.utils import timezone
from openpyxl import load_workbook
from rest_framework.test import APIClient

from usuariosApp.models import Usuario
from reservasApp.models import BitacoraIncidencia, Reserva, ReservaSector, Sector


@override_settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend")
class MejorasHU05HU08Tests(TestCase):
    def setUp(self):
        cache.clear()
        self.api = APIClient()
        self.admin = Usuario.objects.create_user(
            correo="admin@altosdecarpay.cl", password="ClaveSegura2026!", rut="22222222-2",
            nombre="Lucas", apellido_paterno="Ziebold", apellido_materno="Maximo", rol=Usuario.Rol.ADMIN,
        )
        self.cliente = Usuario.objects.create_user(
            correo="cliente@example.com", password="ClaveSegura2026!", rut="11111111-1",
            nombre="Camila", apellido_paterno="Rojas", apellido_materno="Diaz", telefono="+56987654321",
        )
        self.jardin = Sector.objects.create(nombre="Jardín", valor_hora=Decimal("20000.00"))
        self.piscina = Sector.objects.create(nombre="Piscina", valor_hora=Decimal("30000.00"))
        self.reserva = Reserva.objects.create(
            usuario=self.cliente, fecha=timezone.localdate() + timedelta(days=3), hora_inicio=time(14, 0),
            horas_reservadas=4, monto_total=Decimal("80000.00"), estado=Reserva.Estado.EN_PROCESO,
        )
        ReservaSector.objects.create(reserva=self.reserva, sector=self.jardin, horas=4,
                                     valor_hora=self.jardin.valor_hora)
        self.api.force_authenticate(user=self.admin)

    def _incidencia(self, sector):
        return self.api.post("/api/v1/incidencias/", {
            "reserva": self.reserva.pk, "titulo": "Uso de área", "descripcion": "Grupo en el área indicada.",
            "sector": sector.pk,
        }, format="json")

    def test_hu05_alerta_al_registrar_uso_de_area_no_contratada(self):
        r = self._incidencia(self.piscina)
        self.assertEqual(r.status_code, 201)
        self.assertTrue(r.data["area_no_contratada"])
        self.assertEqual(r.data["sector_nombre"], "Piscina")
        self.assertIn("Área no contratada", r.data["alerta"])

    def test_hu05_area_contratada_no_genera_alerta(self):
        r = self._incidencia(self.jardin)
        self.assertEqual(r.status_code, 201)
        self.assertFalse(r.data["area_no_contratada"])
        self.assertIsNone(r.data["alerta"])

    def test_hu08_reservas_en_excel_con_tipos_nativos(self):
        r = self.api.get("/api/v1/reservas/exportar/", {"formato": "xlsx"})
        self.assertEqual(r.status_code, 200)
        self.assertIn("spreadsheetml", r["Content-Type"])
        hoja = load_workbook(io.BytesIO(r.content)).active
        self.assertEqual(hoja["A1"].value, "N° reserva")
        self.assertIsInstance(hoja["D2"].value, (date, datetime))  # fecha como fecha
        self.assertIsInstance(hoja["H2"].value, (int, float, Decimal))  # monto como número
        self.assertEqual(hoja.freeze_panes, "A2")

    def test_hu08_reporte_de_usuarios_en_excel(self):
        r = self.api.get("/api/v1/usuarios/exportar/", {"formato": "xlsx"})
        self.assertEqual(r.status_code, 200)
        hoja = load_workbook(io.BytesIO(r.content)).active
        correos = [hoja.cell(row=f, column=5).value for f in range(2, hoja.max_row + 1)]
        self.assertIn("cliente@example.com", correos)
        self.assertEqual(hoja.cell(row=1, column=12).value, "N° de reservas")

    def test_hu08_reporte_de_incidencias_en_excel(self):
        self._incidencia(self.piscina)
        r = self.api.get("/api/v1/incidencias/exportar/", {"formato": "xlsx"})
        self.assertEqual(r.status_code, 200)
        hoja = load_workbook(io.BytesIO(r.content)).active
        self.assertEqual(hoja["G2"].value, "Piscina")
        self.assertEqual(hoja["H2"].value, "Sí")
        self.assertIsInstance(hoja["C2"].value, datetime)

    def test_hu08_reportes_solo_para_administradores(self):
        self.api.force_authenticate(user=self.cliente)
        for ruta in ("/api/v1/usuarios/exportar/", "/api/v1/incidencias/exportar/", "/api/v1/reservas/exportar/"):
            self.assertEqual(self.api.get(ruta, {"formato": "xlsx"}).status_code, 403)

    def test_admin_puede_agregar_y_eliminar_espacio_sin_reservas(self):
        creado = self.api.post("/api/v1/sectores/", {
            "nombre": "Quincho Nuevo",
            "descripcion": "Sector techado con parrilla",
            "valor_hora": "25000.00",
            "estado": "Disponible",
        }, format="json")
        self.assertEqual(creado.status_code, 201)
        id_sector = creado.data["id_sector"]
        self.assertTrue(Sector.objects.filter(pk=id_sector).exists())

        eliminado = self.api.delete(f"/api/v1/sectores/{id_sector}/")
        self.assertEqual(eliminado.status_code, 204)
        self.assertFalse(Sector.objects.filter(pk=id_sector).exists())

    def test_admin_no_puede_eliminar_espacio_con_reservas(self):
        r = self.api.delete(f"/api/v1/sectores/{self.jardin.pk}/")
        self.assertEqual(r.status_code, 409)
        self.assertTrue(Sector.objects.filter(pk=self.jardin.pk).exists())

    def test_cliente_no_puede_agregar_ni_eliminar_espacios(self):
        self.api.force_authenticate(user=self.cliente)
        r_post = self.api.post("/api/v1/sectores/", {"nombre": "Otro", "valor_hora": "10000"}, format="json")
        r_del = self.api.delete(f"/api/v1/sectores/{self.piscina.pk}/")
        self.assertEqual(r_post.status_code, 403)
        self.assertEqual(r_del.status_code, 403)

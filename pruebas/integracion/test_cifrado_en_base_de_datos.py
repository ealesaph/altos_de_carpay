"""Cifrado en reposo del RUT y del teléfono (Tríada CID · Ley N° 21.719)."""
from django.core.cache import cache
from django.db import connection
from django.test import TestCase
from rest_framework.test import APIClient

from usuariosApp.cifrado import huella_rut
from usuariosApp.models import Usuario


class CifradoDatosPersonalesTests(TestCase):
    def setUp(self):
        cache.clear()
        self.api = APIClient()
        self.usuario = Usuario.objects.create_user(
            correo="titular@example.com", password="ClaveSegura2026!", rut="12.345.678-5",
            nombre="Ana", apellido_paterno="Soto", apellido_materno="Pino", telefono="+56911112222",
        )

    def _fila_cruda(self):
        with connection.cursor() as cursor:
            cursor.execute("SELECT rut, telefono, rut_hash FROM usuario WHERE id_usuario = %s", [self.usuario.pk])
            return cursor.fetchone()

    def test_rut_y_telefono_no_quedan_en_texto_claro_en_la_base(self):
        rut, telefono, rut_hash = self._fila_cruda()
        self.assertNotIn("12345678", rut)
        self.assertNotIn("11112222", telefono)
        self.assertTrue(rut.startswith("gAAAA"))  # token Fernet
        self.assertEqual(rut_hash, huella_rut("123456785"))

    def test_la_aplicacion_lee_los_datos_descifrados(self):
        usuario = Usuario.objects.get(pk=self.usuario.pk)
        self.assertEqual(usuario.rut, "123456785")
        self.assertEqual(usuario.telefono, "+56911112222")

    def test_rut_duplicado_es_rechazado_aunque_cambie_el_formato(self):
        r = self.api.post("/api/v1/auth/registro/", {
            "correo": "otra@example.com", "rut": "12345678-5", "nombre": "Eva",
            "apellido_paterno": "Mora", "apellido_materno": "Vera", "password": "ClaveSegura2026!",
            "acepta_politica_datos": True,
        }, format="json")
        self.assertEqual(r.status_code, 400)
        self.assertIn("rut", r.data)

    def test_administrador_busca_por_rut_exacto(self):
        admin = Usuario.objects.create_user(
            correo="admin@example.com", password="ClaveSegura2026!", rut="22222222-2",
            nombre="Lucas", apellido_paterno="Ziebold", apellido_materno="Maximo", rol=Usuario.Rol.ADMIN,
        )
        self.api.force_authenticate(user=admin)
        r = self.api.get("/api/v1/usuarios/", {"q": "12.345.678-5"})
        self.assertEqual(r.status_code, 200)
        self.assertEqual([u["correo"] for u in r.data["results"]], ["titular@example.com"])

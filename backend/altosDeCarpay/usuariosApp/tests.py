from django.core.cache import cache
from django.test import TestCase
from rest_framework.test import APIClient

from auditoriaApp.models import BrechaSeguridad
from .models import Usuario
from .privacidad import POLITICA_VERSION


class Ley21719Tests(TestCase):
    """Cumplimiento de la Ley N° 21.719: información previa, consentimiento, ARCO y brechas."""

    def setUp(self):
        cache.clear()  # evita que el límite de intentos de otras pruebas afecte a estas
        self.api = APIClient()
        self.datos_registro = {
            "correo": "titular@example.com",
            "rut": "12.345.678-5",
            "nombre": "Ana",
            "apellido_paterno": "Soto",
            "apellido_materno": "Pino",
            "telefono": "+56911111111",
            "password": "ClaveSegura2026!",
        }

    def _usuario_sin_consentimiento(self):
        return Usuario.objects.create_user(
            correo="antiguo@example.com", password="ClaveSegura2026!", rut="11.111.111-1",
            nombre="Luis", apellido_paterno="Mora", apellido_materno="Vera",
        )

    def test_politica_es_publica_y_versionada(self):
        r = self.api.get("/api/v1/auth/politica-privacidad/")
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data["version"], POLITICA_VERSION)
        titulos = [s["titulo"] for s in r.data["secciones"]]
        self.assertIn("Sus derechos", titulos)
        self.assertIn("Finalidades", titulos)

    def test_registro_sin_aceptar_la_politica_es_rechazado(self):
        r = self.api.post("/api/v1/auth/registro/", {**self.datos_registro, "acepta_politica_datos": False},
                          format="json")
        self.assertEqual(r.status_code, 400)
        self.assertIn("acepta_politica_datos", r.data)
        self.assertFalse(Usuario.objects.filter(correo="titular@example.com").exists())

    def test_registro_guarda_fecha_y_version_del_consentimiento(self):
        r = self.api.post("/api/v1/auth/registro/", {**self.datos_registro, "acepta_politica_datos": True},
                          format="json")
        self.assertEqual(r.status_code, 201)
        usuario = Usuario.objects.get(correo="titular@example.com")
        self.assertIsNotNone(usuario.consentimiento_datos)
        self.assertEqual(usuario.consentimiento_version, POLITICA_VERSION)

    def test_login_avisa_consentimiento_pendiente_y_se_puede_otorgar(self):
        self._usuario_sin_consentimiento()
        r = self.api.post("/api/v1/auth/login/", {"correo": "antiguo@example.com",
                                                  "password": "ClaveSegura2026!"}, format="json")
        self.assertEqual(r.status_code, 200)
        self.assertTrue(r.data["usuario"]["requiere_consentimiento"])

        self.api.credentials(HTTP_AUTHORIZATION=f"Bearer {r.data['access']}")
        rechazo = self.api.post("/api/v1/auth/consentimiento/", {"acepta": False, "version": POLITICA_VERSION},
                                format="json")
        self.assertEqual(rechazo.status_code, 400)
        version_antigua = self.api.post("/api/v1/auth/consentimiento/", {"acepta": True, "version": "2020-01-01"},
                                        format="json")
        self.assertEqual(version_antigua.status_code, 400)

        ok = self.api.post("/api/v1/auth/consentimiento/", {"acepta": True, "version": POLITICA_VERSION},
                           format="json")
        self.assertEqual(ok.status_code, 200)
        estado = self.api.get("/api/v1/auth/consentimiento/")
        self.assertFalse(estado.data["requiere_consentimiento"])
        self.assertEqual(estado.data["consentimiento_version"], POLITICA_VERSION)

    def test_mis_datos_entrega_copia_portable(self):
        usuario = self._usuario_sin_consentimiento()
        self.api.force_authenticate(usuario)
        r = self.api.get("/api/v1/auth/mis-datos/")
        self.assertEqual(r.status_code, 200)
        for clave in ("perfil", "reservas", "consultas", "consentimiento_version", "fecha_emision"):
            self.assertIn(clave, r.data)
        self.assertEqual(r.data["perfil"]["correo"], "antiguo@example.com")

    def test_registro_de_brechas_no_se_puede_eliminar(self):
        brecha = BrechaSeguridad.objects.create(
            fecha_deteccion="2026-12-02T10:00:00-03:00", descripcion="Prueba",
            datos_afectados="correos", titulares_afectados=3,
        )
        with self.assertRaises(PermissionError):
            brecha.delete()

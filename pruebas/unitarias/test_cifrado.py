"""PRUEBAS UNITARIAS · Cifrado de datos personales (RUT y teléfono).

Prueban las funciones de usuariosApp/cifrado.py de forma aislada, con claves de prueba
generadas en el momento (no se usa la clave real del .env ni la base de datos).
"""
from cryptography.fernet import Fernet
from django.test import SimpleTestCase, override_settings

from usuariosApp.cifrado import cifrar, descifrar, huella, huella_rut

CLAVE_PRUEBA = Fernet.generate_key().decode()
CLAVE_NUEVA = Fernet.generate_key().decode()


@override_settings(FIELD_ENCRYPTION_KEY=CLAVE_PRUEBA, FIELD_ENCRYPTION_KEYS_ANTERIORES=())
class CifradoTests(SimpleTestCase):
    def test_cifrar_y_descifrar_devuelve_el_original(self):
        self.assertEqual(descifrar(cifrar("12345678-5")), "12345678-5")

    def test_el_texto_cifrado_no_contiene_el_dato(self):
        token = cifrar("+56911112222")
        self.assertNotIn("11112222", token)
        self.assertTrue(token.startswith("gAAAA"))  # formato de un token Fernet

    def test_cifrado_es_aleatorio(self):
        # El mismo dato cifrado dos veces da tokens distintos (no se puede comparar a simple vista).
        self.assertNotEqual(cifrar("123456785"), cifrar("123456785"))

    def test_dato_heredado_sin_cifrar_se_devuelve_tal_cual(self):
        self.assertEqual(descifrar("texto-plano-antiguo"), "texto-plano-antiguo")

    def test_huella_es_determinista_y_hexadecimal(self):
        h1, h2 = huella("123456785"), huella("123456785")
        self.assertEqual(h1, h2)
        self.assertEqual(len(h1), 64)
        int(h1, 16)  # es hexadecimal

    def test_huella_rut_no_depende_del_formato(self):
        self.assertEqual(huella_rut("12.345.678-5"), huella_rut("123456785"))
        self.assertNotEqual(huella_rut("12.345.678-5"), huella_rut("11.111.111-1"))

    def test_huella_rut_vacio(self):
        self.assertIsNone(huella_rut(None))
        self.assertIsNone(huella_rut(""))


class RotacionDeClaveTests(SimpleTestCase):
    def test_datos_cifrados_con_la_clave_anterior_se_siguen_leyendo(self):
        with override_settings(FIELD_ENCRYPTION_KEY=CLAVE_PRUEBA, FIELD_ENCRYPTION_KEYS_ANTERIORES=()):
            token_antiguo = cifrar("dato sensible")
        with override_settings(FIELD_ENCRYPTION_KEY=CLAVE_NUEVA, FIELD_ENCRYPTION_KEYS_ANTERIORES=(CLAVE_PRUEBA,)):
            self.assertEqual(descifrar(token_antiguo), "dato sensible")

    def test_sin_la_clave_correcta_no_se_descifra(self):
        with override_settings(FIELD_ENCRYPTION_KEY=CLAVE_PRUEBA, FIELD_ENCRYPTION_KEYS_ANTERIORES=()):
            token = cifrar("dato sensible")
        with override_settings(FIELD_ENCRYPTION_KEY=CLAVE_NUEVA, FIELD_ENCRYPTION_KEYS_ANTERIORES=()):
            # Con otra clave no se obtiene el dato: se devuelve el token sin descifrar.
            self.assertEqual(descifrar(token), token)

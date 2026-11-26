"""PRUEBAS UNITARIAS · Validación de RUT y limpieza de texto.

Prueban funciones aisladas, sin base de datos ni red (SimpleTestCase).
Módulos bajo prueba: usuariosApp/validators.py y altosDeCarpayApp/texto.py
"""
from django.core.exceptions import ValidationError
from django.test import SimpleTestCase
from rest_framework import serializers

from altosDeCarpayApp.texto import limpiar_texto, texto_obligatorio
from usuariosApp.validators import calcular_digito_verificador, normalizar_rut, validar_rut


class NormalizarRutTests(SimpleTestCase):
    def test_quita_puntos_guion_y_espacios(self):
        self.assertEqual(normalizar_rut("12.345.678-5"), "123456785")
        self.assertEqual(normalizar_rut(" 12 345 678 - 5 "), "123456785")

    def test_k_queda_en_mayuscula(self):
        self.assertEqual(normalizar_rut("10.000.013-k"), "10000013K")


class DigitoVerificadorTests(SimpleTestCase):
    def test_digitos_numericos(self):
        self.assertEqual(calcular_digito_verificador("12345678"), "5")
        self.assertEqual(calcular_digito_verificador("11111111"), "1")
        self.assertEqual(calcular_digito_verificador("7000000"), "8")

    def test_resto_10_es_k(self):
        self.assertEqual(calcular_digito_verificador("10000013"), "K")

    def test_resto_11_es_cero(self):
        self.assertEqual(calcular_digito_verificador("10000004"), "0")


class ValidarRutTests(SimpleTestCase):
    def test_ruts_validos_en_distintos_formatos(self):
        for rut in ("12.345.678-5", "12345678-5", "123456785", "10.000.013-K", "10000013k", "7.000.000-8"):
            with self.subTest(rut=rut):
                validar_rut(rut)  # no debe lanzar excepción

    def test_digito_verificador_incorrecto(self):
        with self.assertRaisesMessage(ValidationError, "dígito verificador"):
            validar_rut("12.345.678-9")

    def test_formatos_invalidos(self):
        for rut in ("", "abc", "123", "12.345.678-Z", "1234567890-1", "12345678-55"):
            with self.subTest(rut=rut):
                with self.assertRaises(ValidationError):
                    validar_rut(rut)


class LimpiarTextoTests(SimpleTestCase):
    def test_elimina_etiquetas_html(self):
        self.assertEqual(limpiar_texto("<b>Hola</b> <script>alert(1)</script>mundo"), "Hola alert(1)mundo")

    def test_elimina_caracter_nulo_y_espacios_extremos(self):
        self.assertEqual(limpiar_texto("  hola\x00 "), "hola")

    def test_none_devuelve_cadena_vacia(self):
        self.assertEqual(limpiar_texto(None), "")

    def test_texto_obligatorio_rechaza_vacio_o_solo_html(self):
        for valor in ("", "   ", "<p></p>", None):
            with self.subTest(valor=valor):
                with self.assertRaises(serializers.ValidationError):
                    texto_obligatorio(valor, "El asunto")

    def test_texto_obligatorio_devuelve_texto_limpio(self):
        self.assertEqual(texto_obligatorio("  <i>Consulta</i> "), "Consulta")

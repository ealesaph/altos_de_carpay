"""PRUEBAS UNITARIAS · Reglas de negocio de la reserva.

Se prueban cálculos aislados con objetos en memoria (sin guardar en la base de datos):
- Anticipo del 30 %, saldo pendiente y plazos (reservasApp/models.py)
- Bloques horarios 13:00–18:00 y 19:00–00:00 (services.validar_bloque_horario)
- Devolución según las condiciones de arriendo (services.calcular_devolucion)
"""
from datetime import date, datetime, time, timedelta
from decimal import Decimal

from django.test import SimpleTestCase, override_settings
from django.utils import timezone
from rest_framework.exceptions import ValidationError

from reservasApp.models import Reserva, ReservaSector, ReservaServicio
from reservasApp.services import calcular_devolucion, validar_bloque_horario

REGLAS = dict(ANTICIPO_PORCENTAJE=Decimal("30"), PLAZO_PAGO_ANTICIPO_HORAS=24, PLAZO_SALDO_HORAS_ANTES=24,
              BLOQUES_HORARIOS=((13, 18), (19, 24)), DIAS_DEVOLUCION_PARCIAL=7)


def reserva_en_memoria(total="100000.00", pagado="0", fecha=date(2026, 12, 20), hora=time(13, 0), horas=5):
    reserva = Reserva(fecha=fecha, hora_inicio=hora, horas_reservadas=horas, monto_total=Decimal(total))
    reserva.total_pagado = Decimal(pagado)  # valor que la API calcula con una anotación de la consulta
    return reserva


@override_settings(**REGLAS)
class MontosDeLaReservaTests(SimpleTestCase):
    def test_anticipo_es_30_por_ciento(self):
        self.assertEqual(reserva_en_memoria("100000.00").monto_anticipo, Decimal("30000"))
        self.assertEqual(reserva_en_memoria("76500.00").monto_anticipo, Decimal("22950"))

    def test_anticipo_se_redondea_al_peso(self):
        self.assertEqual(reserva_en_memoria("33333.00").monto_anticipo, Decimal("10000"))  # 9.999,9 -> 10.000
        self.assertEqual(reserva_en_memoria("10005.00").monto_anticipo, Decimal("3002"))   # 3.001,5 -> 3.002

    def test_saldo_pendiente(self):
        self.assertEqual(reserva_en_memoria("100000.00", "30000").saldo_pendiente, Decimal("70000.00"))

    def test_saldo_nunca_es_negativo(self):
        self.assertEqual(reserva_en_memoria("100000.00", "120000").saldo_pendiente, Decimal("0.00"))

    def test_anticipo_pagado(self):
        self.assertFalse(reserva_en_memoria("100000.00", "29999").anticipo_pagado)
        self.assertTrue(reserva_en_memoria("100000.00", "30000").anticipo_pagado)

    def test_subtotales_de_lineas(self):
        self.assertEqual(ReservaSector(horas=5, valor_hora=Decimal("25000")).subtotal, Decimal("125000"))
        self.assertEqual(ReservaServicio(cantidad=3, valor_unitario=Decimal("5000")).subtotal, Decimal("15000"))


@override_settings(**REGLAS)
class PlazosTests(SimpleTestCase):
    def test_inicio_y_fin_del_evento(self):
        r = reserva_en_memoria(fecha=date(2026, 12, 20), hora=time(19, 0), horas=5)
        self.assertEqual(timezone.localtime(r.inicio).replace(tzinfo=None), datetime(2026, 12, 20, 19, 0))
        self.assertEqual(r.fin - r.inicio, timedelta(hours=5))

    def test_saldo_vence_24_horas_antes_del_inicio(self):
        r = reserva_en_memoria(hora=time(19, 0))
        self.assertEqual(r.inicio - r.plazo_saldo_vence, timedelta(hours=24))

    def test_anticipo_vence_24_horas_despues_de_crear(self):
        r = reserva_en_memoria()
        r.fecha_creacion = timezone.now() - timedelta(hours=25)
        self.assertTrue(r.plazo_anticipo_vencido)
        r.fecha_creacion = timezone.now() - timedelta(hours=23)
        self.assertFalse(r.plazo_anticipo_vencido)


@override_settings(**REGLAS)
class BloquesHorariosTests(SimpleTestCase):
    def test_horarios_validos(self):
        for hora, horas in ((13, 5), (13, 1), (17, 1), (19, 5), (23, 1)):
            with self.subTest(hora=hora, horas=horas):
                validar_bloque_horario(time(hora, 0), horas)

    def test_horarios_invalidos(self):
        casos = (
            (10, 2),   # antes de abrir
            (16, 3),   # 16–19 cruza de un bloque al otro
            (22, 6),   # pasa la medianoche
            (18, 1),   # 18:00–19:00 es el intervalo de limpieza
        )
        for hora, horas in casos:
            with self.subTest(hora=hora, horas=horas):
                with self.assertRaises(ValidationError):
                    validar_bloque_horario(time(hora, 0), horas)

    def test_solo_horas_en_punto(self):
        with self.assertRaises(ValidationError):
            validar_bloque_horario(time(13, 30), 2)


@override_settings(**REGLAS)
class DevolucionTests(SimpleTestCase):
    def setUp(self):
        self.reserva = reserva_en_memoria("100000.00", "100000", fecha=date(2026, 12, 31), hora=time(13, 0))
        self.inicio = self.reserva.inicio

    def test_recinto_cancela_devuelve_todo(self):
        monto, _ = calcular_devolucion(self.reserva, Reserva.OrigenCancelacion.RECINTO,
                                       ahora=self.inicio - timedelta(days=1))
        self.assertEqual(monto, Decimal("100000"))

    def test_cliente_con_mas_de_7_dias_pierde_solo_el_anticipo(self):
        monto, _ = calcular_devolucion(self.reserva, Reserva.OrigenCancelacion.CLIENTE,
                                       ahora=self.inicio - timedelta(days=20))
        self.assertEqual(monto, Decimal("70000"))

    def test_cliente_con_7_dias_o_menos_no_recibe_devolucion(self):
        monto, _ = calcular_devolucion(self.reserva, Reserva.OrigenCancelacion.CLIENTE,
                                       ahora=self.inicio - timedelta(days=3))
        self.assertEqual(monto, Decimal("0.00"))

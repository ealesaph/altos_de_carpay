"""PRUEBAS DE SEGURIDAD · API REST (se ejecutan con el runner de Django en una base de prueba aislada).

Cubre el RNF-02 del plan de pruebas: autenticación JWT, control de acceso por rol (RBAC),
aislamiento entre clientes, inyección SQL, XSS, asignación masiva, manipulación de precios
y exposición de datos sensibles.
"""
from datetime import timedelta
from decimal import Decimal

from django.core.cache import cache
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework.test import APIClient

from consultasApp.models import Consulta
from reservasApp.models import Reserva, ReservaSector, Sector
from usuariosApp.models import Usuario

CLAVE = "ClaveSegura2026!"


@override_settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend")
class SeguridadApiTests(TestCase):
    def setUp(self):
        cache.clear()  # los límites de solicitudes de otras pruebas no deben interferir
        self.api = APIClient()
        self.admin = Usuario.objects.create_user(
            correo="admin@example.com", password=CLAVE, rut="22222222-2", nombre="Ana",
            apellido_paterno="Admin", apellido_materno="Prueba", rol=Usuario.Rol.ADMIN,
        )
        self.cliente = Usuario.objects.create_user(
            correo="cliente@example.com", password=CLAVE, rut="11111111-1", nombre="Camila",
            apellido_paterno="Rojas", apellido_materno="Díaz",
        )
        self.otro = Usuario.objects.create_user(
            correo="otro@example.com", password=CLAVE, rut="12.345.678-5", nombre="Pedro",
            apellido_paterno="Soto", apellido_materno="Lopez",
        )
        self.sector = Sector.objects.create(nombre="Jardín", valor_hora=Decimal("20000.00"))
        inicio = timezone.localtime() + timedelta(days=15)
        self.reserva_cliente = Reserva.objects.create(
            usuario=self.cliente, fecha=inicio.date(), hora_inicio=inicio.time().replace(hour=13, minute=0,
                                                                                         second=0, microsecond=0),
            horas_reservadas=5, monto_total=Decimal("100000.00"), estado=Reserva.Estado.PENDIENTE,
        )
        ReservaSector.objects.create(reserva=self.reserva_cliente, sector=self.sector, horas=5,
                                     valor_hora=Decimal("20000.00"))

    # --- Autenticación -------------------------------------------------------------------------
    def test_sin_token_las_rutas_privadas_responden_401(self):
        for ruta in ("/api/v1/reservas/", "/api/v1/auth/perfil/", "/api/v1/incidencias/",
                     "/api/v1/auditoria/logs/", "/api/v1/usuarios/", "/api/v1/auth/mis-datos/"):
            with self.subTest(ruta=ruta):
                self.assertEqual(self.api.get(ruta).status_code, 401)

    def test_token_falsificado_o_alterado_es_rechazado(self):
        r = self.api.post("/api/v1/auth/login/", {"correo": "cliente@example.com", "password": CLAVE}, format="json")
        token = r.data["access"]
        alterado = token[:-4] + ("AAAA" if not token.endswith("AAAA") else "BBBB")
        for falso in (alterado, "abc.def.ghi", "Bearer"):
            with self.subTest(token=falso[:20]):
                self.api.credentials(HTTP_AUTHORIZATION=f"Bearer {falso}")
                self.assertEqual(self.api.get("/api/v1/auth/perfil/").status_code, 401)

    def test_login_con_clave_incorrecta_no_entrega_token(self):
        r = self.api.post("/api/v1/auth/login/", {"correo": "cliente@example.com", "password": "incorrecta"},
                          format="json")
        self.assertIn(r.status_code, (400, 401))
        self.assertNotIn("access", r.data)

    # --- Control de acceso por rol (RBAC) ----------------------------------------------------------
    def test_cliente_no_accede_a_rutas_de_administrador(self):
        self.api.force_authenticate(self.cliente)
        rutas = ("/api/v1/usuarios/", "/api/v1/auditoria/logs/", "/api/v1/reservas/exportar/",
                 "/api/v1/reservas/resumen/", "/api/v1/incidencias/")
        for ruta in rutas:
            with self.subTest(ruta=ruta):
                self.assertEqual(self.api.get(ruta).status_code, 403)

    def test_cliente_no_puede_registrar_entrada_ni_cambiar_estado(self):
        self.api.force_authenticate(self.cliente)
        pk = self.reserva_cliente.pk
        self.assertEqual(self.api.post(f"/api/v1/reservas/{pk}/validar-acceso/", {"tipo_registro": "Entrada"},
                                       format="json").status_code, 403)
        self.assertEqual(self.api.post(f"/api/v1/reservas/{pk}/estado/", {"estado": "Confirmado"},
                                       format="json").status_code, 403)

    def test_cliente_no_puede_pagar_por_transferencia(self):
        # Solo el administrador registra pagos manuales; un cliente no puede "aprobar" su propia reserva.
        self.api.force_authenticate(self.cliente)
        r = self.api.post(f"/api/v1/reservas/{self.reserva_cliente.pk}/pagos/",
                          {"tipo_pago": "Anticipo", "metodo_pago": "Transferencia"}, format="json")
        self.assertEqual(r.status_code, 403)
        self.reserva_cliente.refresh_from_db()
        self.assertEqual(self.reserva_cliente.estado, Reserva.Estado.PENDIENTE)

    # --- Aislamiento entre clientes (IDOR) ---------------------------------------------------------
    def test_un_cliente_no_ve_ni_modifica_reservas_ajenas(self):
        self.api.force_authenticate(self.otro)
        pk = self.reserva_cliente.pk
        self.assertEqual(self.api.get(f"/api/v1/reservas/{pk}/").status_code, 404)
        self.assertEqual(self.api.post(f"/api/v1/reservas/{pk}/cancelar/", {}, format="json").status_code, 404)
        self.assertEqual(self.api.post(f"/api/v1/reservas/{pk}/pagos/",
                                       {"tipo_pago": "Anticipo", "metodo_pago": "Webpay"},
                                       format="json").status_code, 404)
        self.assertEqual(self.api.get("/api/v1/reservas/").data["count"], 0)

    # --- Inyección SQL -----------------------------------------------------------------------------
    def test_inyeccion_sql_en_login_no_autentica(self):
        for carga in ("' OR '1'='1", "admin@example.com' --", "\" OR 1=1; DROP TABLE usuario; --"):
            with self.subTest(carga=carga):
                cache.clear()
                r = self.api.post("/api/v1/auth/login/", {"correo": carga, "password": carga}, format="json")
                self.assertIn(r.status_code, (400, 401))
                self.assertNotIn("access", r.data)
        self.assertEqual(Usuario.objects.count(), 3)  # la tabla sigue intacta

    def test_inyeccion_sql_en_filtros_no_filtra_datos(self):
        self.api.force_authenticate(self.admin)
        r = self.api.get("/api/v1/usuarios/", {"q": "' OR 1=1 --"})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data["count"], 0)
        self.api.force_authenticate(self.otro)
        r = self.api.get("/api/v1/reservas/", {"estado": "Pendiente' OR '1'='1"})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data["count"], 0)

    # --- XSS ---------------------------------------------------------------------------------------
    def test_consulta_con_script_se_guarda_sin_etiquetas(self):
        r = self.api.post("/api/v1/consultas/", {
            "nombre_contacto": "<img src=x onerror=alert(1)>Juan", "correo_contacto": "juan@example.com",
            "acepta_privacidad": True, "asunto": "<script>alert('xss')</script>Hola",
            "mensaje": "<b>Mensaje</b> con <a href='javascript:alert(1)'>enlace</a>",
        }, format="json")
        self.assertEqual(r.status_code, 201)
        consulta = Consulta.objects.get(pk=r.data["id_consulta"])
        for campo in (consulta.asunto, consulta.mensaje, consulta.nombre_contacto):
            self.assertNotIn("<", campo)
            self.assertNotIn("onerror", campo)

    # --- Asignación masiva y manipulación de datos -----------------------------------------------
    def test_registro_no_permite_crearse_como_administrador(self):
        r = self.api.post("/api/v1/auth/registro/", {
            "correo": "atacante@example.com", "rut": "7.000.000-8", "nombre": "Eva", "apellido_paterno": "Mora",
            "apellido_materno": "Vera", "password": CLAVE, "acepta_politica_datos": True,
            "rol": "Admin", "is_staff": True, "is_superuser": True,
        }, format="json")
        self.assertEqual(r.status_code, 201)
        usuario = Usuario.objects.get(correo="atacante@example.com")
        self.assertEqual(usuario.rol, Usuario.Rol.CLIENTE)
        self.assertFalse(usuario.is_superuser)

    def test_el_precio_lo_calcula_el_servidor(self):
        self.api.force_authenticate(self.otro)
        r = self.api.post("/api/v1/reservas/", {
            "fecha": (timezone.localdate() + timedelta(days=40)).isoformat(), "hora_inicio": "19:00",
            "acepta_condiciones": True, "sectores": [{"sector": self.sector.pk, "horas": 2}],
            "monto_total": "1.00", "estado": "Confirmado",
        }, format="json")
        self.assertEqual(r.status_code, 201)
        self.assertEqual(Decimal(r.data["monto_total"]), Decimal("40000.00"))  # 2 h x $20.000
        self.assertEqual(r.data["estado"], "Pendiente")

    def test_clave_debil_es_rechazada(self):
        r = self.api.post("/api/v1/auth/registro/", {
            "correo": "debil@example.com", "rut": "7.000.000-8", "nombre": "Eva", "apellido_paterno": "Mora",
            "apellido_materno": "Vera", "password": "123456", "acepta_politica_datos": True,
        }, format="json")
        self.assertEqual(r.status_code, 400)
        self.assertIn("password", r.data)

    # --- Exposición de datos sensibles -------------------------------------------------------------
    def test_perfil_no_expone_la_clave_ni_su_hash(self):
        self.api.force_authenticate(self.cliente)
        r = self.api.get("/api/v1/auth/perfil/")
        self.assertEqual(r.status_code, 200)
        texto = str(r.data)
        self.assertNotIn("password", r.data)
        self.assertNotIn("pbkdf2", texto)
        self.assertNotIn("bcrypt", texto)

    def test_la_clave_se_guarda_con_hash(self):
        self.cliente.refresh_from_db()
        self.assertNotEqual(self.cliente.password, CLAVE)
        self.assertTrue(self.cliente.check_password(CLAVE))

    def test_cabeceras_de_seguridad_en_la_api(self):
        r = self.api.get("/api/v1/sectores/")
        self.assertEqual(r["X-Frame-Options"], "DENY")
        self.assertEqual(r["X-Content-Type-Options"], "nosniff")

    def test_cierre_de_sesion_invalida_el_refresh_token(self):
        r = self.api.post("/api/v1/auth/login/", {"correo": "cliente@example.com", "password": CLAVE}, format="json")
        refresh = r.data["refresh"]
        self.api.credentials(HTTP_AUTHORIZATION=f"Bearer {r.data['access']}")
        self.assertEqual(self.api.post("/api/v1/auth/logout/", {"refresh": refresh}, format="json").status_code, 204)
        self.api.credentials()
        r = self.api.post("/api/v1/auth/refresh/", {"refresh": refresh}, format="json")
        self.assertEqual(r.status_code, 401)

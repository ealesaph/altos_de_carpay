package com.altosdecarpay.app.data

import com.altosdecarpay.app.data.model.LoginResponse
import com.altosdecarpay.app.data.model.PerfilResponse
import com.altosdecarpay.app.data.model.ReservaResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseIntegrationMappingTest {

    private val gson: Gson = GsonBuilder().setLenient().create()

    @Test
    fun testLoginResponseDeserialization() {
        val json = """
            {
                "access": "fake_access_token_123",
                "refresh": "fake_refresh_token_456",
                "usuario": {
                    "id_usuario": 1,
                    "nombre": "Admin Carpay",
                    "rol": "Admin"
                }
            }
        """.trimIndent()

        val loginResponse = gson.fromJson(json, LoginResponse::class.java)
        assertNotNull(loginResponse)
        assertEquals("fake_access_token_123", loginResponse.access)
        assertEquals("fake_refresh_token_456", loginResponse.refresh)
        assertEquals(1, loginResponse.usuario.idUsuario)
        assertEquals("Admin Carpay", loginResponse.usuario.nombre)
        assertEquals("Admin", loginResponse.usuario.rol)
    }

    @Test
    fun testPerfilResponseDeserialization() {
        val json = """
            {
                "id_usuario": 2,
                "correo": "camila@carpay.com",
                "rut": "12345678-9",
                "nombre": "Camila",
                "apellido_paterno": "Rojas",
                "apellido_materno": "Soto",
                "telefono": "+56912345678",
                "rol": "Cliente",
                "estado": "Activo",
                "ultima_conexion": "2025-02-20T10:00:00Z"
            }
        """.trimIndent()

        val perfil = gson.fromJson(json, PerfilResponse::class.java)
        assertNotNull(perfil)
        assertEquals(2, perfil.idUsuario)
        assertEquals("camila@carpay.com", perfil.correo)
        assertEquals("Camila Rojas Soto", perfil.nombreCompleto)
        assertEquals("CR", perfil.iniciales)
        assertEquals("Cliente", perfil.rol)
    }

    @Test
    fun testReservaResponseDeserialization() {
        val json = """
            {
                "id_reserva": 248,
                "usuario": 2,
                "cliente": "Camila Rojas",
                "correo_cliente": "camila@carpay.com",
                "fecha": "2025-02-20",
                "hora_inicio": "16:00:00",
                "hora_fin": "20:00:00",
                "horas_reservadas": 4,
                "estado": "En curso",
                "monto_total": "40000.00",
                "monto_anticipo": "20000.00",
                "monto_pagado": "20000.00",
                "saldo_pendiente": "20000.00",
                "plazo_anticipo_vence": "2025-02-18T16:00:00Z",
                "fecha_creacion": "2025-02-15T12:00:00Z",
                "token_qr": "QR-RES-0248",
                "sectores": [
                    {
                        "id_sector": 1,
                        "nombre": "Quincho Los Aromos",
                        "horas": 4,
                        "valor_hora": "10000.00",
                        "subtotal": "40000.00"
                    }
                ],
                "servicios": [],
                "pagos": [
                    {
                        "id_pago": 10,
                        "monto": "20000.00",
                        "tipo_pago": "Anticipo",
                        "metodo_pago": "Webpay",
                        "transaccion_id": "TXN-999",
                        "estado_pago": "Aprobado",
                        "fecha_pago": "2025-02-15T12:05:00Z"
                    }
                ]
            }
        """.trimIndent()

        val reserva = gson.fromJson(json, ReservaResponse::class.java)
        assertNotNull(reserva)
        assertEquals(248, reserva.idReserva)
        assertEquals("RES-0248", reserva.idFormateado)
        assertEquals("Camila Rojas", reserva.cliente)
        assertEquals("En curso", reserva.estado)
        assertEquals("Quincho Los Aromos", reserva.sectorPrincipal)
        assertEquals(1, reserva.sectores.size)
        assertEquals(1, reserva.pagos.size)
        assertEquals("Aprobado", reserva.pagos[0].estadoPago)
    }
}

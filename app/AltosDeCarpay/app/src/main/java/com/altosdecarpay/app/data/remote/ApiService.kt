package com.altosdecarpay.app.data.remote

import com.altosdecarpay.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/registro/")
    suspend fun registro(@Body request: RegistroRequest): Response<PerfilResponse>

    @POST("auth/refresh/")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<TokenRefreshResponse>

    @POST("auth/logout/")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    @GET("auth/perfil/")
    suspend fun getPerfil(): Response<PerfilResponse>

    @PATCH("auth/perfil/")
    suspend fun updatePerfil(@Body changes: Map<String, String>): Response<PerfilResponse>

    @POST("auth/cambiar-password/")
    suspend fun cambiarPassword(@Body request: CambioPasswordRequest): Response<Map<String, String>>

    @GET("auth/politica-privacidad/")
    suspend fun getPoliticaPrivacidad(): Response<PoliticaPrivacidadResponse>

    @POST("auth/consentimiento/")
    suspend fun registrarConsentimiento(@Body request: ConsentimientoRequest): Response<Map<String, Any>>

    @GET("auth/mis-datos/")
    suspend fun getMisDatosArco(): Response<Map<String, Any>>

    @POST("auth/desactivar-cuenta/")
    suspend fun desactivarCuentaArco(): Response<Map<String, String>>

    // ── Sectores ──

    @GET("sectores/")
    suspend fun getSectores(): Response<PaginatedResponse<SectorResponse>>

    @GET("sectores/{id}/")
    suspend fun getSector(@Path("id") id: Int): Response<SectorResponse>

    @PATCH("sectores/{id}/")
    suspend fun actualizarSector(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<SectorResponse>

    // ── Servicios ──

    @GET("servicios/")
    suspend fun getServicios(): Response<PaginatedResponse<ServicioResponse>>

    @POST("servicios/")
    suspend fun crearServicio(@Body request: ServicioCrearRequest): Response<ServicioResponse>

    @PATCH("servicios/{id}/")
    suspend fun actualizarServicio(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<ServicioResponse>

    // ── Auditoría & Bitácora ──

    @GET("auditoria/logs/")
    suspend fun getAuditoriaLogs(
        @Query("tabla") tabla: String? = null,
        @Query("id_registro") idRegistro: Int? = null
    ): Response<PaginatedResponse<LogModificacionResponse>>

    @GET("reservas/{id}/validar-acceso/")
    suspend fun getValidacionesAcceso(@Path("id") id: Int): Response<List<ValidacionAccesoResponse>>

    @POST("reservas/{id}/validar-acceso/")
    suspend fun validarAcceso(
        @Path("id") id: Int,
        @Body request: ValidacionAccesoRequest
    ): Response<ValidacionAccesoResponse>

    @GET("incidencias/")
    suspend fun getIncidencias(
        @Query("reserva") reservaId: Int? = null
    ): Response<PaginatedResponse<BitacoraIncidenciaResponse>>

    @POST("incidencias/")
    suspend fun crearIncidencia(@Body request: BitacoraIncidenciaRequest): Response<BitacoraIncidenciaResponse>

    // ── Reservas ──

    @GET("reservas/")
    suspend fun getReservas(
        @Query("estado") estado: String? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("cliente") cliente: Int? = null,
        @Query("vista") vista: String? = null,
        @Query("page_size") pageSize: Int = 100
    ): Response<PaginatedResponse<ReservaResponse>>

    @GET("reservas/{id}/")
    suspend fun getReserva(@Path("id") id: Int): Response<ReservaResponse>

    @POST("reservas/")
    suspend fun crearReserva(@Body request: ReservaCrearRequest): Response<ReservaResponse>

    @POST("reservas/{id}/cancelar/")
    suspend fun cancelarReserva(
        @Path("id") id: Int,
        @Body request: CancelacionRequest = CancelacionRequest()
    ): Response<ReservaResponse>

    @GET("reservas/{id}/simular-cancelacion/")
    suspend fun simularCancelacion(
        @Path("id") id: Int,
        @Query("origen") origen: String? = null
    ): Response<SimulacionCancelacionResponse>

    @POST("reservas/{id}/registrar-devolucion/")
    suspend fun registrarDevolucion(@Path("id") id: Int): Response<ReservaResponse>

    @POST("reservas/{id}/estado/")
    suspend fun cambiarEstadoReserva(
        @Path("id") id: Int,
        @Body request: CambioEstadoRequest
    ): Response<ReservaResponse>

    @GET("reservas/{id}/pagos/")
    suspend fun getPagosReserva(@Path("id") id: Int): Response<List<PagoResponse>>

    @POST("reservas/{id}/pagos/")
    suspend fun crearPago(
        @Path("id") id: Int,
        @Body request: PagoCrearRequest
    ): Response<PagoResponse>

    // ── Consultas ──

    @GET("consultas/")
    suspend fun getConsultas(
        @Query("estado") estado: String? = null,
        @Query("page_size") pageSize: Int = 100
    ): Response<PaginatedResponse<ConsultaResponse>>

    @GET("consultas/{id}/")
    suspend fun getConsulta(@Path("id") id: Int): Response<ConsultaResponse>

    @POST("consultas/")
    suspend fun crearConsulta(@Body request: ConsultaCrearRequest): Response<ConsultaResponse>

    @POST("consultas/{id}/responder/")
    suspend fun responderConsulta(
        @Path("id") id: Int,
        @Body request: RespuestaRequest
    ): Response<ConsultaResponse>
}

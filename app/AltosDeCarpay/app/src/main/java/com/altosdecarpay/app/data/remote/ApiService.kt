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

    // ── Sectores ──

    @GET("sectores/")
    suspend fun getSectores(): Response<List<SectorResponse>>

    @GET("sectores/{id}/")
    suspend fun getSector(@Path("id") id: Int): Response<SectorResponse>

    // ── Servicios ──

    @GET("servicios/")
    suspend fun getServicios(): Response<List<ServicioResponse>>

    // ── Reservas ──

    @GET("reservas/")
    suspend fun getReservas(
        @Query("estado") estado: String? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("cliente") cliente: Int? = null
    ): Response<PaginatedResponse<ReservaResponse>>

    @GET("reservas/{id}/")
    suspend fun getReserva(@Path("id") id: Int): Response<ReservaResponse>

    @POST("reservas/")
    suspend fun crearReserva(@Body request: ReservaCrearRequest): Response<ReservaResponse>

    @POST("reservas/{id}/cancelar/")
    suspend fun cancelarReserva(@Path("id") id: Int): Response<ReservaResponse>

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
        @Query("estado") estado: String? = null
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

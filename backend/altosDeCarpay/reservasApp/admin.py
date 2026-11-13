from django.contrib import admin

from .models import Pago, Reserva, ReservaSector, ReservaServicio, Review, Sector, Servicio


class ReservaSectorInline(admin.TabularInline):
    model = ReservaSector
    extra = 0


class ReservaServicioInline(admin.TabularInline):
    model = ReservaServicio
    extra = 0


@admin.register(Reserva)
class ReservaAdmin(admin.ModelAdmin):
    list_display = ("id_reserva", "usuario", "fecha", "hora_inicio", "estado", "monto_total")
    list_filter = ("estado", "fecha")
    search_fields = ("usuario__correo", "usuario__rut")
    readonly_fields = ("fecha_creacion", "condiciones_aceptadas_en", "token_qr")
    inlines = [ReservaSectorInline, ReservaServicioInline]


@admin.register(Sector)
class SectorAdmin(admin.ModelAdmin):
    list_display = ("id_sector", "nombre", "valor_hora", "estado")
    list_filter = ("estado",)


@admin.register(Servicio)
class ServicioAdmin(admin.ModelAdmin):
    list_display = ("id_servicio", "nombre", "valor", "estado")
    list_filter = ("estado",)


@admin.register(Pago)
class PagoAdmin(admin.ModelAdmin):
    list_display = ("id_pago", "reserva", "monto", "tipo_pago", "metodo_pago", "estado_pago", "fecha_pago")
    list_filter = ("estado_pago", "metodo_pago", "tipo_pago")
    readonly_fields = ("fecha_pago",)


@admin.register(Review)
class ReviewAdmin(admin.ModelAdmin):
    list_display = ("id_review", "reserva", "evaluador", "evaluado", "calificacion", "fecha")

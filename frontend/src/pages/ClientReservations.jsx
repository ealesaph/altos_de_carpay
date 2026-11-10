import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Calendar, Clock, AlertCircle, LogOut } from 'lucide-react';

export default function ClientReservations() {
  const location = useLocation();

  // Si no viene de un flujo de compra, usa los datos de ejemplo de Camila
  const reservaActual = location.state || {
    nombreEspacio: 'Terraza superior',
    precioTotal: 300000,
    montoAPagarHoy: 90000,
    saldoPendiente: 210000,
    tipoPago: 'anticipo',
    img: 'https://images.unsplash.com/photo-1544984243-ec57ea16fe25?auto=format&fit=crop&w=800&q=80',
    fecha: '2026-12-12',
    bloque: '13:00–18:00 (5 horas)',
    nombreEvento: 'Un almuerzo en familia',
    asistentes: '30'
  };

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased selection:bg-forest-100 selection:text-forest-900">
      
      {/* Header específico de Cliente */}
      <header className="bg-sand-50/95 backdrop-blur-md border-b border-sand-200/60 sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-forest-900">
            <span className="w-7 h-7 rounded-full bg-forest-100 flex items-center justify-center text-forest-800 text-sm">
              🍃
            </span>
            <span className="font-serif text-xl font-bold tracking-tight">
              Altos de carpay
            </span>
          </Link>

          <div className="flex items-center gap-5 text-xs">
            <span className="bg-sand-100 text-forest-900 px-3 py-1.5 rounded-full font-semibold">
              Mi cuenta · Camila Rojas
            </span>
            <Link to="/" className="text-gray-500 hover:text-red-700 flex items-center gap-1 transition-colors">
              <LogOut size={14} /> Salir
            </Link>
          </div>
        </div>
      </header>

      {/* Contenido del espacio de cliente */}
      <main className="max-w-5xl mx-auto px-6 py-10 space-y-8">
        
        <div className="flex flex-col sm:flex-row justify-between sm:items-end gap-4 border-b border-sand-200/80 pb-6">
          <div>
            <span className="text-xs uppercase font-semibold tracking-wider text-forest-700 block mb-1">
              Área de clientes
            </span>
            <h1 className="font-serif text-3xl md:text-4xl text-forest-900">
              Mis reservas
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              Revisa los detalles de tus arriendos, saldos pendientes y fechas límites de pago.
            </p>
          </div>

          <Link 
            to="/reservar/espacio"
            className="bg-forest-800 text-white text-xs font-semibold px-5 py-2.5 rounded-full hover:bg-forest-900 transition-colors shadow-sm self-start sm:self-auto"
          >
            + Nueva reserva
          </Link>
        </div>

        {/* Tarjeta de la Reserva Activa */}
        <div className="bg-white rounded-3xl border border-sand-200/90 shadow-sm overflow-hidden">
          
          <div className="p-6 md:p-8 grid grid-cols-1 md:grid-cols-12 gap-8 items-center">
            
            <div className="md:col-span-4 rounded-2xl overflow-hidden h-48 w-full bg-sand-100 border border-sand-200">
              <img 
                src={reservaActual.img} 
                alt={reservaActual.nombreEspacio} 
                className="w-full h-full object-cover block"
              />
            </div>

            <div className="md:col-span-8 space-y-4">
              
              <div className="flex flex-wrap justify-between items-start gap-2">
                <div>
                  <span className="text-[11px] font-mono text-gray-400 font-semibold uppercase">Ref: RES-0261</span>
                  <h2 className="font-serif text-2xl font-bold text-forest-900 mt-0.5">
                    {reservaActual.nombreEvento}
                  </h2>
                  <span className="text-sm font-semibold text-forest-800 block">
                    {reservaActual.nombreEspacio}
                  </span>
                </div>

                <span className={`text-xs px-3 py-1 rounded-full font-semibold border ${
                  reservaActual.saldoPendiente > 0 
                    ? 'bg-amber-50 text-amber-900 border-amber-200' 
                    : 'bg-green-50 text-green-900 border-green-200'
                }`}>
                  {reservaActual.saldoPendiente > 0 ? 'Saldo pendiente' : 'Completamente pagada'}
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-2 text-xs text-gray-600">
                <div>
                  <span className="text-[10px] text-gray-400 uppercase font-semibold block">Fecha</span>
                  <span className="font-semibold text-gray-800 flex items-center gap-1 mt-0.5">
                    <Calendar size={13} className="text-forest-800" /> {reservaActual.fecha}
                  </span>
                </div>

                <div>
                  <span className="text-[10px] text-gray-400 uppercase font-semibold block">Horario</span>
                  <span className="font-semibold text-gray-800 flex items-center gap-1 mt-0.5">
                    <Clock size={13} className="text-forest-800" /> {reservaActual.bloque}
                  </span>
                </div>

                <div>
                  <span className="text-[10px] text-gray-400 uppercase font-semibold block">Asistentes</span>
                  <span className="font-semibold text-gray-800 mt-0.5 block">
                    {reservaActual.asistentes} personas
                  </span>
                </div>
              </div>

              <div className="pt-3 border-t border-sand-100 flex flex-wrap justify-between items-center gap-4">
                <div className="text-xs">
                  <span className="text-gray-400 block">Total arriendo: <strong>${Number(reservaActual.precioTotal || 0).toLocaleString('es-CL')}</strong></span>
                  <span className="text-forest-900 font-semibold">
                    Abonado: ${Number(reservaActual.montoAPagarHoy || 0).toLocaleString('es-CL')}
                  </span>
                </div>

                <div className="text-right">
                  <span className="text-[11px] text-gray-400 uppercase font-semibold block">Saldo pendiente</span>
                  <span className="text-2xl font-serif font-bold text-forest-900">
                    ${Number(reservaActual.saldoPendiente || 0).toLocaleString('es-CL')}
                  </span>
                </div>
              </div>

            </div>

          </div>

          {reservaActual.saldoPendiente > 0 && (
            <div className="bg-amber-50/80 p-4 border-t border-amber-200/80 flex items-center justify-between gap-4 text-xs text-amber-900">
              <div className="flex items-center gap-2">
                <AlertCircle size={16} className="text-amber-800 shrink-0" />
                <span>
                  <strong>Plazo de liquidación:</strong> Paga el saldo restante de ${Number(reservaActual.saldoPendiente || 0).toLocaleString('es-CL')} hasta 24 horas antes del evento ({reservaActual.fecha}) para no perder la reserva.
                </span>
              </div>
              <button 
                type="button"
                onClick={() => alert("Simulación: Pasarela para pagar el saldo restante.")}
                className="bg-amber-900 text-white font-semibold px-4 py-1.5 rounded-xl hover:bg-amber-950 transition-colors shrink-0"
              >
                Pagar saldo
              </button>
            </div>
          )}

        </div>

        {/* Mensajes */}
        <div className="bg-white p-6 rounded-3xl border border-sand-200/90 shadow-sm space-y-4">
          <h3 className="font-serif text-lg font-semibold text-forest-900">
            Mensajes sobre tu reserva
          </h3>
          <div className="p-4 rounded-2xl bg-sand-50/60 border border-sand-200/80 text-xs text-gray-600 space-y-2">
            <div className="flex justify-between items-center font-semibold text-forest-900">
              <span>Equipo Altos de Carpay</span>
              <span className="text-[10px] text-gray-400">Hace 1 día</span>
            </div>
            <p>
              "Hola Camila, confirmamos la recepción de tu anticipo. Te recordamos que la mantención y limpieza del quincho está incluida. Si necesitas coordinar la visita previa, puedes responder a este hilo."
            </p>
          </div>
        </div>

      </main>

    </div>
  );
}
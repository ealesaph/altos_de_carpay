import { Link, useLocation } from 'react-router-dom';
import { Check, Mail, Clock, ArrowRight } from 'lucide-react';

export default function Confirmation() {
  const location = useLocation();

  const datosReserva = location.state || {
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
      
      {/* Header */}
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

          <div className="flex items-center gap-6 text-xs text-gray-600 font-medium">
            <span className="bg-sand-100 text-forest-900 px-3 py-1.5 rounded-full font-semibold">
              Mi cuenta · Camila
            </span>
            <Link to="/mis-reservas" state={datosReserva} className="text-forest-800 font-semibold hover:underline">
              Mis reservas
            </Link>
          </div>
        </div>
      </header>

      {/* Stepper (Paso 4 Activo) */}
      <div className="border-b border-sand-200/80 bg-white/50 py-4">
        <div className="max-w-6xl mx-auto px-6 flex items-center justify-center gap-4 sm:gap-10 text-xs">
          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-forest-800 text-white flex items-center justify-center text-[10px]">
              <Check size={12} />
            </span>
            <span className="hidden sm:inline">Espacio y fecha</span>
          </div>
          <div className="w-8 h-px bg-forest-800"></div>
          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-forest-800 text-white flex items-center justify-center text-[10px]">
              <Check size={12} />
            </span>
            <span className="hidden sm:inline">Tu cuenta</span>
          </div>
          <div className="w-8 h-px bg-forest-800"></div>
          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-forest-800 text-white flex items-center justify-center text-[10px]">
              <Check size={12} />
            </span>
            <span className="hidden sm:inline">Revisión y pago</span>
          </div>
          <div className="w-8 h-px bg-forest-800"></div>
          <div className="flex items-center gap-2 text-forest-900 font-bold">
            <span className="w-5 h-5 rounded-full bg-forest-800 text-white flex items-center justify-center text-[10px]">
              4
            </span>
            <span>Confirmación</span>
          </div>
        </div>
      </div>

      {/* Contenido Principal */}
      <main className="max-w-6xl mx-auto px-6 py-10">
        
        <div className="flex items-start gap-4 mb-8">
          <div className="w-12 h-12 rounded-full bg-forest-100 flex items-center justify-center text-forest-800 shrink-0">
            <Check size={24} className="stroke-[2.5]" />
          </div>
          <div>
            <h1 className="font-serif text-3xl md:text-4xl text-forest-900 leading-tight">
              Tu encuentro está reservado, Camila
            </h1>
            <p className="text-xs text-gray-600 mt-1">
              Reserva confirmada con {datosReserva.tipoPago === 'anticipo' ? 'anticipo' : 'pago total'}. {datosReserva.saldoPendiente > 0 ? `Queda un saldo de $${datosReserva.saldoPendiente.toLocaleString('es-CL')} por pagar.` : 'El arriendo está pagado en su totalidad.'}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* LADO IZQUIERDO */}
          <div className="lg:col-span-7 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
            <div className="flex justify-between items-center pb-2">
              <span className="text-xs font-mono text-gray-400 font-semibold tracking-wider">RES-0261</span>
              <span className="text-xs bg-forest-100 text-forest-900 font-semibold px-3 py-1 rounded-full border border-forest-200/50">
                {datosReserva.saldoPendiente > 0 ? 'Confirmada · saldo pendiente' : 'Confirmada · pagada'}
              </span>
            </div>

            <div className="rounded-2xl overflow-hidden h-52 w-full bg-sand-100 border border-sand-200">
              <img 
                src={datosReserva.img} 
                alt={datosReserva.nombreEspacio} 
                className="w-full h-full object-cover block"
              />
            </div>

            <div>
              <h2 className="font-serif text-2xl font-semibold text-forest-900">
                {datosReserva.nombreEvento}
              </h2>
              <p className="text-sm font-medium text-forest-800 mt-0.5">
                {datosReserva.nombreEspacio}
              </p>
              <p className="text-xs text-gray-500">La Reina, Santiago</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-sand-100 text-xs">
              <div>
                <span className="text-[11px] uppercase tracking-wider text-gray-400 font-semibold block mb-0.5">Fecha y horario</span>
                <span className="font-semibold text-gray-800 block text-sm">{datosReserva.fecha}</span>
                <span className="text-gray-500">{datosReserva.bloque}</span>
              </div>
              <div>
                <span className="text-[11px] uppercase tracking-wider text-gray-400 font-semibold block mb-0.5">Asistentes</span>
                <span className="font-semibold text-gray-800 block text-sm">{datosReserva.asistentes} personas</span>
                <span className="text-gray-500">Aforo referencial</span>
              </div>
            </div>

            <div className="pt-2 text-xs text-gray-500">
              <span>Camila Rojas · camila@ejemplo.cl</span>
            </div>
          </div>

          {/* LADO DERECHO */}
          <div className="lg:col-span-5 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
            <h2 className="font-serif text-2xl font-semibold text-forest-900">
              Tu pago y saldo
            </h2>

            <div className="space-y-2 text-xs text-gray-600 border-b border-sand-100 pb-4">
              <div className="flex justify-between">
                <span>Total arriendo · 5 horas</span>
                <span>${datosReserva.precioTotal.toLocaleString('es-CL')}</span>
              </div>
            </div>

            <div className="bg-forest-50/70 p-4 rounded-2xl border border-forest-200/60 flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs text-forest-900">
                <Check size={16} className="text-forest-700" />
                <div>
                  <span className="font-bold block">Abonado · {datosReserva.tipoPago === 'anticipo' ? 'anticipo 30%' : 'total 100%'}</span>
                  <span className="text-[10px] text-gray-500">Pago simulado</span>
                </div>
              </div>
              <span className="font-serif text-xl font-bold text-forest-900">
                ${datosReserva.montoAPagarHoy.toLocaleString('es-CL')}
              </span>
            </div>

            <div className="py-2">
              <span className="text-xs text-gray-500 block uppercase tracking-wider font-semibold">
                Saldo pendiente
              </span>
              <span className="font-serif text-4xl font-bold text-forest-900 block mt-1">
                ${datosReserva.saldoPendiente.toLocaleString('es-CL')}
              </span>
            </div>

            {datosReserva.saldoPendiente > 0 && (
              <div className="bg-amber-50/80 border border-amber-200/90 rounded-2xl p-4 text-xs text-amber-900 space-y-1.5 leading-relaxed">
                <div className="flex items-center gap-1.5 font-bold">
                  <Clock size={15} />
                  <span>Plazo: hasta 24 horas antes del {datosReserva.fecha}</span>
                </div>
                <p className="text-[11px]">
                  Si no completas el saldo dentro de ese plazo, la reserva caduca por impago.
                </p>
              </div>
            )}
          </div>

        </div>

        {/* Notificación Correo */}
        <div className="mt-8 bg-forest-50/60 border border-sand-200/90 rounded-2xl p-5 flex items-center gap-3.5 text-xs text-forest-900">
          <div className="w-8 h-8 rounded-full bg-forest-100 flex items-center justify-center shrink-0">
            <Mail size={16} className="text-forest-800" />
          </div>
          <div>
            <span className="font-semibold block">Confirmación enviada a camila@ejemplo.cl</span>
            <span className="text-gray-500 text-[11px]">
              Recibirás el desglose del espacio {datosReserva.nombreEspacio}, fecha y el saldo restante.
            </span>
          </div>
        </div>

        {/* Botones de acción hacia /mis-reservas */}
        <div className="mt-8 flex flex-wrap items-center gap-4">
          <Link 
            to="/mis-reservas" 
            state={datosReserva}
            className="bg-forest-800 text-white text-xs font-semibold px-6 py-3.5 rounded-full hover:bg-forest-900 transition-colors shadow-sm inline-flex items-center gap-2"
          >
            Ver mis reservas
            <ArrowRight size={14} />
          </Link>
          <Link 
            to="/" 
            className="border border-sand-200 text-gray-700 bg-white text-xs font-semibold px-6 py-3.5 rounded-full hover:bg-sand-50 transition-colors"
          >
            Volver al inicio
          </Link>
        </div>

      </main>

      <footer className="bg-forest-900 text-white py-10 border-t border-forest-800 mt-16 text-xs text-gray-400">
        <div className="max-w-6xl mx-auto px-6 flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-2 text-white font-serif font-bold text-base">
            <span>🍃</span> Altos de carpay
          </div>
          <div className="flex gap-6">
            <a href="#" className="hover:underline">Condiciones de reserva</a>
            <a href="#" className="hover:underline">Privacidad</a>
            <a href="#" className="hover:underline">Contacto</a>
          </div>
        </div>
      </footer>

    </div>
  );
}
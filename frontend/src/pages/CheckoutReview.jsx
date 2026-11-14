import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Check, Lock, ExternalLink } from 'lucide-react';

export default function CheckoutReview() {
  const navigate = useNavigate();
  const location = useLocation();

  const datosReserva = location.state;
  const [tipoPago, setTipoPago] = useState('Anticipo');
  const [aceptaTerminos, setAceptaTerminos] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!datosReserva?.id_reserva) navigate('/reservar/espacio', { replace: true });
  }, [datosReserva, navigate]);

  if (!datosReserva?.id_reserva) return null;

  const precioTotal = Number(datosReserva.monto_total);
  const montoAnticipo = Number(datosReserva.monto_anticipo);
  const esAnticipo = tipoPago === 'Anticipo';
  const saldoPendiente = esAnticipo ? precioTotal - montoAnticipo : 0;
  const montoAPagarHoy = esAnticipo ? montoAnticipo : precioTotal;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!aceptaTerminos) {
      setError('Debes aceptar las condiciones de arriendo y plazos de pago para continuar.');
      return;
    }
    setError('');
    navigate('/simulador/tarjeta', {
      state: {
        ...datosReserva,
        montoAPagarHoy,
        saldoPendiente,
        tipoPago
      }
    });
  };

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased selection:bg-forest-100 selection:text-forest-900">
      
      {/* Header con enlace corregido a /mis-reservas */}
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
            <span className="hidden sm:inline">Espacios</span>
            <span className="hidden sm:inline">Cómo reservar</span>
            <span className="hidden sm:inline">Contacto</span>
            <span className="bg-sand-100 text-forest-900 px-3 py-1.5 rounded-full font-semibold">
              Mi cuenta · {datosReserva.cliente}
            </span>
            <Link to="/mis-reservas" state={datosReserva} className="text-forest-800 hover:underline">
              Mis reservas
            </Link>
          </div>
        </div>
      </header>

      {/* Stepper (Paso 3 Activo) */}
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

          <div className="flex items-center gap-2 text-forest-900 font-bold">
            <span className="w-5 h-5 rounded-full bg-forest-800 text-white flex items-center justify-center text-[10px]">
              3
            </span>
            <span>Revisión y pago</span>
          </div>

          <div className="w-8 h-px bg-gray-200"></div>

          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-gray-200 text-gray-600 flex items-center justify-center text-[10px]">
              4
            </span>
            <span className="hidden sm:inline">Confirmación</span>
          </div>
        </div>
      </div>

      {/* Contenido */}
      <main className="max-w-6xl mx-auto px-6 py-10">
        
        <div className="mb-8">
          <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mb-2">
            Todo listo para tu encuentro
          </h1>
          <p className="text-xs text-gray-500">
            Revisa los detalles de la reserva guardada y elige cuánto pagar hoy.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          <div className="lg:col-span-7 space-y-6">
            
            {/* Tarjeta: Tu encuentro */}
            <div className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm">
              <div className="flex justify-between items-center mb-6">
                <h2 className="font-serif text-xl font-semibold text-forest-900">Tu encuentro</h2>
                <Link to="/reservar/espacio" className="text-xs text-forest-800 font-medium hover:underline">
                  Editar selección
                </Link>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="text-[11px] uppercase tracking-wider text-gray-400 font-semibold block mb-1">
                    Nombre del evento
                  </label>
                  <input 
                    type="text" 
                    readOnly 
                    value={`Reserva N° ${datosReserva.id_reserva}`}
                    className="w-full text-sm border border-sand-200 rounded-xl px-4 py-2.5 bg-sand-50/40 text-gray-700 cursor-not-allowed font-medium" 
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-[11px] uppercase tracking-wider text-gray-400 font-semibold block mb-1">
                      Espacio seleccionado
                    </label>
                    <input 
                      type="text" 
                      readOnly 
                      value={datosReserva.nombreEspacio} 
                      className="w-full text-sm border border-sand-200 rounded-xl px-4 py-2.5 bg-sand-50/40 text-gray-700 cursor-not-allowed font-medium" 
                    />
                  </div>
                  <div>
                    <label className="text-[11px] uppercase tracking-wider text-gray-400 font-semibold block mb-1">
                      Personas estimadas
                    </label>
                    <input 
                      type="text" 
                      readOnly 
                      value={`${datosReserva.fecha} · ${datosReserva.hora_inicio} · ${datosReserva.horas_reservadas} horas`}
                      className="w-full text-sm border border-sand-200 rounded-xl px-4 py-2.5 bg-sand-50/40 text-gray-700 cursor-not-allowed font-medium" 
                    />
                  </div>
                </div>

                <p className="text-xs text-gray-500 pt-2 flex items-center gap-2">
                  <span>👤</span> {datosReserva.cliente} · {datosReserva.correo_cliente}
                </p>
              </div>
            </div>

            {/* Selector de Pago */}
            <div className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-5">
              <h2 className="font-serif text-xl font-semibold text-forest-900">
                ¿Cuánto quieres pagar hoy?
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                
                {/* 30% Anticipo */}
                <div 
                  onClick={() => setTipoPago('Anticipo')}
                  className={`p-5 rounded-2xl border-2 cursor-pointer transition-all ${
                    esAnticipo
                      ? 'border-forest-800 bg-forest-50/40 shadow-sm' 
                      : 'border-sand-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center gap-2 text-xs font-semibold text-forest-900 mb-2">
                    <input 
                      type="radio" 
                      name="tipoPago" 
                      checked={esAnticipo}
                      onChange={() => setTipoPago('Anticipo')}
                      className="text-forest-800"
                    />
                    <span>Anticipo · 30%</span>
                  </div>
                  <div className="text-2xl font-bold text-forest-900 font-sans">
                    ${montoAnticipo.toLocaleString('es-CL')}
                  </div>
                  <div className="text-xs text-gray-500 mt-1">
                    Saldo pendiente: ${(precioTotal - montoAnticipo).toLocaleString('es-CL')} (70%)
                  </div>
                </div>

                {/* 100% Pago total */}
                <div 
                  onClick={() => setTipoPago('Total')}
                  className={`p-5 rounded-2xl border-2 cursor-pointer transition-all ${
                    !esAnticipo
                      ? 'border-forest-800 bg-forest-50/40 shadow-sm' 
                      : 'border-sand-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center gap-2 text-xs font-semibold text-forest-900 mb-2">
                    <input 
                      type="radio" 
                      name="tipoPago" 
                      checked={!esAnticipo}
                      onChange={() => setTipoPago('Total')}
                      className="text-forest-800"
                    />
                    <span>Pago total · 100%</span>
                  </div>
                  <div className="text-2xl font-bold text-forest-900 font-sans">
                    ${precioTotal.toLocaleString('es-CL')}
                  </div>
                  <div className="text-xs text-gray-500 mt-1">
                    Saldo pendiente: $0
                  </div>
                </div>

              </div>

              {esAnticipo && (
                <div className="p-4 rounded-xl bg-amber-50 border border-amber-200/80 text-amber-900 text-xs flex items-start gap-3 leading-relaxed">
                  <span className="text-base">⏳</span>
                  <div>
                    <span className="font-semibold block mb-0.5">Plazo límite de pago restante:</span>
                    Con anticipo, el saldo de <strong>${(precioTotal - montoAnticipo).toLocaleString('es-CL')}</strong> debe pagarse antes del evento ({datosReserva.fecha}). Si no se completa dentro del plazo configurado, la reserva puede caducar por impago.
                  </div>
                </div>
              )}
            </div>

            {/* Medio de pago */}
            <div className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm">
              <div className="flex justify-between items-center mb-4">
                <h2 className="font-serif text-xl font-semibold text-forest-900">Medio de pago</h2>
                <Lock size={16} className="text-gray-400" />
              </div>

              <div className="p-4 rounded-xl border border-sand-200 bg-sand-50/30 flex items-center gap-3 text-xs text-gray-700">
                <span className="text-base">💳</span>
                <span className="font-medium">Tarjeta de débito o crédito</span>
              </div>
            </div>

          </div>

          {/* COLUMNA DERECHA: Resumen dinámico */}
          <div className="lg:col-span-5 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
            
            <div className="rounded-2xl overflow-hidden h-44 w-full bg-sand-100 border border-sand-200">
              <img 
                src={datosReserva.img} 
                alt={datosReserva.nombreEspacio} 
                className="w-full h-full object-cover block"
              />
            </div>

            <div>
              <h3 className="font-serif text-2xl font-semibold text-forest-900">
                {datosReserva.nombreEspacio}
              </h3>
              <p className="text-xs text-gray-500 mt-1">
                {datosReserva.fecha} · {datosReserva.hora_inicio} · {datosReserva.horas_reservadas} horas
              </p>
              <p className="text-xs text-gray-500">
                {datosReserva.cliente}
              </p>
            </div>

            {/* Desglose */}
            <div className="space-y-2 text-xs">
              <div className="flex justify-between text-gray-600">
                <span>Arriendo · {datosReserva.horas_reservadas} horas</span>
                <span>${precioTotal.toLocaleString('es-CL')}</span>
              </div>
              <div className="flex justify-between text-gray-600">
                <span>Extras seleccionados</span>
                <span>$0</span>
              </div>
              <div className="flex justify-between text-sm font-bold text-forest-900 pt-2 border-t border-sand-100">
                <span>Total CLP</span>
                <span>${precioTotal.toLocaleString('es-CL')}</span>
              </div>
            </div>

            {/* Monto seleccionado */}
            <div className="bg-sand-50 p-4 rounded-2xl border border-sand-200/80 flex justify-between items-center">
              <div>
                <span className="text-xs text-gray-600 block">A pagar hoy · {esAnticipo ? 'anticipo' : 'total'}</span>
                <span className="text-xs text-gray-400">Saldo posterior: ${saldoPendiente.toLocaleString('es-CL')}</span>
              </div>
              <div className="text-right">
                <span className="text-2xl font-serif font-bold text-forest-900">
                  ${montoAPagarHoy.toLocaleString('es-CL')}
                </span>
                <span className="text-xs text-gray-500 block">
                  ${saldoPendiente.toLocaleString('es-CL')} restante
                </span>
              </div>
            </div>

            {/* Términos y botón */}
            <div className="space-y-2 pt-2">
              {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input 
                  type="checkbox" 
                  checked={aceptaTerminos}
                  onChange={(e) => setAceptaTerminos(e.target.checked)}
                  className="mt-1 rounded text-forest-800 focus:ring-forest-700" 
                />
                <span className="text-xs text-gray-600 leading-snug">
                  Acepto las condiciones de uso y cancelación, las áreas y el horario de mi reserva, y el plazo de pago del saldo.
                </span>
              </label>

              <a href="#" className="text-xs text-forest-800 font-semibold inline-flex items-center gap-1 hover:underline ml-6">
                Leer condiciones de reserva <ExternalLink size={12} />
              </a>
            </div>

            <button 
              type="button"
              onClick={handleSubmit}
              className="w-full bg-forest-800 text-white font-medium text-sm py-4 rounded-xl hover:bg-forest-900 transition-colors shadow-sm"
            >
              Pagar {esAnticipo ? 'anticipo' : 'total'} · ${montoAPagarHoy.toLocaleString('es-CL')}
            </button>

          </div>

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
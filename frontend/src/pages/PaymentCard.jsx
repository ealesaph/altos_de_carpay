import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { CreditCard, Check, ArrowRight } from 'lucide-react';

export default function PaymentCard() {
  const navigate = useNavigate();
  const location = useLocation();

  const datosReserva = location.state;
  const [tipoTarjeta, setTipoTarjeta] = useState('debito');

  useEffect(() => {
    if (!datosReserva?.id_reserva) navigate('/reservar/espacio', { replace: true });
  }, [datosReserva, navigate]);

  if (!datosReserva?.id_reserva) return null;

  const handleContinuar = (e) => {
    e.preventDefault();
    navigate('/simulador/autorizacion', {
      state: datosReserva
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
              Reserva N° {datosReserva.id_reserva}
            </span>
            <Link to="/mis-reservas" state={datosReserva} className="text-forest-800 hover:underline">
              Mis reservas
            </Link>
          </div>
        </div>
      </header>

      {/* Stepper superior */}
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

      {/* Contenido Principal */}
      <main className="max-w-6xl mx-auto px-6 py-10">
        <div className="mb-6">
          <span className="text-xs uppercase font-semibold tracking-wider text-forest-700 block mb-1">
            Simulador de pago · Altos de carpay
          </span>
          <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mb-2">
            Pago simulado · Datos de prueba
          </h1>
          <p className="text-xs text-gray-500">
            Explora cómo sería el pago de tu {datosReserva.tipoPago === 'Anticipo' ? 'anticipo' : 'arriendo'}, sin usar información financiera real.
          </p>
        </div>

        <div className="bg-amber-50/70 border border-amber-200/90 rounded-2xl p-4 text-xs text-amber-900 mb-8 leading-relaxed">
          <span className="font-semibold block mb-0.5">ⓘ Maqueta académica · Sin conexión a Transbank · No realiza cobros</span>
          Datos, pagos y autorizaciones ficticios. No se almacena información financiera ni se envían correos bancarios.
        </div>
        {datosReserva.rechazoSimulado && (
          <div role="status" className="bg-red-50 border border-red-200 rounded-2xl p-4 text-sm text-red-800 mb-6">
            El pago fue rechazado y quedó registrado como tal. Puedes volver a intentar el pago de esta reserva.
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Formulario */}
          <div className="lg:col-span-7 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
            <div className="flex justify-between items-center pb-4 border-b border-sand-100">
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span className="font-semibold uppercase tracking-wider text-gray-400">Dentro del paso 3</span>
                <span className="bg-forest-800 text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px]">A</span>
                <span className="font-bold text-forest-900">Datos de prueba</span>
              </div>
              <div className="flex items-center gap-2 text-xs text-gray-400">
                <span className="border border-gray-300 rounded-full w-4 h-4 flex items-center justify-center text-[10px]">B</span>
                <span>Autorización</span>
              </div>
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <h2 className="font-serif text-xl font-semibold text-forest-900">
                  Usa una tarjeta de prueba
                </h2>
                <span className="text-[10px] bg-sand-100 text-gray-600 px-2 py-0.5 rounded-full font-medium">
                  Solo demostración
                </span>
              </div>
              <p className="text-xs text-gray-500">
                Los datos ya están precompletados y son ficticios. No ingreses tarjetas reales ni claves bancarias.
              </p>
            </div>

            <form onSubmit={handleContinuar} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-2">
                  Elige el tipo de tarjeta
                </label>
                <div className="grid grid-cols-2 gap-4">
                  <label className={`flex items-center gap-2.5 p-3 rounded-xl border-2 cursor-pointer transition-all ${
                    tipoTarjeta === 'debito' ? 'border-forest-800 bg-forest-50/40 font-semibold text-forest-900' : 'border-sand-200 text-gray-600'
                  }`}>
                    <input 
                      type="radio" 
                      name="tarjeta" 
                      checked={tipoTarjeta === 'debito'} 
                      onChange={() => setTipoTarjeta('debito')}
                      className="text-forest-800" 
                    />
                    <span className="text-xs">Débito</span>
                  </label>

                  <label className={`flex items-center gap-2.5 p-3 rounded-xl border-2 cursor-pointer transition-all ${
                    tipoTarjeta === 'credito' ? 'border-forest-800 bg-forest-50/40 font-semibold text-forest-900' : 'border-sand-200 text-gray-600'
                  }`}>
                    <input 
                      type="radio" 
                      name="tarjeta" 
                      checked={tipoTarjeta === 'credito'} 
                      onChange={() => setTipoTarjeta('credito')}
                      className="text-forest-800" 
                    />
                    <span className="text-xs">Crédito</span>
                  </label>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">
                  Número de tarjeta de prueba
                </label>
                <div className="relative">
                  <input 
                    type="text" 
                    readOnly 
                    value="•••• •••• •••• 0000" 
                    className="w-full text-sm border border-sand-200 rounded-xl px-4 py-3 bg-sand-50/40 text-gray-700 cursor-not-allowed font-mono tracking-wider" 
                  />
                  <CreditCard size={18} className="absolute right-3.5 top-3.5 text-gray-400" />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">
                  Titular de demostración
                </label>
                <input 
                  type="text" 
                  readOnly 
                  value="PERSONA DE PRUEBA" 
                  className="w-full text-sm border border-sand-200 rounded-xl px-4 py-3 bg-sand-50/40 text-gray-700 cursor-not-allowed font-mono tracking-wider" 
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1">
                    Vencimiento ficticio
                  </label>
                  <input 
                    type="text" 
                    readOnly 
                    value="MM/AA" 
                    className="w-full text-sm border border-sand-200 rounded-xl px-4 py-3 bg-sand-50/40 text-gray-500 cursor-not-allowed text-center font-mono" 
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1">
                    Código de prueba
                  </label>
                  <input 
                    type="text" 
                    readOnly 
                    value="•••" 
                    className="w-full text-sm border border-sand-200 rounded-xl px-4 py-3 bg-sand-50/40 text-gray-500 cursor-not-allowed text-center font-mono" 
                  />
                </div>
              </div>

              <p className="text-[11px] text-gray-400 pt-1 flex items-center gap-1.5">
                <span>🛡️</span> Valores enmascarados de muestra; no representan una tarjeta válida.
              </p>

              <button 
                type="submit" 
                className="w-full bg-forest-800 text-white font-medium text-sm py-4 rounded-xl hover:bg-forest-900 transition-colors shadow-sm flex items-center justify-center gap-2 mt-4"
              >
                Continuar con el pago simulado
                <ArrowRight size={16} />
              </button>

              <div className="text-center pt-2">
                <Link to="/reservar/pago" state={datosReserva} className="text-xs text-gray-500 hover:text-forest-900">
                  Cancelar y volver al paso 3
                </Link>
              </div>
            </form>

            <div className="pt-4 border-t border-sand-100 text-[11px] text-gray-400">
              → A continuación: autorización ficticia en Banco de demostración. La reserva aún no está confirmada.
            </div>
          </div>

          {/* Resumen Lateral */}
          <div className="lg:col-span-5 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
            <div className="flex justify-between items-center text-xs text-gray-400 pb-2 border-b border-sand-100">
              <span>Altos de carpay</span>
              <span className="font-mono">RES-{datosReserva.id_reserva}</span>
            </div>

            <div className="rounded-2xl overflow-hidden h-36 w-full bg-sand-100 border border-sand-200">
              <img 
                src={datosReserva.img} 
                alt={datosReserva.nombreEspacio} 
                className="w-full h-full object-cover block"
              />
            </div>

            <div>
              <h3 className="font-serif text-xl font-semibold text-forest-900">
                Reserva N° {datosReserva.id_reserva}
              </h3>
              <p className="text-xs text-gray-700 font-medium mt-0.5">{datosReserva.nombreEspacio} · La Reina, Santiago</p>
              <p className="text-xs text-gray-500">{datosReserva.fecha} · {datosReserva.hora_inicio} · {datosReserva.horas_reservadas} horas</p>
              <p className="text-[11px] text-gray-400 mt-1">{datosReserva.cliente} · {datosReserva.correo_cliente}</p>
            </div>

            <div className="bg-sand-50 p-4 rounded-2xl border border-sand-200/80">
              <span className="text-[11px] text-gray-500 block uppercase tracking-wider font-semibold">
                Monto a simular · {datosReserva.tipoPago === 'Anticipo' ? 'anticipo' : 'pago total'}
              </span>
              <div className="text-2xl font-serif font-bold text-forest-900 mt-0.5">
                ${Number(datosReserva.montoAPagarHoy || 0).toLocaleString('es-CL')} <span className="text-xs font-sans font-normal text-gray-500">CLP</span>
              </div>
              <span className="text-xs text-gray-500 block mt-0.5">
                Saldo posterior: ${Number(datosReserva.saldoPendiente || 0).toLocaleString('es-CL')}
              </span>
            </div>

            <div className="space-y-1.5 text-xs text-gray-500 border-t border-sand-100 pt-3">
              <div className="flex justify-between">
                <span>Total arriendo · 5 horas</span>
                <span>${Number(datosReserva.monto_total || 0).toLocaleString('es-CL')}</span>
              </div>
              <div className="flex justify-between">
                <span>Extras seleccionados</span>
                <span>$0</span>
              </div>
            </div>

            {datosReserva.tipoPago === 'Anticipo' && (
              <div className="bg-amber-50/60 p-3.5 rounded-xl border border-amber-200 text-amber-900 text-xs space-y-1 leading-relaxed">
                <span className="font-semibold block">Saldo pendiente: ${Number(datosReserva.saldoPendiente || 0).toLocaleString('es-CL')}</span>
                <span>Debe cancelarse hasta 24 horas antes del evento ({datosReserva.fecha}). La reserva caduca por impago si no se completa el saldo.</span>
              </div>
            )}
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
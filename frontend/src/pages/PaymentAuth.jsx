import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Check, ArrowRight, XCircle, CheckCircle } from 'lucide-react';
import { createPayment, getReservation } from '../api.js';

export default function PaymentAuth() {
  const navigate = useNavigate();
  const location = useLocation();
  const datosReserva = location.state;
  const [error, setError] = useState('');
  const [procesando, setProcesando] = useState(false);

  useEffect(() => {
    if (!datosReserva?.id_reserva) navigate('/reservar/espacio', { replace: true });
  }, [datosReserva, navigate]);

  if (!datosReserva?.id_reserva) return null;

  const registrarPago = async (simularRechazo) => {
    setError('');
    setProcesando(true);
    try {
      const pago = await createPayment(datosReserva.id_reserva, {
        tipo_pago: datosReserva.tipoPago,
        metodo_pago: 'Webpay',
        simular_rechazo: simularRechazo,
      });
      if (simularRechazo) {
        navigate('/simulador/tarjeta', {
          state: { ...datosReserva, rechazoSimulado: true },
        });
        return;
      }

      const reserva = await getReservation(datosReserva.id_reserva);
      navigate('/reservar/confirmacion', {
        state: {
          ...datosReserva,
          ...reserva,
          precioTotal: Number(reserva.monto_total),
          montoAPagarHoy: Number(pago.monto),
          saldoPendiente: Number(reserva.saldo_pendiente),
          nombreEvento: `Reserva N° ${reserva.id_reserva}`,
          nombreEspacio: reserva.sectores.map((sector) => sector.nombre).join(', '),
          bloque: `${reserva.hora_inicio} · ${reserva.horas_reservadas} horas`,
        },
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setProcesando(false);
    }
  };

  const conceptoPago = datosReserva.tipoPago === 'Anticipo'
    ? 'Anticipo'
    : datosReserva.tipoPago === 'Saldo'
      ? 'Pago de saldo'
      : 'Pago total';

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
            <span className="bg-sand-100 text-forest-900 px-3 py-1.5 rounded-full font-semibold">
              Reserva N° {datosReserva.id_reserva}
            </span>
            <Link to="/mis-reservas" state={datosReserva} className="text-forest-800 hover:underline">
              Mis reservas
            </Link>
          </div>
        </div>
      </header>

      {/* Stepper */}
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
            Pago simulado · Autorización
          </h1>
          <p className="text-xs text-gray-500">
            Revisa la operación y confirma el monto correspondiente.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-7 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
            <div className="bg-sand-50/80 p-5 rounded-2xl border border-sand-200 flex justify-between items-center">
              <div>
                <div className="flex items-center gap-2 text-forest-900 font-bold text-sm">
                  <span>🏛️</span> Banco de demostración
                </div>
                <span className="text-[11px] text-gray-400 block mt-0.5">Entidad ficticia · sin conexión bancaria</span>
              </div>
              <div className="text-right">
                <span className="text-xs text-gray-400 block uppercase font-semibold">Total a autorizar</span>
                <span className="text-xl font-bold text-forest-900">${Number(datosReserva.montoAPagarHoy || 0).toLocaleString('es-CL')} CLP</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs bg-sand-50/30 p-4 rounded-xl border border-sand-100">
              <div>
                <span className="text-gray-400 block mb-0.5">Concepto</span>
                <span className="font-semibold text-gray-700">{conceptoPago}</span>
                <span className="text-[11px] text-gray-400 block mt-1">Ref: RES-{datosReserva.id_reserva}</span>
              </div>
              <div className="text-right">
                <span className="text-gray-400 block mb-0.5">Tarjeta de prueba</span>
                <span className="font-mono text-gray-700 font-medium">•••• •••• •••• 0000</span>
                <span className="text-[11px] text-gray-400 block mt-1">Operación ficticia</span>
              </div>
            </div>

            <div className="space-y-4 pt-2">
              <h2 className="font-serif text-lg font-semibold text-forest-900">
                Una validación, solo de ejemplo
              </h2>

              <div className="space-y-3">
                <div className="p-3.5 rounded-xl border border-green-200 bg-green-50/50 text-xs text-green-900 flex items-start gap-2.5">
                  <CheckCircle size={16} className="text-green-700 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold block">Si apruebas → paso 4 · Confirmación</span>
                    <span>Se registrará un pago de ${Number(datosReserva.montoAPagarHoy || 0).toLocaleString('es-CL')} y quedará un saldo de ${Number(datosReserva.saldoPendiente || 0).toLocaleString('es-CL')}.</span>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl border border-red-200 bg-red-50/50 text-xs text-red-900 flex items-start gap-2.5">
                  <XCircle size={16} className="text-red-700 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold block">Si rechazas → el pago queda rechazado</span>
                    <span>La reserva seguirá pendiente y no se registrará un abono.</span>
                  </div>
                </div>
              </div>
            </div>

            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4">
              <button 
                type="button"
                onClick={() => registrarPago(false)}
                disabled={procesando}
                className="bg-forest-800 text-white font-medium text-sm py-3.5 px-4 rounded-xl hover:bg-forest-900 transition-colors shadow-sm flex items-center justify-center gap-2"
              >
                {procesando ? 'Registrando pago…' : 'Simular aprobación del pago'}
                <ArrowRight size={15} />
              </button>

              <button 
                type="button"
                onClick={() => registrarPago(true)}
                disabled={procesando}
                className="border border-sand-200 text-gray-700 bg-white hover:bg-sand-50 font-medium text-sm py-3.5 px-4 rounded-xl transition-colors"
              >
                Rechazar simulación
              </button>
            </div>
          </div>

          {/* Resumen Lateral */}
          <div className="lg:col-span-5 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
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
              <p className="text-xs text-gray-700 font-medium mt-0.5">{datosReserva.nombreEspacio}</p>
              <p className="text-xs text-gray-500">{datosReserva.fecha} · {datosReserva.hora_inicio} · {datosReserva.horas_reservadas} horas</p>
            </div>

            <div className="bg-sand-50 p-4 rounded-2xl border border-sand-200/80">
              <span className="text-[11px] text-gray-500 block uppercase tracking-wider font-semibold">
                Monto a autorizar
              </span>
              <div className="text-2xl font-serif font-bold text-forest-900 mt-0.5">
                ${Number(datosReserva.montoAPagarHoy || 0).toLocaleString('es-CL')} <span className="text-xs font-sans font-normal text-gray-500">CLP</span>
              </div>
              <span className="text-xs text-gray-500 block mt-0.5">
                Saldo restante: ${Number(datosReserva.saldoPendiente || 0).toLocaleString('es-CL')}
              </span>
            </div>
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
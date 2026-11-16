import React, { useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Check, Clock } from 'lucide-react';

export default function Confirmation() {
  const location = useLocation();
  const navigate = useNavigate();
  const reserva = location.state;

  useEffect(() => {
    if (!reserva?.id_reserva) navigate('/mis-reservas', { replace: true });
  }, [reserva, navigate]);

  if (!reserva?.id_reserva) return null;

  const saldoPendiente = Number(reserva.saldoPendiente);
  const precioTotal = Number(reserva.monto_total);
  const montoPagado = Number(reserva.montoAPagarHoy);
  const espacios = reserva.sectores.map((sector) => sector.nombre).join(', ');

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans">
      <header className="bg-white border-b border-sand-200">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="font-serif text-xl font-bold text-forest-900">Altos de carpay</Link>
          <Link to="/mis-reservas" className="text-sm text-forest-800 hover:underline">Mis reservas</Link>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="flex items-start gap-4 mb-8">
          <div className="w-12 h-12 rounded-full bg-forest-100 flex items-center justify-center text-forest-800">
            <Check size={24} />
          </div>
          <div>
            <h1 className="font-serif text-3xl text-forest-900">Pago registrado</h1>
            <p className="text-sm text-gray-600 mt-1">
              Reserva N° {reserva.id_reserva} · {reserva.estado}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <section className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200 shadow-sm space-y-4">
            <h2 className="font-serif text-2xl font-semibold text-forest-900">{espacios}</h2>
            <p className="text-sm text-gray-600">{reserva.fecha} · {reserva.hora_inicio} · {reserva.horas_reservadas} horas</p>
            <p className="text-sm text-gray-600">{reserva.cliente} · {reserva.correo_cliente}</p>
            <div className="border-t border-sand-100 pt-4 flex justify-between text-sm">
              <span>Total</span>
              <strong>${precioTotal.toLocaleString('es-CL')}</strong>
            </div>
          </section>

          <section className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200 shadow-sm space-y-5">
            <h2 className="font-serif text-2xl font-semibold text-forest-900">Pago y saldo</h2>
            <div className="flex justify-between text-sm">
              <span>Pago recibido</span>
              <strong>${montoPagado.toLocaleString('es-CL')}</strong>
            </div>
            <div className="flex justify-between text-sm">
              <span>Saldo pendiente</span>
              <strong>${saldoPendiente.toLocaleString('es-CL')}</strong>
            </div>
            {saldoPendiente > 0 && (
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-900 flex gap-2">
                <Clock size={17} className="shrink-0" />
                <span>Revisa la fecha límite de pago en el detalle de tu reserva.</span>
              </div>
            )}
          </section>
        </div>

        <div className="mt-8 flex gap-4">
          <Link to="/mis-reservas" className="bg-forest-800 text-white text-sm font-semibold px-6 py-3 rounded-full">
            Ver mis reservas
          </Link>
          <Link to="/" className="border border-sand-200 bg-white text-sm px-6 py-3 rounded-full">
            Volver al inicio
          </Link>
        </div>
      </main>
    </div>
  );
}

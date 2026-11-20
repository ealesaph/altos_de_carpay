import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Check, Clock, Mail } from 'lucide-react';
import { sendReservationEmail } from '../api';

export default function Confirmation() {
  const location = useLocation();
  const navigate = useNavigate();
  const reserva = location.state;
  const [emailStatus, setEmailStatus] = useState({ sending: false, message: '', error: '' });

  useEffect(() => {
    if (!reserva?.id_reserva) navigate('/mis-reservas', { replace: true });
  }, [reserva, navigate]);

  if (!reserva?.id_reserva) return null;

  const handleResendEmail = async () => {
    setEmailStatus({ sending: true, message: '', error: '' });
    try {
      const response = await sendReservationEmail(reserva.id_reserva);
      setEmailStatus({
        sending: false,
        message: response?.detail || `Detalles enviados a ${reserva.correo_cliente}.`,
        error: '',
      });
    } catch (err) {
      setEmailStatus({
        sending: false,
        message: '',
        error: err.message || 'No se pudo reenviar el correo.',
      });
    }
  };

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

        <div className="mt-6 bg-white p-5 rounded-2xl border border-sand-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-start gap-3 text-sm text-gray-700">
            <Mail size={18} className="text-forest-800 shrink-0 mt-0.5" />
            <div>
              <p className="font-medium text-forest-900">
                Comprobante y detalles enviados a {reserva.correo_cliente}
              </p>
              {emailStatus.message && (
                <p className="text-xs text-forest-800 mt-1">{emailStatus.message}</p>
              )}
              {emailStatus.error && (
                <p className="text-xs text-red-700 mt-1">{emailStatus.error}</p>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={handleResendEmail}
            disabled={emailStatus.sending}
            className="border border-forest-800 text-forest-800 hover:bg-forest-50 disabled:opacity-50 text-sm font-medium px-4 py-2 rounded-full transition-colors shrink-0"
          >
            {emailStatus.sending ? 'Enviando...' : 'Reenviar detalles al correo'}
          </button>
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

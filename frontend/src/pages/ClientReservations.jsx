import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Calendar, Clock, AlertCircle, LogOut, Mail } from 'lucide-react';
import { cancelReservation, getReservations, hasSession, logout, previewCancellation, sendReservationEmail } from '../api.js';
import defaultSpaceImg from '../assets/default-space.svg';
import { useConfirmacion } from '../components/ConfirmDialog.jsx';
import Footer from '../components/Footer.jsx';
import { formatoCLP, formatoFechaHora } from '../reglas.js';

export default function ClientReservations() {
  const navigate = useNavigate();
  const [reservas, setReservas] = useState([]);
  const [error, setError] = useState('');
  const [mensajeCorreo, setMensajeCorreo] = useState('');
  const [enviandoCorreoId, setEnviandoCorreoId] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [dialogo, confirmar] = useConfirmacion();

  useEffect(() => {
    if (!hasSession()) {
      navigate('/login', { replace: true });
      return;
    }

    let activo = true;
    getReservations()
      .then((reservasApi) => {
        if (activo) setReservas(reservasApi);
      })
      .catch((requestError) => {
        if (activo) setError(requestError.message);
      })
      .finally(() => {
        if (activo) setCargando(false);
      });

    return () => {
      activo = false;
    };
  }, [navigate]);

  const salir = async () => {
    try {
      await logout();
      navigate('/');
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const cancelar = async (id) => {
    setError('');
    setMensajeCorreo('');
    try {
      const simulacion = await previewCancellation(id);
      const acepta = await confirmar({
        titulo: `¿Cancelar la reserva N° ${id}?`,
        mensaje: (
          <>
            <p>Esta acción no se puede deshacer y la fecha quedará disponible para otras personas.</p>
            <p>
              Pagado: <strong>{formatoCLP(simulacion.monto_pagado)}</strong> · Devolución:{' '}
              <strong>{formatoCLP(simulacion.monto_devolucion)}</strong>
            </p>
            <p className="text-xs text-gray-500">{simulacion.regla}</p>
          </>
        ),
        textoConfirmar: 'Sí, cancelar reserva',
        textoCancelar: 'No, mantenerla',
        peligro: true,
      });
      if (!acepta) return;
      await cancelReservation(id);
      setReservas(await getReservations());
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const reenviarCorreo = async (id) => {
    setError('');
    setMensajeCorreo('');
    setEnviandoCorreoId(id);
    try {
      const resp = await sendReservationEmail(id);
      setMensajeCorreo(resp?.detail || `Detalles de la reserva N° ${id} enviados a tu correo.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setEnviandoCorreoId(null);
    }
  };

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans">
      {dialogo}
      <header className="bg-white border-b border-sand-200">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="font-serif text-xl font-bold text-forest-900">Altos de carpay</Link>
          <div className="flex items-center gap-4">
            <Link to="/mi-cuenta#mis-consultas" className="text-sm font-medium text-forest-800 hover:underline">
              Mis consultas
            </Link>
            <Link to="/mi-cuenta" className="text-sm font-medium text-forest-800 hover:underline">
              Mi cuenta
            </Link>
            <button type="button" onClick={salir} className="text-sm text-gray-600 flex items-center gap-2">
              <LogOut size={15} /> Salir
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10 space-y-6">
        <div className="flex flex-col sm:flex-row justify-between sm:items-end gap-4 border-b border-sand-200 pb-6">
          <div>
            <span className="text-xs uppercase font-semibold tracking-wider text-forest-700">Área de clientes</span>
            <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mt-1">Mis reservas</h1>
            <p className="text-sm text-gray-500 mt-1">Gestiona tus encuentros, pagos pendientes y comprobantes.</p>
          </div>
          <Link to="/reservar/espacio" className="bg-forest-800 text-white text-sm font-semibold px-5 py-2.5 rounded-full">
            + Nueva reserva
          </Link>
        </div>

        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        {mensajeCorreo && <p role="status" className="text-sm text-forest-800 bg-forest-50 border border-forest-200 px-4 py-2.5 rounded-xl">{mensajeCorreo}</p>}
        {cargando && <p className="text-sm text-gray-500">Cargando reservas…</p>}
        {!cargando && !error && reservas.length === 0 && (
          <div className="bg-white p-8 rounded-3xl border border-sand-200 text-sm text-gray-600">
            Todavía no tienes reservas.
          </div>
        )}

        {reservas.map((reserva) => {
          const saldo = Number(reserva.saldo_pendiente);
          const sectores = reserva.sectores.map((sector) => sector.nombre).join(', ');
          return (
            <article key={reserva.id_reserva} className="bg-white rounded-3xl border border-sand-200 shadow-sm p-6 md:p-8">
              <div className="flex flex-wrap justify-between items-start gap-3">
                <div>
                  <span className="text-xs text-gray-400">Reserva N° {reserva.id_reserva}</span>
                  <h2 className="font-serif text-2xl font-bold text-forest-900 mt-1">{sectores}</h2>
                </div>
                <span className="text-xs bg-sand-100 px-3 py-1 rounded-full font-semibold">{reserva.estado}</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 text-sm text-gray-600">
                <span><Calendar size={15} className="inline mr-2" />{reserva.fecha}</span>
                <span><Clock size={15} className="inline mr-2" />{reserva.hora_inicio ? `${String(reserva.hora_inicio).slice(0, 5)} · ${reserva.horas_reservadas} horas` : 'Horario no registrado'}</span>
                <span>Total: ${Number(reserva.monto_total).toLocaleString('es-CL')}</span>
              </div>

              <div className="border-t border-sand-100 mt-5 pt-4 flex flex-wrap items-center justify-between gap-4">
                <div className="text-sm">
                  <span className="text-gray-500">Pagado: ${Number(reserva.monto_pagado).toLocaleString('es-CL')}</span>
                  <strong className="block text-forest-900 mt-1">Saldo: ${saldo.toLocaleString('es-CL')}</strong>
                </div>
                <div className="flex flex-wrap gap-3">
                  {!['Cancelado', 'No se presentó'].includes(reserva.estado) && (
                    <button
                      type="button"
                      disabled={enviandoCorreoId === reserva.id_reserva}
                      onClick={() => reenviarCorreo(reserva.id_reserva)}
                      className="border border-sand-200 text-forest-900 hover:bg-sand-50 text-sm px-4 py-2 rounded-xl inline-flex items-center gap-1.5"
                    >
                      <Mail size={14} />
                      {enviandoCorreoId === reserva.id_reserva ? 'Enviando…' : 'Reenviar correo'}
                    </button>
                  )}
                  {saldo > 0 && ['Pendiente', 'Confirmado', 'En Proceso'].includes(reserva.estado) && (
                    <Link
                      to="/simulador/tarjeta"
                      state={{
                        ...reserva,
                        img: defaultSpaceImg,
                        tipoPago: reserva.estado === 'Pendiente' ? 'Anticipo' : 'Saldo',
                        montoAPagarHoy: reserva.estado === 'Pendiente' ? Number(reserva.monto_anticipo) : saldo,
                        saldoPendiente: reserva.estado === 'Pendiente' ? Number(reserva.monto_total) - Number(reserva.monto_anticipo) : 0,
                        precioTotal: Number(reserva.monto_total),
                        nombreEspacio: sectores,
                        nombreEvento: `Reserva N° ${reserva.id_reserva}`,
                      }}
                      className="bg-forest-800 text-white text-sm px-4 py-2 rounded-xl"
                    >
                      {reserva.estado === 'Pendiente' ? 'Pagar anticipo' : 'Pagar saldo'}
                    </Link>
                  )}
                  {reserva.estado === 'Pendiente' && (
                    <button
                      type="button"
                      onClick={() => cancelar(reserva.id_reserva)}
                      className="border border-sand-200 text-red-700 hover:bg-red-50 text-sm px-4 py-2 rounded-xl"
                    >
                      Cancelar
                    </button>
                  )}
                </div>
              </div>

              {reserva.estado === 'Pendiente' && saldo > 0 && (
                <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl p-3 text-xs text-amber-900 flex gap-2">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>Fecha límite del anticipo: {formatoFechaHora(reserva.plazo_anticipo_vence)}. Si no se paga, la fecha se libera automáticamente.</span>
                </div>
              )}
              {reserva.estado === 'Confirmado' && saldo > 0 && (
                <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl p-3 text-xs text-amber-900 flex gap-2">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>Paga el saldo hasta 24 horas antes del evento: {formatoFechaHora(reserva.plazo_saldo_vence)}.</span>
                </div>
              )}
              {reserva.estado === 'Cancelado' && Number(reserva.monto_devolucion) > 0 && (
                <div className="mt-4 bg-sand-50 border border-sand-200 rounded-xl p-3 text-xs text-gray-700">
                  Devolución: {formatoCLP(reserva.monto_devolucion)} ·{' '}
                  {reserva.devolucion_realizada_en
                    ? `realizada el ${formatoFechaHora(reserva.devolucion_realizada_en)}`
                    : 'pendiente; Altos de Carpay te contactará para transferirla.'}
                </div>
              )}
              {reserva.estado === 'No se presentó' && (
                <div className="mt-4 bg-sand-50 border border-sand-200 rounded-xl p-3 text-xs text-gray-700">
                  No se registró tu llegada en el horario reservado; según las condiciones, se conserva el anticipo.
                </div>
              )}
            </article>
          );
        })}
      </main>
      <Footer />
    </div>
  );
}

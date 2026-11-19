import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Calendar, Clock, AlertCircle, LogOut } from 'lucide-react';
import { cancelReservation, getReservations, hasSession, logout } from '../api.js';

export default function ClientReservations() {
  const navigate = useNavigate();
  const [reservas, setReservas] = useState([]);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);

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
    try {
      await cancelReservation(id);
      setReservas(await getReservations());
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans">
      <header className="bg-white border-b border-sand-200">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="font-serif text-xl font-bold text-forest-900">Altos de carpay</Link>
          <button type="button" onClick={salir} className="text-sm text-gray-600 flex items-center gap-2">
            <LogOut size={15} /> Salir
          </button>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10 space-y-6">
        <div className="flex flex-col sm:flex-row justify-between sm:items-end gap-4 border-b border-sand-200 pb-6">
          <div>
            <span className="text-xs uppercase font-semibold tracking-wider text-forest-700">Área de clientes</span>
            <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mt-1">Mis reservas</h1>
            <p className="text-sm text-gray-500 mt-1">Datos consultados desde la base de datos.</p>
          </div>
          <Link to="/reservar/espacio" className="bg-forest-800 text-white text-sm font-semibold px-5 py-2.5 rounded-full">
            + Nueva reserva
          </Link>
        </div>

        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
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
                <span><Clock size={15} className="inline mr-2" />{reserva.hora_inicio ? `${reserva.hora_inicio} · ${reserva.horas_reservadas} horas` : 'Horario no registrado'}</span>
                <span>Total: ${Number(reserva.monto_total).toLocaleString('es-CL')}</span>
              </div>

              <div className="border-t border-sand-100 mt-5 pt-4 flex flex-wrap items-center justify-between gap-4">
                <div className="text-sm">
                  <span className="text-gray-500">Pagado: ${Number(reserva.monto_pagado).toLocaleString('es-CL')}</span>
                  <strong className="block text-forest-900 mt-1">Saldo: ${saldo.toLocaleString('es-CL')}</strong>
                </div>
                <div className="flex gap-3">
                  {saldo > 0 && reserva.estado !== 'Cancelado' && (
                    <Link
                      to="/simulador/tarjeta"
                      state={{
                        ...reserva,
                        tipoPago: 'Saldo',
                        montoAPagarHoy: saldo,
                        saldoPendiente: 0,
                        precioTotal: Number(reserva.monto_total),
                        nombreEspacio: sectores,
                        nombreEvento: `Reserva N° ${reserva.id_reserva}`,
                      }}
                      className="bg-forest-800 text-white text-sm px-4 py-2 rounded-xl"
                    >
                      Pagar saldo
                    </Link>
                  )}
                  {reserva.estado === 'Pendiente' && (
                    <button
                      type="button"
                      onClick={() => cancelar(reserva.id_reserva)}
                      className="border border-sand-200 text-sm px-4 py-2 rounded-xl"
                    >
                      Cancelar
                    </button>
                  )}
                </div>
              </div>

              {saldo > 0 && reserva.estado !== 'Cancelado' && (
                <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl p-3 text-xs text-amber-900 flex gap-2">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>Fecha límite de anticipo: {new Date(reserva.plazo_anticipo_vence).toLocaleString('es-CL')}</span>
                </div>
              )}
            </article>
          );
        })}
      </main>
    </div>
  );
}

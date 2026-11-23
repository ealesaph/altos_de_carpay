import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Calendar as CalendarIcon, Clock, ArrowRight } from 'lucide-react';
import { createReservation, getSectors, getServices, hasSession, updateReservationSelection } from '../api.js';
import { horaTermino, horasDeInicio, maximoHoras, textoBloques } from '../reglas.js';
import defaultSpaceImg from '../assets/default-space.svg';

function fechaMinima() {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + 1);
  return [
    fecha.getFullYear(),
    String(fecha.getMonth() + 1).padStart(2, '0'),
    String(fecha.getDate()).padStart(2, '0'),
  ].join('-');
}

export default function SelectDateSpace() {
  const navigate = useNavigate();
  const location = useLocation();
  // «Editar selección» llega con la reserva pendiente: se precarga y al guardar se modifica esa misma reserva.
  const reservaEditada = location.state?.editar || null;
  const [sectores, setSectores] = useState([]);
  const [servicios, setServicios] = useState([]);
  const [serviciosSeleccionados, setServiciosSeleccionados] = useState(() =>
    Object.fromEntries((reservaEditada?.servicios || []).map((s) => [s.id_servicio, s.cantidad])),
  );
  const [sectorSeleccionado, setSectorSeleccionado] = useState(
    reservaEditada?.sectores?.[0] ? String(reservaEditada.sectores[0].id_sector) : '',
  );
  const [fecha, setFecha] = useState(reservaEditada?.fecha || fechaMinima());
  const [horaInicio, setHoraInicio] = useState(String(reservaEditada?.hora_inicio || '13:00').slice(0, 5));
  const [horas, setHoras] = useState(Number(reservaEditada?.horas_reservadas || 5));
  const [aceptaCondiciones, setAceptaCondiciones] = useState(Boolean(reservaEditada));
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    Promise.all([getSectors(), getServices().catch(() => [])])
      .then(([catalogo, listaServicios]) => {
        const disponibles = catalogo.filter((sector) => sector.estado === 'Disponible');
        setSectores(disponibles);
        setServicios((listaServicios || []).filter((s) => s.estado === 'Activo'));
        if (disponibles.length) {
          setSectorSeleccionado((actual) => actual || String(disponibles[0].id_sector));
        }
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setCargando(false));
  }, []);

  const sectorActual = sectores.find(
    (sector) => String(sector.id_sector) === sectorSeleccionado,
  );
  const precioArriendo = sectorActual ? Number(sectorActual.valor_hora) * Number(horas) : 0;
  const precioExtras = servicios.reduce((acc, srv) => {
    const cant = Number(serviciosSeleccionados[srv.id_servicio] || 0);
    return acc + (cant > 0 ? Number(srv.valor) * cant : 0);
  }, 0);
  const precioTotal = precioArriendo + precioExtras;
  const horasMaximas = maximoHoras(horaInicio);

  const cambiarHoraInicio = (valor) => {
    setHoraInicio(valor);
    setHoras((actual) => Math.min(Number(actual), maximoHoras(valor)));
  };

  const toggleServicio = (idServicio) => {
    setServiciosSeleccionados((prev) => {
      const actual = Number(prev[idServicio] || 0);
      return { ...prev, [idServicio]: actual > 0 ? 0 : 1 };
    });
  };

  const handleContinuar = async (event) => {
    event.preventDefault();
    setError('');
    if (!hasSession()) {
      navigate('/login', { state: { next: '/reservar/espacio' } });
      return;
    }
    if (!sectorActual) {
      setError('Selecciona un espacio disponible.');
      return;
    }
    const horasNumero = Number(horas);
    if (!Number.isInteger(horasNumero) || horasNumero < 1 || horasNumero > horasMaximas) {
      setError(`Desde las ${horaInicio} puedes reservar entre 1 y ${horasMaximas} horas.`);
      return;
    }
    if (!aceptaCondiciones) {
      setError('Debes aceptar las condiciones de arriendo antes de reservar.');
      return;
    }

    setEnviando(true);
    try {
      const serviciosPayload = Object.entries(serviciosSeleccionados)
        .filter(([, cant]) => Number(cant) > 0)
        .map(([idSrv, cant]) => ({ servicio: Number(idSrv), cantidad: Number(cant) }));

      const datos = {
        fecha,
        hora_inicio: horaInicio,
        sectores: [{ sector: sectorActual.id_sector, horas: Number(horas) }],
        servicios: serviciosPayload,
        acepta_condiciones: aceptaCondiciones,
      };
      const reserva = reservaEditada
        ? await updateReservationSelection(reservaEditada.id_reserva, datos)
        : await createReservation(datos);
      navigate('/reservar/pago', {
        state: {
          ...reserva,
          id_reserva: reserva.id_reserva,
          nombreEspacio: sectorActual.nombre,
          precioTotal: Number(reserva.monto_total),
          precioArriendo,
          precioExtras,
          anticipo: Number(reserva.monto_anticipo),
          img: sectorActual.fotografias?.[0] || defaultSpaceImg,
          bloque: `${reserva.hora_inicio} · ${reserva.horas_reservadas} horas`,
          tipoPago: 'Anticipo',
        },
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased">
      <header className="bg-white border-b border-sand-200/60">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="font-serif text-xl font-bold text-forest-900">Altos de carpay</Link>
          <Link to="/mis-reservas" className="text-sm text-forest-800 hover:underline">Mis reservas</Link>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="mb-8">
          <span className="text-xs uppercase font-semibold tracking-wider text-forest-700">
            Paso 1 · Reserva
          </span>
          <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mt-1">
            {reservaEditada ? `Editar reserva N° ${reservaEditada.id_reserva}` : 'Selecciona tu espacio y fecha'}
          </h1>
          <p className="text-sm text-gray-500 mt-2">
            Horario de arriendo: {textoBloques()}. Se reserva por horas completas.
          </p>
        </div>

        <form onSubmit={handleContinuar} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <section className="lg:col-span-2 bg-white p-6 md:p-8 rounded-3xl border border-sand-200 shadow-sm space-y-6">
            <div>
              <h2 className="font-serif text-xl font-semibold text-forest-900 mb-4">Espacio disponible</h2>
              {cargando && <p className="text-sm text-gray-500">Cargando catálogo…</p>}
              {!cargando && sectores.length === 0 && !error && (
                <p className="text-sm text-gray-500">No hay espacios disponibles para reservar.</p>
              )}
              <div className="grid gap-3">
                {sectores.map((sector) => (
                  <label
                    key={sector.id_sector}
                    className={`flex items-start gap-3 p-4 rounded-2xl border-2 cursor-pointer ${
                      sectorSeleccionado === String(sector.id_sector)
                        ? 'border-forest-800 bg-forest-50/40'
                        : 'border-sand-200'
                    }`}
                  >
                    <input
                      type="radio"
                      name="sector"
                      value={sector.id_sector}
                      checked={sectorSeleccionado === String(sector.id_sector)}
                      onChange={() => setSectorSeleccionado(String(sector.id_sector))}
                      className="mt-1"
                    />
                    <span className="flex-1">
                      <span className="block font-semibold text-forest-900">{sector.nombre}</span>
                      {sector.descripcion && <span className="block text-sm text-gray-500 mt-1">{sector.descripcion}</span>}
                    </span>
                    <span className="text-sm font-semibold text-forest-900 whitespace-nowrap">
                      ${Number(sector.valor_hora).toLocaleString('es-CL')} / hora
                    </span>
                  </label>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-2">
                  <CalendarIcon size={15} className="inline mr-1" /> Fecha
                </label>
                <input
                  type="date"
                  required
                  min={fechaMinima()}
                  value={fecha}
                  onChange={(event) => setFecha(event.target.value)}
                  className="w-full border border-sand-200 rounded-xl p-3"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-2">
                  <Clock size={15} className="inline mr-1" /> Hora de inicio
                </label>
                <select
                  required
                  value={horaInicio}
                  onChange={(event) => cambiarHoraInicio(event.target.value)}
                  className="w-full border border-sand-200 rounded-xl p-3 bg-white"
                >
                  {horasDeInicio().map((opcion) => (
                    <option key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-2">Horas</label>
                <select
                  required
                  value={horas}
                  onChange={(event) => setHoras(Number(event.target.value))}
                  className="w-full border border-sand-200 rounded-xl p-3 bg-white"
                >
                  {Array.from({ length: horasMaximas }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>{n} {n === 1 ? 'hora' : 'horas'}</option>
                  ))}
                </select>
                <span className="text-[11px] text-gray-500 block mt-1">Termina a las {horaTermino(horaInicio, horas)}</span>
              </div>
            </div>
            {servicios.length > 0 && (
              <div className="pt-2 border-t border-sand-100">
                <h2 className="font-serif text-lg font-semibold text-forest-900 mb-3">
                  Servicios adicionales (opcional)
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {servicios.map((srv) => {
                    const activo = Number(serviciosSeleccionados[srv.id_servicio] || 0) > 0;
                    return (
                      <label
                        key={srv.id_servicio}
                        className={`flex items-start justify-between gap-2 p-3 rounded-xl border cursor-pointer text-xs ${
                          activo ? 'border-forest-800 bg-forest-50/40' : 'border-sand-200'
                        }`}
                      >
                        <span className="flex items-start gap-2">
                          <input
                            type="checkbox"
                            checked={activo}
                            onChange={() => toggleServicio(srv.id_servicio)}
                            className="mt-0.5"
                          />
                          <span>
                            <span className="block font-semibold text-forest-900">{srv.nombre}</span>
                            {srv.descripcion && (
                              <span className="block text-[11px] text-gray-500">{srv.descripcion}</span>
                            )}
                          </span>
                        </span>
                        <span className="font-semibold text-forest-900 whitespace-nowrap">
                          +${Number(srv.valor).toLocaleString('es-CL')}
                        </span>
                      </label>
                    );
                  })}
                </div>
              </div>
            )}

            <label className="flex items-start gap-2 text-sm text-gray-600">
              <input
                type="checkbox"
                required
                checked={aceptaCondiciones}
                onChange={(event) => setAceptaCondiciones(event.target.checked)}
                className="mt-1"
              />
              <span>
                Acepto las{' '}
                <Link to="/condiciones" target="_blank" className="text-forest-800 font-semibold underline">
                  condiciones de arriendo y la política de cancelación
                </Link>.
              </span>
            </label>
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
          </section>

          <aside className="bg-white p-6 rounded-3xl border border-sand-200 shadow-sm h-fit space-y-4">
            <h2 className="font-serif text-xl font-semibold text-forest-900">Resumen</h2>
            <p className="text-sm text-gray-600">{sectorActual?.nombre || 'Selecciona un espacio'}</p>
            <p className="text-sm text-gray-600">{fecha} · {horaInicio}–{horaTermino(horaInicio, horas)} · {horas} {Number(horas) === 1 ? 'hora' : 'horas'}</p>
            <div className="border-t border-sand-100 pt-4 flex justify-between font-semibold text-forest-900">
              <span>Total estimado</span>
              <span>${precioTotal.toLocaleString('es-CL')}</span>
            </div>
            <p className="text-xs text-gray-500">
              El precio y anticipo definitivos los calcula el servidor al guardar la reserva.
            </p>
            <button
              type="submit"
              disabled={cargando || enviando || !sectorActual}
              className="w-full bg-forest-800 text-white py-3 rounded-xl disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {enviando ? 'Guardando reserva…' : reservaEditada ? 'Guardar cambios' : 'Continuar a revisión'}
              {!enviando && <ArrowRight size={16} />}
            </button>
          </aside>
        </form>
      </main>
    </div>
  );
}

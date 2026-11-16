import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Calendar as CalendarIcon, Clock, ArrowRight } from 'lucide-react';
import { createReservation, getSectors, hasSession } from '../api.js';

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
  const [sectores, setSectores] = useState([]);
  const [sectorSeleccionado, setSectorSeleccionado] = useState('');
  const [fecha, setFecha] = useState(fechaMinima);
  const [horaInicio, setHoraInicio] = useState('13:00');
  const [horas, setHoras] = useState(5);
  const [aceptaCondiciones, setAceptaCondiciones] = useState(false);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    getSectors()
      .then((catalogo) => {
        const disponibles = catalogo.filter((sector) => sector.estado === 'Disponible');
        setSectores(disponibles);
        if (disponibles.length) setSectorSeleccionado(String(disponibles[0].id_sector));
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setCargando(false));
  }, []);

  const sectorActual = sectores.find(
    (sector) => String(sector.id_sector) === sectorSeleccionado,
  );
  const precioTotal = sectorActual ? Number(sectorActual.valor_hora) * Number(horas) : 0;

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
    if (!aceptaCondiciones) {
      setError('Debes aceptar las condiciones de arriendo antes de reservar.');
      return;
    }

    setEnviando(true);
    try {
      const reserva = await createReservation({
        fecha,
        hora_inicio: horaInicio,
        sectores: [{ sector: sectorActual.id_sector, horas: Number(horas) }],
        servicios: [],
        acepta_condiciones: aceptaCondiciones,
      });
      navigate('/reservar/pago', {
        state: {
          ...reserva,
          id_reserva: reserva.id_reserva,
          nombreEspacio: sectorActual.nombre,
          precioTotal: Number(reserva.monto_total),
          anticipo: Number(reserva.monto_anticipo),
          img: sectorActual.fotografias?.[0] || '',
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
            Selecciona tu espacio y fecha
          </h1>
          <p className="text-sm text-gray-500 mt-2">
            Los espacios y precios se consultan directamente desde el catálogo del sistema.
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
                <input
                  type="time"
                  required
                  value={horaInicio}
                  onChange={(event) => setHoraInicio(event.target.value)}
                  className="w-full border border-sand-200 rounded-xl p-3"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-2">Horas</label>
                <input
                  type="number"
                  min="1"
                  max="24"
                  required
                  value={horas}
                  onChange={(event) => setHoras(event.target.value)}
                  className="w-full border border-sand-200 rounded-xl p-3"
                />
              </div>
            </div>
            <label className="flex items-start gap-2 text-sm text-gray-600">
              <input
                type="checkbox"
                required
                checked={aceptaCondiciones}
                onChange={(event) => setAceptaCondiciones(event.target.checked)}
                className="mt-1"
              />
              Acepto las condiciones de arriendo y la política de cancelación.
            </label>
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
          </section>

          <aside className="bg-white p-6 rounded-3xl border border-sand-200 shadow-sm h-fit space-y-4">
            <h2 className="font-serif text-xl font-semibold text-forest-900">Resumen</h2>
            <p className="text-sm text-gray-600">{sectorActual?.nombre || 'Selecciona un espacio'}</p>
            <p className="text-sm text-gray-600">{fecha} · {horaInicio} · {horas} horas</p>
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
              {enviando ? 'Guardando reserva…' : 'Continuar a revisión'}
              {!enviando && <ArrowRight size={16} />}
            </button>
          </aside>
        </form>
      </main>
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  CalendarDays, 
  Tags, 
  MessageSquare, 
  Settings, 
  ExternalLink, 
  LogOut, 
  Clock, 
  Search, 
  Bell
} from 'lucide-react';
import { getReservations, getSectors, hasSession, logout, updateSector } from '../api.js';

export default function AdminDashboard() {
  const [seccionActiva, setSeccionActiva] = useState('dashboard');
  const [reservas, setReservas] = useState([]);
  const [sectores, setSectores] = useState([]);
  const [sectorSeleccionado, setSectorSeleccionado] = useState('');
  const [tarifa, setTarifa] = useState('');
  const [guardandoSector, setGuardandoSector] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!hasSession()) {
      navigate('/login', { replace: true });
      return;
    }
    Promise.all([getReservations(), getSectors()])
      .then(([reservasApi, sectoresApi]) => {
        setReservas(reservasApi);
        setSectores(sectoresApi);
        if (sectoresApi.length) {
          setSectorSeleccionado(String(sectoresApi[0].id_sector));
          setTarifa(sectoresApi[0].valor_hora);
        }
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setCargando(false));
  }, [navigate]);

  const reservasVisibles = reservas.filter((reserva) => {
    const texto = `${reserva.id_reserva} ${reserva.cliente} ${reserva.correo_cliente}`.toLowerCase();
    return texto.includes(busqueda.toLowerCase());
  });
  const saldoPorCobrar = reservas.reduce(
    (total, reserva) => total + Number(reserva.saldo_pendiente),
    0,
  );
  const sectorActual = sectores.find(
    (sector) => String(sector.id_sector) === sectorSeleccionado,
  );

  const guardarTarifa = async (event) => {
    event.preventDefault();
    if (!sectorActual) return;
    setError('');
    setGuardandoSector(true);
    try {
      const actualizado = await updateSector(sectorActual.id_sector, { valor_hora: tarifa });
      setSectores((actuales) =>
        actuales.map((sector) => sector.id_sector === actualizado.id_sector ? actualizado : sector),
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setGuardandoSector(false);
    }
  };

  const cerrarSesion = async () => {
    try {
      await logout();
      navigate('/');
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  return (
    <div className="min-h-screen bg-sand-50 flex font-sans text-gray-800 antialiased">
      
      {/* 1. SIDEBAR LATERAL (Verde institucional) */}
      <aside className="w-64 bg-forest-900 text-white flex flex-col justify-between p-6 shrink-0 hidden md:flex">
        <div className="space-y-8">
          
          {/* Logo y título */}
          <div>
            <div className="flex items-center gap-2 text-white">
              <span className="text-xl">🍃</span>
              <span className="font-serif text-xl font-bold tracking-tight">Altos de carpay</span>
            </div>
            <span className="text-[10px] uppercase tracking-widest text-forest-100/60 block mt-1 font-semibold">
              ADMINISTRACIÓN
            </span>
          </div>

          {/* Menú de navegación */}
          <nav className="space-y-1 text-xs">
            <button 
              onClick={() => setSeccionActiva('dashboard')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'dashboard' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <LayoutDashboard size={16} />
              <span>Vista general</span>
            </button>

            <button 
              onClick={() => setSeccionActiva('reservas')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'reservas' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <CalendarDays size={16} />
              <span>Reservas</span>
            </button>

            <button 
              onClick={() => setSeccionActiva('catalogo')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'catalogo' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <Tags size={16} />
              <span>Catálogo y precios</span>
            </button>

            <button 
              onClick={() => setSeccionActiva('consultas')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'consultas' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <MessageSquare size={16} />
              <span>Consultas</span>
            </button>

            <button 
              onClick={() => setSeccionActiva('config')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'config' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <Settings size={16} />
              <span>Configuración</span>
            </button>
          </nav>

        </div>

        {/* Footer del Sidebar */}
        <div className="space-y-4 pt-6 border-t border-forest-800 text-xs">
          <Link to="/" className="text-gray-300 hover:text-white flex items-center gap-2">
            <ExternalLink size={14} /> Ver sitio público
          </Link>
          <button type="button" onClick={cerrarSesion} className="text-gray-400 hover:text-red-300 flex items-center gap-2">
            <LogOut size={14} /> Cerrar sesión
          </button>

          <div className="p-3 rounded-xl bg-forest-950/60 border border-forest-800/60 text-[10px] text-gray-400 leading-relaxed">
            Reservas y catálogo conectados con el backend.
          </div>
        </div>
      </aside>

      {/* 2. CONTENIDO PRINCIPAL */}
      <div className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        
        {/* Topbar */}
        <header className="h-16 bg-white border-b border-sand-200/80 px-8 flex items-center justify-between sticky top-0 z-30">
          <div className="text-xs text-gray-500">
            Gestión de Altos de carpay / <span className="text-forest-900 font-semibold">Vista general</span>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <button className="text-gray-400 hover:text-forest-900 relative">
              <Bell size={16} />
              <span className="w-1.5 h-1.5 rounded-full bg-forest-700 absolute top-0 right-0"></span>
            </button>
            <div className="flex items-center gap-2 border-l border-sand-200 pl-4">
              <span className="w-7 h-7 rounded-full bg-forest-100 text-forest-900 font-bold flex items-center justify-center text-xs">
                AC
              </span>
              <span className="font-medium text-gray-700">Equipo administrador</span>
            </div>
          </div>
        </header>

        {/* Cuerpo del Dashboard */}
        <main className="p-8 max-w-6xl w-full mx-auto space-y-8">
          {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
          {cargando && <p className="text-sm text-gray-500">Cargando datos…</p>}
          
          {/* Header de bienvenida y botón de acción */}
          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
            <div>
              <h1 className="font-serif text-3xl font-bold text-forest-900">
                Cada encuentro, en orden.
              </h1>
              <p className="text-xs text-gray-500 mt-1">
                Reservas y catálogo sincronizados con el backend.
              </p>
            </div>

          </div>

          {/* Tarjetas de Métricas (KPIs) */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">{reservas.length}</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Reservas</span>
                <span className="text-[10px] text-gray-400">Desde PostgreSQL</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">${saldoPorCobrar.toLocaleString('es-CL')}</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Saldo por cobrar</span>
              <span className="text-[10px] text-forest-700">{reservas.filter((reserva) => Number(reserva.saldo_pendiente) > 0).length} con saldo</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">—</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Consultas pendientes</span>
              <span className="text-[10px] text-amber-700">API no disponible</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">{sectores.length}</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Espacios publicados</span>
              <span className="text-[10px] text-gray-400">Catálogo del servidor</span>
            </div>
          </div>

          {/* TABLA: Próximas reservas */}
          <div className="bg-white rounded-3xl border border-sand-200/80 shadow-sm overflow-hidden">
            <div className="p-6 border-b border-sand-100 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
              <div>
                <h2 className="font-serif text-xl font-semibold text-forest-900">
                  Próximas reservas
                </h2>
              </div>
              <a href="#" className="text-xs font-semibold text-forest-800 hover:underline">
                Ver calendario →
              </a>
            </div>

            {/* Barra de búsqueda de la tabla */}
            <div className="p-4 bg-sand-50/40 border-b border-sand-100 flex flex-wrap gap-3 items-center justify-between text-xs">
              <div className="relative flex-1 min-w-[200px] max-w-sm">
                <Search size={14} className="absolute left-3 top-2.5 text-gray-400" />
                <input 
                  type="text" 
                  placeholder="Buscar por cliente o reserva..."
                  value={busqueda}
                  onChange={(event) => setBusqueda(event.target.value)}
                  className="w-full bg-white border border-sand-200 rounded-lg pl-9 pr-3 py-1.5 text-xs focus:outline-none focus:border-forest-800"
                />
              </div>

              <div className="flex gap-2">
                <span className="bg-white border border-sand-200 px-3 py-1.5 rounded-lg text-gray-600 font-medium">Todas · {reservas.length}</span>
                <span className="bg-white border border-sand-200 px-3 py-1.5 rounded-lg text-gray-600 font-medium">Con saldo · {reservas.filter((reserva) => Number(reserva.saldo_pendiente) > 0).length}</span>
              </div>
            </div>

            {/* Registros de reservas */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-sand-50 text-[10px] uppercase font-bold text-gray-500 border-b border-sand-100">
                  <tr>
                    <th className="py-3 px-6">Reserva</th>
                    <th className="py-3 px-6">Cliente / Espacio</th>
                    <th className="py-3 px-6">Fecha y Bloque</th>
                    <th className="py-3 px-6">Total CLP</th>
                    <th className="py-3 px-6">Saldo CLP</th>
                    <th className="py-3 px-6">Estado</th>
                    <th className="py-3 px-6 text-right">Acción</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-sand-100">
                  {reservasVisibles.map((reserva) => (
                    <tr key={reserva.id_reserva} className="hover:bg-sand-50/60 transition-colors">
                      <td className="py-4 px-6 font-mono text-gray-500">RES-{reserva.id_reserva}</td>
                      <td className="py-4 px-6">
                        <div className="font-semibold text-gray-800">{reserva.cliente}</div>
                        <div className="text-[11px] text-gray-400">
                          {reserva.sectores.map((sector) => sector.nombre).join(', ')}
                        </div>
                      </td>
                      <td className="py-4 px-6 text-gray-600">
                        {reserva.fecha}<br />
                        {reserva.hora_inicio ? `${reserva.hora_inicio}–${reserva.hora_fin}` : 'Horario no registrado'}
                      </td>
                      <td className="py-4 px-6 font-medium text-gray-800">
                        ${Number(reserva.monto_total).toLocaleString('es-CL')}
                      </td>
                      <td className="py-4 px-6 font-bold text-forest-900">
                        ${Number(reserva.saldo_pendiente).toLocaleString('es-CL')}
                      </td>
                      <td className="py-4 px-6">
                        <span className="bg-sand-100 text-gray-800 text-[10px] font-semibold px-2.5 py-1 rounded-full">
                          {reserva.estado}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-right text-gray-500">—</td>
                    </tr>
                  ))}
                  {!cargando && reservasVisibles.length === 0 && (
                    <tr><td colSpan="7" className="p-6 text-center text-gray-500">No hay reservas para mostrar.</td></tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Aviso de caducidad 24 horas */}
            <div className="p-3 bg-amber-50/80 border-t border-amber-200/80 text-[11px] text-amber-900 flex items-center gap-2 px-6">
              <Clock size={13} />
              <span>Los montos y estados reflejan la información devuelta por la API.</span>
            </div>
          </div>

          {/* GRID INFERIOR: Catálogo/Edición a la izquierda, Consultas y Bloques a la derecha */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            
            {/* LADO IZQUIERDO: Catálogo de espacios y Edición */}
            <div className="lg:col-span-6 space-y-6">
              
              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <div className="flex justify-between items-center">
                  <h3 className="font-serif text-lg font-semibold text-forest-900">Catálogo y precios</h3>
                  <span className="text-xs text-gray-500">Datos del catálogo</span>
                </div>

                <div className="space-y-3">
                  {sectores.map((sector) => (
                    <button
                      type="button"
                      key={sector.id_sector}
                      onClick={() => {
                        setSectorSeleccionado(String(sector.id_sector));
                        setTarifa(sector.valor_hora);
                      }}
                      className="w-full text-left flex items-center justify-between p-3 rounded-xl border border-sand-200 bg-sand-50/30"
                    >
                      <span>
                        <span className="font-semibold text-xs block text-gray-800">{sector.nombre}</span>
                        <span className="text-[10px] text-gray-400">${Number(sector.valor_hora).toLocaleString('es-CL')} / hora</span>
                      </span>
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full">{sector.estado}</span>
                    </button>
                  ))}
                  {!sectores.length && !cargando && (
                    <p className="text-xs text-gray-500">El catálogo no tiene espacios.</p>
                  )}
                </div>
              </div>

              <form onSubmit={guardarTarifa} className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <div className="flex justify-between items-center">
                  <h3 className="font-serif text-lg font-semibold text-forest-900">Editar tarifa</h3>
                  <span className="text-[10px] bg-sand-100 text-gray-600 px-2 py-0.5 rounded-full">{sectorActual?.nombre || 'Selecciona un espacio'}</span>
                </div>

                <label className="block text-xs text-gray-600">
                  Tarifa por hora · CLP
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    disabled={!sectorActual}
                    value={tarifa}
                    onChange={(event) => setTarifa(event.target.value)}
                    className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                  />
                </label>

                <div className="flex gap-3 pt-2">
                  <button disabled={!sectorActual || guardandoSector} className="bg-forest-800 text-white text-xs font-semibold px-4 py-2 rounded-xl hover:bg-forest-900 disabled:opacity-50">
                    {guardandoSector ? 'Guardando…' : 'Guardar tarifa'}
                  </button>
                </div>
              </form>

            </div>

            {/* LADO DERECHO: Estado de funciones pendientes */}
            <div className="lg:col-span-6 space-y-6">
              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <h3 className="font-serif text-lg font-semibold text-forest-900">Consultas</h3>
                <p className="text-sm text-gray-600">
                  La API y el esquema documentado todavía no incluyen consultas ni respuestas. Esta sección no registra datos.
                </p>
              </div>

              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <h3 className="font-serif text-lg font-semibold text-forest-900">Disponibilidad por bloque</h3>
                <p className="text-sm text-gray-600">
                  La disponibilidad se valida al crear la reserva para evitar superposiciones. Aún no existe un endpoint de calendario.
                </p>
              </div>

            </div>

          </div>

        </main>

      </div>

    </div>
  );
}
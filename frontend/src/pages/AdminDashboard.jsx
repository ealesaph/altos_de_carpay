import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  CalendarDays,
  Tags,
  MessageSquare,
  ExternalLink,
  LogOut,
  Clock,
  Search,
  Bell,
  Mail,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  XCircle,
  CreditCard,
  Image as ImageIcon,
  Trash2,
  Users,
  Upload,
  Shield,
} from 'lucide-react';
import defaultSpaceImg from '../assets/default-space.svg';
import {
  cancelReservation,
  changeReservationStatus,
  createPayment,
  createStaffAdmin,
  exportReservationsCsv,
  getConsultas,
  getProfile,
  getReservations,
  getSectors,
  getUsers,
  hasSession,
  logout,
  replyConsulta,
  sendReservationEmail,
  updateSector,
  updateUser,
  uploadSectorImage,
} from '../api.js';

const TRANSICIONES_ESTADO = {
  Pendiente: ['Confirmado'],
  Confirmado: ['En Proceso'],
  'En Proceso': ['Finalizado'],
  Finalizado: [],
  Cancelado: [],
};

export default function AdminDashboard() {
  const [seccionActiva, setSeccionActiva] = useState('dashboard');
  const [reservas, setReservas] = useState([]);
  const [sectores, setSectores] = useState([]);
  const [consultas, setConsultas] = useState([]);
  const [respuestas, setRespuestas] = useState({});
  const [respondiendoId, setRespondiendoId] = useState(null);
  const [consultaExpandidaId, setConsultaExpandidaId] = useState(null);
  const [filtroConsulta, setFiltroConsulta] = useState('Todas');

  const [sectorEditandoId, setSectorEditandoId] = useState(null);
  const [sectorForm, setSectorForm] = useState({
    nombre: '',
    descripcion: '',
    valor_hora: '',
    estado: 'Disponible',
    imagenUrl: '',
  });
  const [guardandoSector, setGuardandoSector] = useState(false);
  const [subiendoImagen, setSubiendoImagen] = useState(false);

  const [usuarios, setUsuarios] = useState([]);
  const [adminPerfil, setAdminPerfil] = useState(null);
  const [busquedaUsuario, setBusquedaUsuario] = useState('');
  const [filtroRolUsuario, setFiltroRolUsuario] = useState('Todos');
  const [nuevoStaffForm, setNuevoStaffForm] = useState({
    correo: '',
    rut: '',
    nombre: '',
    apellido_paterno: '',
    apellido_materno: '',
    telefono: '',
    password: '',
  });
  const [creandoStaff, setCreandoStaff] = useState(false);
  const [procesandoUsuarioId, setProcesandoUsuarioId] = useState(null);

  const [busqueda, setBusqueda] = useState('');
  const [filtroReserva, setFiltroReserva] = useState('Todas');
  const [reservaExpandidaId, setReservaExpandidaId] = useState(null);
  const [procesandoReservaId, setProcesandoReservaId] = useState(null);
  const [metodoPagoManual, setMetodoPagoManual] = useState('Transferencia');

  const [mensajeExito, setMensajeExito] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const navigate = useNavigate();

  const cargarTodo = async () => {
    const [reservasApi, sectoresApi, consultasApi, perfilApi, usuariosApi] = await Promise.all([
      getReservations(),
      getSectors(),
      getConsultas(),
      getProfile(),
      getUsers(),
    ]);
    setReservas(reservasApi);
    setSectores(sectoresApi);
    setConsultas(consultasApi);
    setAdminPerfil(perfilApi);
    setUsuarios(usuariosApi);
    if (sectoresApi.length && sectorEditandoId === null) {
      seleccionarSectorParaEditar(sectoresApi[0]);
    }
  };

  useEffect(() => {
    if (!hasSession()) {
      navigate('/login', { replace: true });
      return;
    }
    cargarTodo()
      .catch((requestError) => setError(requestError.message))
      .finally(() => setCargando(false));
  }, [navigate]); // eslint-disable-line react-hooks/exhaustive-deps

  const seleccionarSectorParaEditar = (sector) => {
    setSectorEditandoId(sector.id_sector);
    setSectorForm({
      nombre: sector.nombre || '',
      descripcion: sector.descripcion || '',
      valor_hora: sector.valor_hora ?? '',
      estado: sector.estado || 'Disponible',
      imagenUrl: sector.fotografias?.[0] || '',
    });
  };

  const notificar = (texto) => {
    setError('');
    setMensajeExito(texto);
  };

  const reservasVisibles = reservas.filter((reserva) => {
    const texto = `${reserva.id_reserva} ${reserva.cliente} ${reserva.correo_cliente}`.toLowerCase();
    if (!texto.includes(busqueda.toLowerCase())) return false;
    if (filtroReserva === 'Con saldo') return Number(reserva.saldo_pendiente) > 0;
    if (filtroReserva === 'Pendiente') return reserva.estado === 'Pendiente';
    if (filtroReserva === 'Confirmado') return reserva.estado === 'Confirmado';
    return true;
  });

  const consultasVisibles = consultas.filter((consulta) => {
    if (filtroConsulta === 'Pendiente') return consulta.estado === 'Pendiente';
    if (filtroConsulta === 'Respondida') return consulta.estado === 'Respondida';
    return true;
  });

  const saldoPorCobrar = reservas.reduce(
    (total, reserva) => total + Number(reserva.saldo_pendiente),
    0,
  );
  const sectorActual = sectores.find((s) => s.id_sector === sectorEditandoId);

  const guardarSector = async (event) => {
    event.preventDefault();
    if (!sectorActual) return;
    setError('');
    setMensajeExito('');
    setGuardandoSector(true);
    try {
      const imagenLimpia = sectorForm.imagenUrl.trim();
      const actualizado = await updateSector(sectorActual.id_sector, {
        nombre: sectorForm.nombre.trim(),
        descripcion: sectorForm.descripcion.trim(),
        valor_hora: sectorForm.valor_hora,
        estado: sectorForm.estado,
        fotografias: imagenLimpia ? [imagenLimpia] : [],
      });
      setSectores((actuales) =>
        actuales.map((s) => (s.id_sector === actualizado.id_sector ? actualizado : s)),
      );
      setSectorForm((f) => ({ ...f, imagenUrl: actualizado.fotografias?.[0] || '' }));
      notificar(`Espacio «${actualizado.nombre}» actualizado correctamente.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setGuardandoSector(false);
    }
  };

  const ejecutarAccionReserva = async (id, accion) => {
    setError('');
    setMensajeExito('');
    setProcesandoReservaId(id);
    try {
      await accion();
      const reservasActualizadas = await getReservations();
      setReservas(reservasActualizadas);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setProcesandoReservaId(null);
    }
  };

  const manejarPagoManual = (reserva) =>
    ejecutarAccionReserva(reserva.id_reserva, async () => {
      const tipo = Number(reserva.monto_pagado) > 0 ? 'Saldo' : 'Anticipo';
      await createPayment(reserva.id_reserva, {
        tipo_pago: tipo,
        metodo_pago: metodoPagoManual,
      });
      notificar(`Pago (${tipo} · ${metodoPagoManual}) registrado en reserva RES-${reserva.id_reserva}.`);
    });

  const manejarCambioEstado = (reserva, nuevoEstado) =>
    ejecutarAccionReserva(reserva.id_reserva, async () => {
      await changeReservationStatus(reserva.id_reserva, nuevoEstado);
      notificar(`Reserva RES-${reserva.id_reserva} actualizada a «${nuevoEstado}».`);
    });

  const manejarCancelarReserva = (reserva) =>
    ejecutarAccionReserva(reserva.id_reserva, async () => {
      await cancelReservation(reserva.id_reserva);
      notificar(`Reserva RES-${reserva.id_reserva} cancelada.`);
    });

  const manejarReenviarCorreo = (reserva) =>
    ejecutarAccionReserva(reserva.id_reserva, async () => {
      const resp = await sendReservationEmail(reserva.id_reserva);
      notificar(resp?.detail || `Correo enviado a ${reserva.correo_cliente}.`);
    });

  const cerrarSesion = async () => {
    try {
      await logout();
      navigate('/');
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const nombreSeccion = {
    dashboard: 'Vista general',
    reservas: 'Reservas',
    catalogo: 'Catálogo y precios',
    consultas: 'Consultas',
    usuarios: 'Usuarios y personal',
  }[seccionActiva] || 'Vista general';

  const esSuperusuario = Boolean(adminPerfil?.is_superuser);
  const usuariosVisibles = usuarios.filter((u) => {
    if (filtroRolUsuario !== 'Todos' && u.rol !== filtroRolUsuario) return false;
    const texto = `${u.nombre} ${u.apellido_paterno} ${u.correo} ${u.rut}`.toLowerCase();
    return texto.includes(busquedaUsuario.toLowerCase());
  });

  const manejarSubirImagenArchivo = async (sectorId, file) => {
    if (!file) return;
    setError('');
    setMensajeExito('');
    setSubiendoImagen(true);
    try {
      const actualizado = await uploadSectorImage(sectorId, file);
      setSectores((actuales) =>
        actuales.map((s) => (s.id_sector === actualizado.id_sector ? actualizado : s)),
      );
      setSectorForm((f) => ({ ...f, imagenUrl: actualizado.fotografias?.[0] || '' }));
      notificar(`Imagen de «${actualizado.nombre}» subida y guardada.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubiendoImagen(false);
    }
  };

  const manejarCrearPersonal = async (event) => {
    event.preventDefault();
    setError('');
    setMensajeExito('');
    setCreandoStaff(true);
    try {
      const creado = await createStaffAdmin(nuevoStaffForm);
      setUsuarios((prev) => [...prev, creado]);
      setNuevoStaffForm({
        correo: '',
        rut: '',
        nombre: '',
        apellido_paterno: '',
        apellido_materno: '',
        telefono: '',
        password: '',
      });
      notificar(`Personal administrativo «${creado.nombre} ${creado.apellido_paterno}» creado.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setCreandoStaff(false);
    }
  };

  const manejarActualizarUsuario = async (usuario, changes) => {
    setError('');
    setMensajeExito('');
    setProcesandoUsuarioId(usuario.id_usuario);
    try {
      const actualizado = await updateUser(usuario.id_usuario, changes);
      setUsuarios((prev) =>
        prev.map((u) => (u.id_usuario === actualizado.id_usuario ? actualizado : u)),
      );
      notificar(`Usuario ${actualizado.correo} actualizado.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setProcesandoUsuarioId(null);
    }
  };

  const mostrarTodo = seccionActiva === 'dashboard';
  const consultasPendientesCount = consultas.filter((c) => c.estado === 'Pendiente').length;

  return (
    <div className="min-h-screen bg-sand-50 flex font-sans text-gray-800 antialiased">

      {/* 1. SIDEBAR LATERAL (Verde institucional) */}
      <aside className="w-64 bg-forest-900 text-white flex flex-col justify-between p-6 shrink-0 hidden md:flex">
        <div className="space-y-8">
          <div>
            <div className="flex items-center gap-2 text-white">
              <span className="text-xl">🍃</span>
              <span className="font-serif text-xl font-bold tracking-tight">Altos de carpay</span>
            </div>
            <span className="text-[10px] uppercase tracking-widest text-forest-100/60 block mt-1 font-semibold">
              ADMINISTRACIÓN
            </span>
          </div>

          <nav className="space-y-1 text-xs">
            <button
              type="button"
              onClick={() => setSeccionActiva('dashboard')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'dashboard' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <LayoutDashboard size={16} />
              <span>Vista general</span>
            </button>

            <button
              type="button"
              onClick={() => setSeccionActiva('reservas')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'reservas' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <span className="flex items-center gap-3">
                <CalendarDays size={16} />
                <span>Reservas</span>
              </span>
              <span className="text-[10px] bg-forest-950/60 px-2 py-0.5 rounded-full">{reservas.length}</span>
            </button>

            <button
              type="button"
              onClick={() => setSeccionActiva('catalogo')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'catalogo' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <span className="flex items-center gap-3">
                <Tags size={16} />
                <span>Catálogo y precios</span>
              </span>
              <span className="text-[10px] bg-forest-950/60 px-2 py-0.5 rounded-full">{sectores.length}</span>
            </button>

            <button
              type="button"
              onClick={() => setSeccionActiva('consultas')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'consultas' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <span className="flex items-center gap-3">
                <MessageSquare size={16} />
                <span>Consultas</span>
              </span>
              {consultasPendientesCount > 0 && (
                <span className="text-[10px] bg-amber-400 text-forest-950 font-bold px-2 py-0.5 rounded-full">
                  {consultasPendientesCount}
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={() => setSeccionActiva('usuarios')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl transition-colors ${
                seccionActiva === 'usuarios' ? 'bg-forest-800 text-white font-semibold' : 'text-gray-300 hover:bg-forest-800/50'
              }`}
            >
              <span className="flex items-center gap-3">
                <Users size={16} />
                <span>Usuarios y personal</span>
              </span>
              <span className="text-[10px] bg-forest-950/60 px-2 py-0.5 rounded-full">{usuarios.length}</span>
            </button>
          </nav>
        </div>

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
        <header className="h-16 bg-white border-b border-sand-200/80 px-8 flex items-center justify-between sticky top-0 z-30">
          <div className="text-xs text-gray-500">
            Gestión de Altos de carpay / <span className="text-forest-900 font-semibold">{nombreSeccion}</span>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <button
              type="button"
              onClick={() => {
                setSeccionActiva('consultas');
                setFiltroConsulta('Pendiente');
              }}
              title="Ver consultas pendientes"
              className="text-gray-400 hover:text-forest-900 relative"
            >
              <Bell size={16} />
              {consultasPendientesCount > 0 && (
                <span className="w-2 h-2 rounded-full bg-amber-500 absolute -top-0.5 -right-0.5" />
              )}
            </button>
            <Link
              to="/mi-cuenta"
              className="flex items-center gap-2 border-l border-sand-200 pl-4 hover:text-forest-900"
              title="Editar mis datos o cerrar cuenta"
            >
              <span className="w-7 h-7 rounded-full bg-forest-100 text-forest-900 font-bold flex items-center justify-center text-xs">
                AC
              </span>
              <span className="font-medium text-gray-700 hover:text-forest-900">Mi cuenta</span>
            </Link>
          </div>
        </header>

        <main className="p-8 max-w-6xl w-full mx-auto space-y-8">
          {/* Navegación rápida en móviles */}
          <div className="flex md:hidden flex-wrap gap-2">
            {[
              ['dashboard', 'Vista general'],
              ['reservas', 'Reservas'],
              ['catalogo', 'Catálogo y precios'],
              ['consultas', 'Consultas'],
              ['usuarios', 'Usuarios y personal'],
            ].map(([id, label]) => (
              <button
                key={id}
                type="button"
                onClick={() => setSeccionActiva(id)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium border ${
                  seccionActiva === id
                    ? 'bg-forest-900 text-white border-forest-900'
                    : 'bg-white text-gray-700 border-sand-200'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {error && (
            <div role="alert" className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-3 rounded-xl flex justify-between items-center">
              <span>{error}</span>
              <button type="button" onClick={() => setError('')} className="font-semibold hover:underline">Cerrar</button>
            </div>
          )}
          {mensajeExito && (
            <div className="bg-forest-50 border border-forest-200 text-forest-900 text-xs px-4 py-3 rounded-xl flex justify-between items-center">
              <span>{mensajeExito}</span>
              <button type="button" onClick={() => setMensajeExito('')} className="font-semibold hover:underline">Cerrar</button>
            </div>
          )}
          {cargando && <p className="text-sm text-gray-500">Cargando datos…</p>}

          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
            <div>
              <h1 className="font-serif text-3xl font-bold text-forest-900">
                {mostrarTodo ? 'Cada encuentro, en orden.' : nombreSeccion}
              </h1>
              <p className="text-xs text-gray-500 mt-1">
                Haz clic en cualquier tarjeta, reserva, espacio o consulta para gestionarlo en vivo.
              </p>
            </div>
          </div>

          {/* Tarjetas de Métricas (Quick Menu interactivo) */}
          {mostrarTodo && (
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <button
                type="button"
                onClick={() => {
                  setSeccionActiva('reservas');
                  setFiltroReserva('Todas');
                }}
                className="text-left bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm hover:border-forest-700 hover:shadow-md transition-all"
              >
                <span className="text-3xl font-serif font-bold text-forest-900 block">{reservas.length}</span>
                <span className="text-xs font-semibold text-gray-700 mt-1 block">Reservas</span>
                <span className="text-[10px] text-forest-700">Ver todas →</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setSeccionActiva('reservas');
                  setFiltroReserva('Con saldo');
                }}
                className="text-left bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm hover:border-forest-700 hover:shadow-md transition-all"
              >
                <span className="text-3xl font-serif font-bold text-forest-900 block">
                  ${saldoPorCobrar.toLocaleString('es-CL')}
                </span>
                <span className="text-xs font-semibold text-gray-700 mt-1 block">Saldo por cobrar</span>
                <span className="text-[10px] text-forest-700">
                  {reservas.filter((r) => Number(r.saldo_pendiente) > 0).length} con saldo →
                </span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setSeccionActiva('consultas');
                  setFiltroConsulta('Pendiente');
                }}
                className="text-left bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm hover:border-forest-700 hover:shadow-md transition-all"
              >
                <span className="text-3xl font-serif font-bold text-forest-900 block">
                  {consultasPendientesCount}
                </span>
                <span className="text-xs font-semibold text-gray-700 mt-1 block">Consultas pendientes</span>
                <span className="text-[10px] text-amber-700">Ir a responder ({consultas.length} totales) →</span>
              </button>

              <button
                type="button"
                onClick={() => setSeccionActiva('catalogo')}
                className="text-left bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm hover:border-forest-700 hover:shadow-md transition-all"
              >
                <span className="text-3xl font-serif font-bold text-forest-900 block">{sectores.length}</span>
                <span className="text-xs font-semibold text-gray-700 mt-1 block">Espacios publicados</span>
                <span className="text-[10px] text-forest-700">Editar catálogo →</span>
              </button>
            </div>
          )}

          {/* TABLA INTERACTIVA: Reservas */}
          {(mostrarTodo || seccionActiva === 'reservas') && (
            <div className="bg-white rounded-3xl border border-sand-200/80 shadow-sm overflow-hidden">
              <div className="p-6 border-b border-sand-100 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
                <div>
                  <h2 className="font-serif text-xl font-semibold text-forest-900">Próximas reservas</h2>
                </div>
                <button
                  type="button"
                  onClick={() => exportReservationsCsv().catch((e) => setError(e.message))}
                  className="text-xs font-semibold text-forest-800 hover:underline"
                >
                  Exportar CSV (Excel) →
                </button>
              </div>

              <div className="p-4 bg-sand-50/40 border-b border-sand-100 flex flex-wrap gap-3 items-center justify-between text-xs">
                <div className="relative flex-1 min-w-[200px] max-w-sm">
                  <Search size={14} className="absolute left-3 top-2.5 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Buscar por cliente, correo o N°..."
                    value={busqueda}
                    onChange={(event) => setBusqueda(event.target.value)}
                    className="w-full bg-white border border-sand-200 rounded-lg pl-9 pr-3 py-1.5 text-xs focus:outline-none focus:border-forest-800"
                  />
                </div>

                <div className="flex flex-wrap gap-2">
                  {[
                    ['Todas', `Todas · ${reservas.length}`],
                    ['Con saldo', `Con saldo · ${reservas.filter((r) => Number(r.saldo_pendiente) > 0).length}`],
                    ['Pendiente', `Pendientes · ${reservas.filter((r) => r.estado === 'Pendiente').length}`],
                    ['Confirmado', `Confirmadas · ${reservas.filter((r) => r.estado === 'Confirmado').length}`],
                  ].map(([clave, etiqueta]) => (
                    <button
                      key={clave}
                      type="button"
                      onClick={() => setFiltroReserva(clave)}
                      className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                        filtroReserva === clave
                          ? 'bg-forest-900 text-white border-forest-900'
                          : 'bg-white border-sand-200 text-gray-600 hover:border-forest-700'
                      }`}
                    >
                      {etiqueta}
                    </button>
                  ))}
                </div>
              </div>

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
                      <th className="py-3 px-6 text-right">Gestionar</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-sand-100">
                    {reservasVisibles.map((reserva) => {
                      const abierta = reservaExpandidaId === reserva.id_reserva;
                      const ocupada = procesandoReservaId === reserva.id_reserva;
                      const siguientes = TRANSICIONES_ESTADO[reserva.estado] || [];
                      return (
                        <React.Fragment key={reserva.id_reserva}>
                          <tr
                            onClick={() => setReservaExpandidaId(abierta ? null : reserva.id_reserva)}
                            className={`cursor-pointer transition-colors ${abierta ? 'bg-forest-50/40' : 'hover:bg-sand-50/60'}`}
                          >
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
                            <td className="py-4 px-6 text-right">
                              <span className="inline-flex items-center gap-1 text-forest-800 font-semibold">
                                {abierta ? <>Ocultar <ChevronUp size={14} /></> : <>Abrir <ChevronDown size={14} /></>}
                              </span>
                            </td>
                          </tr>

                          {abierta && (
                            <tr className="bg-sand-50/60">
                              <td colSpan="7" className="px-6 py-4">
                                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                                  <div className="space-y-1 text-xs text-gray-600">
                                    <p>
                                      <strong>Correo:</strong> {reserva.correo_cliente} ·{' '}
                                      <strong>Pagado:</strong> ${Number(reserva.monto_pagado).toLocaleString('es-CL')}
                                    </p>
                                    {reserva.servicios?.length > 0 && (
                                      <p>
                                        <strong>Servicios extra:</strong>{' '}
                                        {reserva.servicios.map((s) => `${s.nombre} (x${s.cantidad})`).join(', ')}
                                      </p>
                                    )}
                                    <p className="font-mono text-[11px] text-gray-500">
                                      QR: {reserva.token_qr}
                                    </p>
                                  </div>

                                  <div className="flex flex-wrap items-center gap-2">
                                    {Number(reserva.saldo_pendiente) > 0 && reserva.estado !== 'Cancelado' && (
                                      <div className="flex items-center gap-1 bg-white border border-sand-200 rounded-lg p-1">
                                        <select
                                          value={metodoPagoManual}
                                          onChange={(e) => setMetodoPagoManual(e.target.value)}
                                          className="text-xs px-2 py-1 bg-transparent focus:outline-none"
                                        >
                                          <option value="Transferencia">Transferencia</option>
                                          <option value="Efectivo">Efectivo</option>
                                        </select>
                                        <button
                                          type="button"
                                          disabled={ocupada}
                                          onClick={() => manejarPagoManual(reserva)}
                                          className="inline-flex items-center gap-1 bg-forest-800 text-white px-2.5 py-1 rounded-md font-semibold hover:bg-forest-900 disabled:opacity-50"
                                        >
                                          <CreditCard size={13} />
                                          {Number(reserva.monto_pagado) > 0 ? 'Registrar saldo' : 'Registrar anticipo'}
                                        </button>
                                      </div>
                                    )}

                                    {siguientes.map((estadoDest) => (
                                      <button
                                        key={estadoDest}
                                        type="button"
                                        disabled={ocupada}
                                        onClick={() => manejarCambioEstado(reserva, estadoDest)}
                                        className="inline-flex items-center gap-1 bg-white border border-forest-800 text-forest-900 px-3 py-1.5 rounded-lg font-semibold hover:bg-forest-50 disabled:opacity-50"
                                      >
                                        <CheckCircle2 size={13} /> Pasar a {estadoDest}
                                      </button>
                                    ))}

                                    <button
                                      type="button"
                                      disabled={ocupada}
                                      onClick={() => manejarReenviarCorreo(reserva)}
                                      className="inline-flex items-center gap-1 bg-white border border-sand-300 text-gray-700 px-3 py-1.5 rounded-lg font-medium hover:bg-sand-100 disabled:opacity-50"
                                    >
                                      <Mail size={13} /> Enviar correo
                                    </button>

                                    {!['Cancelado', 'En Proceso', 'Finalizado'].includes(reserva.estado) && (
                                      <button
                                        type="button"
                                        disabled={ocupada}
                                        onClick={() => manejarCancelarReserva(reserva)}
                                        className="inline-flex items-center gap-1 bg-white border border-red-200 text-red-700 px-3 py-1.5 rounded-lg font-medium hover:bg-red-50 disabled:opacity-50"
                                      >
                                        <XCircle size={13} /> Cancelar
                                      </button>
                                    )}
                                  </div>
                                </div>
                              </td>
                            </tr>
                          )}
                        </React.Fragment>
                      );
                    })}
                    {!cargando && reservasVisibles.length === 0 && (
                      <tr><td colSpan="7" className="p-6 text-center text-gray-500">No hay reservas para mostrar con este filtro.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>

              <div className="p-3 bg-amber-50/80 border-t border-amber-200/80 text-[11px] text-amber-900 flex items-center gap-2 px-6">
                <Clock size={13} />
                <span>Haz clic en cualquier fila para registrar pagos, cambiar estado o reenviar el detalle por correo.</span>
              </div>
            </div>
          )}

          {/* GRID INFERIOR: Catálogo/Edición inline y Consultas */}
          {(mostrarTodo || seccionActiva === 'catalogo' || seccionActiva === 'consultas') && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">

              {/* CATÁLOGO Y EDICIÓN INLINE */}
              {(mostrarTodo || seccionActiva === 'catalogo') && (
                <div className={`${seccionActiva === 'catalogo' ? 'lg:col-span-12' : 'lg:col-span-6'} space-y-6`}>
                  <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                    <div className="flex justify-between items-center">
                      <h3 className="font-serif text-lg font-semibold text-forest-900">Catálogo y precios</h3>
                      <span className="text-xs text-gray-500">Haz clic en un espacio para editarlo</span>
                    </div>

                    <div className="space-y-3">
                      {sectores.map((sector) => {
                        const seleccionado = sectorEditandoId === sector.id_sector;
                        return (
                          <div
                            key={sector.id_sector}
                            className={`rounded-2xl border transition-colors ${
                              seleccionado ? 'border-forest-800 bg-forest-50/30' : 'border-sand-200 bg-sand-50/30'
                            }`}
                          >
                            <button
                              type="button"
                              onClick={() =>
                                seleccionado ? setSectorEditandoId(null) : seleccionarSectorParaEditar(sector)
                              }
                              className="w-full text-left flex items-center justify-between p-3.5"
                            >
                              <span>
                                <span className="font-semibold text-xs block text-gray-800">{sector.nombre}</span>
                                <span className="text-[11px] text-gray-500">
                                  ${Number(sector.valor_hora).toLocaleString('es-CL')} / hora
                                </span>
                              </span>
                              <span className="flex items-center gap-2">
                                <span className="text-[10px] font-semibold px-2.5 py-0.5 rounded-full bg-white border border-sand-200">
                                  {sector.estado}
                                </span>
                                {seleccionado ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                              </span>
                            </button>

                            {seleccionado && (
                              <form onSubmit={guardarSector} className="px-4 pb-4 pt-2 border-t border-sand-200/80 space-y-3 text-xs">
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                  <label className="block text-gray-600">
                                    Nombre del espacio
                                    <input
                                      type="text"
                                      required
                                      value={sectorForm.nombre}
                                      onChange={(e) => setSectorForm((f) => ({ ...f, nombre: e.target.value }))}
                                      className="w-full bg-white border border-sand-200 rounded-lg p-2 mt-1"
                                    />
                                  </label>
                                  <label className="block text-gray-600">
                                    Tarifa por hora (CLP)
                                    <input
                                      type="number"
                                      min="0"
                                      step="0.01"
                                      required
                                      value={sectorForm.valor_hora}
                                      onChange={(e) => setSectorForm((f) => ({ ...f, valor_hora: e.target.value }))}
                                      className="w-full bg-white border border-sand-200 rounded-lg p-2 mt-1"
                                    />
                                  </label>
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                  <label className="block text-gray-600">
                                    Estado
                                    <select
                                      value={sectorForm.estado}
                                      onChange={(e) => setSectorForm((f) => ({ ...f, estado: e.target.value }))}
                                      className="w-full bg-white border border-sand-200 rounded-lg p-2 mt-1"
                                    >
                                      <option value="Disponible">Disponible</option>
                                      <option value="Mantencion">Mantención</option>
                                      <option value="Inactivo">Inactivo</option>
                                    </select>
                                  </label>
                                  <label className="block text-gray-600">
                                    Descripción
                                    <input
                                      type="text"
                                      value={sectorForm.descripcion}
                                      onChange={(e) => setSectorForm((f) => ({ ...f, descripcion: e.target.value }))}
                                      className="w-full bg-white border border-sand-200 rounded-lg p-2 mt-1"
                                    />
                                  </label>
                                </div>

                                <div className="bg-white border border-sand-200 rounded-xl p-3 space-y-3">
                                  <div className="flex flex-col sm:flex-row sm:items-center gap-3">
                                    <img
                                      src={sectorForm.imagenUrl.trim() || defaultSpaceImg}
                                      alt={sectorForm.nombre || 'Vista previa'}
                                      onError={(e) => { e.currentTarget.src = defaultSpaceImg; }}
                                      className="w-24 h-16 object-cover rounded-lg border border-sand-200 shrink-0"
                                    />
                                    <div className="flex-1 space-y-1.5">
                                      <label className="flex items-center gap-1.5 text-gray-600 font-medium">
                                        <ImageIcon size={13} />
                                        <span>URL de imagen (http/https)</span>
                                        {!sectorForm.imagenUrl.trim() && (
                                          <span className="text-[10px] bg-sand-100 text-gray-600 px-2 py-0.5 rounded-full">
                                            Usando imagen por defecto
                                          </span>
                                        )}
                                      </label>
                                      <div className="flex flex-wrap gap-2">
                                        <input
                                          type="url"
                                          placeholder="https://ejemplo.com/espacio.jpg"
                                          value={sectorForm.imagenUrl}
                                          onChange={(e) => setSectorForm((f) => ({ ...f, imagenUrl: e.target.value }))}
                                          className="flex-1 min-w-[180px] bg-sand-50/40 border border-sand-200 rounded-lg p-2 text-xs"
                                        />
                                        <label className="inline-flex items-center gap-1 border border-forest-800/40 text-forest-900 bg-forest-50/60 hover:bg-forest-100 px-2.5 py-1.5 rounded-lg font-medium cursor-pointer shrink-0">
                                          <Upload size={13} />
                                          <span>{subiendoImagen ? 'Subiendo…' : 'Subir archivo'}</span>
                                          <input
                                            type="file"
                                            accept="image/jpeg,image/png,image/webp"
                                            disabled={subiendoImagen}
                                            onChange={(e) => {
                                              const file = e.target.files?.[0];
                                              if (file) manejarSubirImagenArchivo(sector.id_sector, file);
                                              e.target.value = '';
                                            }}
                                            className="hidden"
                                          />
                                        </label>
                                        {sectorForm.imagenUrl.trim() && (
                                          <button
                                            type="button"
                                            onClick={() => setSectorForm((f) => ({ ...f, imagenUrl: '' }))}
                                            className="inline-flex items-center gap-1 border border-red-200 text-red-700 hover:bg-red-50 px-2.5 py-1.5 rounded-lg font-medium shrink-0"
                                          >
                                            <Trash2 size={13} /> Eliminar
                                          </button>
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                </div>

                                <div className="flex justify-end gap-2 pt-1">
                                  <button
                                    type="submit"
                                    disabled={guardandoSector}
                                    className="bg-forest-800 text-white font-semibold px-4 py-2 rounded-xl hover:bg-forest-900 disabled:opacity-50"
                                  >
                                    {guardandoSector ? 'Guardando…' : 'Guardar cambios'}
                                  </button>
                                </div>
                              </form>
                            )}
                          </div>
                        );
                      })}
                      {!sectores.length && !cargando && (
                        <p className="text-xs text-gray-500">El catálogo no tiene espacios.</p>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* CONSULTAS INTERACTIVAS */}
              {(mostrarTodo || seccionActiva === 'consultas') && (
                <div className={`${seccionActiva === 'consultas' ? 'lg:col-span-12' : 'lg:col-span-6'} space-y-6`}>
                  <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                    <div className="flex flex-wrap justify-between items-center gap-3">
                      <h3 className="font-serif text-lg font-semibold text-forest-900">Consultas</h3>
                      <div className="flex gap-1.5 text-xs">
                        {['Todas', 'Pendiente', 'Respondida'].map((estado) => (
                          <button
                            key={estado}
                            type="button"
                            onClick={() => setFiltroConsulta(estado)}
                            className={`px-2.5 py-1 rounded-lg border font-medium ${
                              filtroConsulta === estado
                                ? 'bg-forest-900 text-white border-forest-900'
                                : 'bg-white border-sand-200 text-gray-600 hover:border-forest-700'
                            }`}
                          >
                            {estado}
                          </button>
                        ))}
                      </div>
                    </div>

                    {consultasVisibles.length === 0 ? (
                      <p className="text-sm text-gray-600">No hay consultas para mostrar con este filtro.</p>
                    ) : (
                      <div className={`space-y-3 ${mostrarTodo ? 'max-h-96 overflow-y-auto pr-1' : ''}`}>
                        {(mostrarTodo ? consultasVisibles.slice(0, 6) : consultasVisibles).map((c) => {
                          const expandida = consultaExpandidaId === c.id_consulta || c.estado === 'Pendiente';
                          return (
                            <div key={c.id_consulta} className="rounded-2xl bg-sand-50/70 border border-sand-200 text-xs overflow-hidden">
                              <button
                                type="button"
                                onClick={() => setConsultaExpandidaId(consultaExpandidaId === c.id_consulta ? null : c.id_consulta)}
                                className="w-full p-3.5 text-left flex justify-between items-center gap-2 hover:bg-sand-100/50"
                              >
                                <div>
                                  <span className="font-semibold text-forest-900 block">{c.asunto}</span>
                                  <span className="text-[11px] text-gray-500">{c.cliente_nombre}</span>
                                </div>
                                <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold ${
                                  c.estado === 'Respondida' ? 'bg-forest-100 text-forest-800' : 'bg-amber-100 text-amber-800'
                                }`}>
                                  {c.estado}
                                </span>
                              </button>

                              {expandida && (
                                <div className="px-3.5 pb-3.5 space-y-2 border-t border-sand-200/60 pt-2.5">
                                  <p className="text-gray-700">{c.mensaje}</p>
                                  {c.respuesta ? (
                                    <p className="text-forest-800 font-medium bg-white p-2.5 rounded-lg border border-sand-200">
                                      Respuesta: {c.respuesta}
                                    </p>
                                  ) : (
                                    <div className="flex gap-2 pt-1">
                                      <input
                                        type="text"
                                        placeholder="Escribir respuesta y cerrar consulta..."
                                        value={respuestas[c.id_consulta] || ''}
                                        onChange={(e) => setRespuestas((prev) => ({ ...prev, [c.id_consulta]: e.target.value }))}
                                        className="flex-1 px-2.5 py-1.5 rounded-lg border border-sand-300 bg-white focus:outline-none focus:border-forest-700"
                                      />
                                      <button
                                        type="button"
                                        disabled={respondiendoId === c.id_consulta || !(respuestas[c.id_consulta] || '').trim()}
                                        onClick={async () => {
                                          setRespondiendoId(c.id_consulta);
                                          try {
                                            const actualizada = await replyConsulta(c.id_consulta, respuestas[c.id_consulta]);
                                            setConsultas((prev) =>
                                              prev.map((item) => (item.id_consulta === actualizada.id_consulta ? actualizada : item)),
                                            );
                                            setRespuestas((prev) => ({ ...prev, [c.id_consulta]: '' }));
                                            notificar(`Consulta «${actualizada.asunto}» respondida y cerrada.`);
                                          } catch (e) {
                                            setError(e.message);
                                          } finally {
                                            setRespondiendoId(null);
                                          }
                                        }}
                                        className="bg-forest-800 text-white px-3 py-1.5 rounded-lg font-semibold hover:bg-forest-900 disabled:opacity-50"
                                      >
                                        Responder
                                      </button>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  {mostrarTodo && (
                    <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-3">
                      <h3 className="font-serif text-lg font-semibold text-forest-900">Disponibilidad por bloque</h3>
                      <p className="text-sm text-gray-600">
                        La disponibilidad se valida automáticamente al crear cada reserva para evitar superposiciones horarias.
                      </p>
                    </div>
                  )}
                </div>
              )}

            </div>
          )}

          {seccionActiva === 'usuarios' && (
            <div className="space-y-6">
              {esSuperusuario ? (
                <form
                  onSubmit={manejarCrearPersonal}
                  className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <h3 className="font-serif text-lg font-semibold text-forest-900 flex items-center gap-2">
                        <Shield size={17} className="text-forest-800" /> Asignar nuevo personal administrador
                      </h3>
                      <p className="text-xs text-gray-500 mt-0.5">
                        El personal creado tendrá acceso operativo al panel administrativo, pero no podrá crear ni modificar otros administradores.
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-xs">
                    <label className="block text-gray-600">
                      Nombre
                      <input
                        type="text"
                        required
                        value={nuevoStaffForm.nombre}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, nombre: e.target.value }))}
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                    <label className="block text-gray-600">
                      Apellido paterno
                      <input
                        type="text"
                        required
                        value={nuevoStaffForm.apellido_paterno}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, apellido_paterno: e.target.value }))}
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                    <label className="block text-gray-600">
                      Apellido materno
                      <input
                        type="text"
                        required
                        value={nuevoStaffForm.apellido_materno}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, apellido_materno: e.target.value }))}
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                    <label className="block text-gray-600">
                      Correo electrónico
                      <input
                        type="email"
                        required
                        value={nuevoStaffForm.correo}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, correo: e.target.value }))}
                        placeholder="personal@altosdecarpay.com"
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                    <label className="block text-gray-600">
                      RUT
                      <input
                        type="text"
                        required
                        value={nuevoStaffForm.rut}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, rut: e.target.value }))}
                        placeholder="12.345.678-5"
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                    <label className="block text-gray-600">
                      Teléfono
                      <input
                        type="tel"
                        value={nuevoStaffForm.telefono}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, telefono: e.target.value }))}
                        placeholder="+56912345678"
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                    <label className="block text-gray-600 sm:col-span-2 lg:col-span-1">
                      Contraseña inicial (mín. 10 caracteres)
                      <input
                        type="password"
                        required
                        minLength={10}
                        value={nuevoStaffForm.password}
                        onChange={(e) => setNuevoStaffForm((f) => ({ ...f, password: e.target.value }))}
                        className="w-full border border-sand-200 rounded-lg p-2 mt-1"
                      />
                    </label>
                  </div>

                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={creandoStaff}
                      className="bg-forest-800 text-white text-xs font-semibold px-5 py-2.5 rounded-xl hover:bg-forest-900 disabled:opacity-50"
                    >
                      {creandoStaff ? 'Creando personal…' : 'Crear personal administrador'}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="bg-amber-50 border border-amber-200 text-amber-900 text-xs px-4 py-3 rounded-2xl">
                  Has iniciado sesión como <strong>Personal Administrador</strong>. Solo el superusuario principal puede crear o modificar cuentas de personal administrador.
                </div>
              )}

              <div className="bg-white rounded-3xl border border-sand-200/80 shadow-sm overflow-hidden">
                <div className="p-4 bg-sand-50/40 border-b border-sand-100 flex flex-wrap gap-3 items-center justify-between text-xs">
                  <div className="relative flex-1 min-w-[200px] max-w-sm">
                    <Search size={14} className="absolute left-3 top-2.5 text-gray-400" />
                    <input
                      type="text"
                      placeholder="Buscar por nombre, correo o RUT..."
                      value={busquedaUsuario}
                      onChange={(e) => setBusquedaUsuario(e.target.value)}
                      className="w-full bg-white border border-sand-200 rounded-lg pl-9 pr-3 py-1.5 text-xs"
                    />
                  </div>
                  <div className="flex gap-2">
                    {['Todos', 'Admin', 'Cliente'].map((rol) => (
                      <button
                        key={rol}
                        type="button"
                        onClick={() => setFiltroRolUsuario(rol)}
                        className={`px-3 py-1.5 rounded-lg border font-medium ${
                          filtroRolUsuario === rol
                            ? 'bg-forest-900 text-white border-forest-900'
                            : 'bg-white border-sand-200 text-gray-600 hover:border-forest-700'
                        }`}
                      >
                        {rol}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-sand-50 text-[10px] uppercase font-bold text-gray-500 border-b border-sand-100">
                      <tr>
                        <th className="py-3 px-6">Usuario</th>
                        <th className="py-3 px-6">RUT</th>
                        <th className="py-3 px-6">Rol / Nivel</th>
                        <th className="py-3 px-6">Estado</th>
                        <th className="py-3 px-6 text-right">Acciones</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-sand-100">
                      {usuariosVisibles.map((u) => {
                        const bloqueado = u.is_superuser || u.id_usuario === adminPerfil?.id_usuario;
                        const ocupado = procesandoUsuarioId === u.id_usuario;
                        return (
                          <tr key={u.id_usuario} className="hover:bg-sand-50/50">
                            <td className="py-3.5 px-6">
                              <div className="font-semibold text-gray-800">
                                {u.nombre} {u.apellido_paterno} {u.apellido_materno}
                              </div>
                              <div className="text-[11px] text-gray-500">{u.correo}</div>
                            </td>
                            <td className="py-3.5 px-6 font-mono text-gray-600">{u.rut}</td>
                            <td className="py-3.5 px-6">
                              {u.is_superuser ? (
                                <span className="bg-forest-900 text-white text-[10px] font-semibold px-2.5 py-1 rounded-full">
                                  Superusuario
                                </span>
                              ) : u.rol === 'Admin' ? (
                                <span className="bg-forest-100 text-forest-900 text-[10px] font-semibold px-2.5 py-1 rounded-full">
                                  Personal Admin
                                </span>
                              ) : (
                                <span className="bg-sand-100 text-gray-700 text-[10px] font-semibold px-2.5 py-1 rounded-full">
                                  Cliente
                                </span>
                              )}
                            </td>
                            <td className="py-3.5 px-6">
                              <span className={`text-[10px] font-semibold px-2.5 py-1 rounded-full ${
                                u.estado === 'Activo' ? 'bg-emerald-50 text-emerald-800' : 'bg-red-50 text-red-700'
                              }`}>
                                {u.estado}
                              </span>
                            </td>
                            <td className="py-3.5 px-6 text-right space-x-2">
                              {esSuperusuario && !bloqueado ? (
                                <>
                                  <button
                                    type="button"
                                    disabled={ocupado}
                                    onClick={() =>
                                      manejarActualizarUsuario(u, { rol: u.rol === 'Admin' ? 'Cliente' : 'Admin' })
                                    }
                                    className="border border-forest-800/40 text-forest-900 hover:bg-forest-50 px-2.5 py-1 rounded-lg font-medium disabled:opacity-50"
                                  >
                                    {u.rol === 'Admin' ? 'Pasar a Cliente' : 'Asignar Personal Admin'}
                                  </button>
                                  <button
                                    type="button"
                                    disabled={ocupado}
                                    onClick={() =>
                                      manejarActualizarUsuario(u, {
                                        estado: u.estado === 'Activo' ? 'Inactivo' : 'Activo',
                                      })
                                    }
                                    className="border border-sand-300 text-gray-700 hover:bg-sand-100 px-2.5 py-1 rounded-lg font-medium disabled:opacity-50"
                                  >
                                    {u.estado === 'Activo' ? 'Desactivar' : 'Activar'}
                                  </button>
                                </>
                              ) : (
                                <span className="text-[11px] text-gray-400">
                                  {u.is_superuser ? 'Superusuario protegido' : 'Sin acciones'}
                                </span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

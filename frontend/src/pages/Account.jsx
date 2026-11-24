import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  KeyRound,
  LayoutDashboard,
  LogOut,
  ShieldAlert,
  User,
} from 'lucide-react';
import {
  changePassword,
  deactivateAccount,
  downloadMyDataPdf,
  getConsentStatus,
  getConsultas,
  getProfile,
  hasSession,
  logout,
  updateProfile,
} from '../api.js';

export default function Account() {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState(null);
  const [datosForm, setDatosForm] = useState({
    nombre: '',
    apellido_paterno: '',
    apellido_materno: '',
    telefono: '',
  });
  const [passwordForm, setPasswordForm] = useState({
    password_actual: '',
    password_nueva: '',
  });
  const [cargando, setCargando] = useState(true);
  const [guardandoDatos, setGuardandoDatos] = useState(false);
  const [cambiandoClave, setCambiandoClave] = useState(false);
  const [confirmandoCierreCuenta, setConfirmandoCierreCuenta] = useState(false);
  const [cerrandoCuenta, setCerrandoCuenta] = useState(false);
  const [mensaje, setMensaje] = useState('');
  const [consentimiento, setConsentimiento] = useState(null);
  const [descargandoDatos, setDescargandoDatos] = useState(false);
  const [error, setError] = useState('');
  const [consultas, setConsultas] = useState([]);

  useEffect(() => {
    if (!hasSession()) {
      navigate('/login', { replace: true });
      return;
    }

    let activo = true;
    getConsentStatus()
      .then((data) => activo && setConsentimiento(data))
      .catch(() => {});
    getConsultas()
      .then((data) => activo && setConsultas(data))
      .catch(() => {});
    getProfile()
      .then((data) => {
        if (!activo) return;
        setPerfil(data);
        setDatosForm({
          nombre: data.nombre || '',
          apellido_paterno: data.apellido_paterno || '',
          apellido_materno: data.apellido_materno || '',
          telefono: data.telefono || '',
        });
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

  const guardarDatos = async (event) => {
    event.preventDefault();
    setError('');
    setMensaje('');
    setGuardandoDatos(true);
    try {
      const actualizado = await updateProfile({
        nombre: datosForm.nombre.trim(),
        apellido_paterno: datosForm.apellido_paterno.trim(),
        apellido_materno: datosForm.apellido_materno.trim(),
        telefono: datosForm.telefono.trim(),
      });
      setPerfil(actualizado);
      setMensaje('Tus datos personales fueron actualizados correctamente.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setGuardandoDatos(false);
    }
  };

  const actualizarPassword = async (event) => {
    event.preventDefault();
    setError('');
    setMensaje('');
    setCambiandoClave(true);
    try {
      await changePassword(passwordForm.password_actual, passwordForm.password_nueva);
      setPasswordForm({ password_actual: '', password_nueva: '' });
      setMensaje('Contraseña actualizada correctamente.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setCambiandoClave(false);
    }
  };

  const manejarCerrarSesion = async () => {
    setError('');
    await logout();
    navigate('/');
  };

  const descargarMisDatos = async () => {
    setError('');
    setDescargandoDatos(true);
    try {
      await downloadMyDataPdf();
      setMensaje('Descargamos una copia en PDF de tus datos personales.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setDescargandoDatos(false);
    }
  };

  const manejarDesactivarCuenta = async () => {
    setError('');
    setCerrandoCuenta(true);
    try {
      await deactivateAccount();
      navigate('/', { replace: true });
    } catch (requestError) {
      setError(requestError.message);
      setCerrandoCuenta(false);
    }
  };

  const esAdmin = perfil?.rol === 'Admin';

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased">
      <header className="bg-white border-b border-sand-200">
        <div className="max-w-5xl mx-auto px-6 h-20 flex items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-2 text-forest-900">
            <span className="w-8 h-8 rounded-full bg-forest-100 flex items-center justify-center text-forest-800">
              🍃
            </span>
            <span className="font-serif text-xl font-bold tracking-tight">Altos de carpay</span>
          </Link>

          <div className="flex items-center gap-3 text-xs">
            <Link
              to="/"
              className="text-gray-600 hover:text-forest-900 inline-flex items-center gap-1.5 px-3 py-2 rounded-full hover:bg-sand-100"
            >
              <ArrowLeft size={14} /> Inicio
            </Link>

            {esAdmin ? (
              <Link
                to="/panel"
                className="bg-forest-800 text-white font-semibold inline-flex items-center gap-1.5 px-4 py-2 rounded-full hover:bg-forest-900"
              >
                <LayoutDashboard size={14} /> Volver al Panel Admin
              </Link>
            ) : (
              <Link
                to="/mis-reservas"
                className="bg-forest-800 text-white font-semibold inline-flex items-center gap-1.5 px-4 py-2 rounded-full hover:bg-forest-900"
              >
                <Calendar size={14} /> Mis reservas
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-10 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sand-200 pb-6">
          <div>
            <span className="text-xs uppercase font-semibold tracking-wider text-forest-700">
              {esAdmin ? 'Cuenta administrativa' : 'Área de clientes'}
            </span>
            <h1 className="font-serif text-3xl text-forest-900 mt-1">Mi cuenta</h1>
            {perfil && (
              <p className="text-xs text-gray-500 mt-1">
                {perfil.correo} · RUT {perfil.rut} · Rol: <strong>{perfil.rol}</strong>
              </p>
            )}
          </div>

          <button
            type="button"
            onClick={manejarCerrarSesion}
            className="inline-flex items-center gap-2 border border-sand-300 bg-white text-gray-700 hover:bg-sand-100 text-xs font-semibold px-4 py-2.5 rounded-full self-start"
          >
            <LogOut size={14} /> Cerrar sesión
          </button>
        </div>

        {error && (
          <div role="alert" className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-3 rounded-xl">
            {error}
          </div>
        )}
        {mensaje && (
          <div role="status" className="bg-forest-50 border border-forest-200 text-forest-900 text-xs px-4 py-3 rounded-xl">
            {mensaje}
          </div>
        )}

        {cargando ? (
          <p className="text-sm text-gray-500">Cargando tu perfil…</p>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Datos personales */}
            <form
              onSubmit={guardarDatos}
              className="lg:col-span-7 bg-white p-6 md:p-8 rounded-3xl border border-sand-200 shadow-sm space-y-4"
            >
              <div className="flex items-center gap-2 text-forest-900">
                <User size={18} />
                <h2 className="font-serif text-xl font-semibold">Datos personales</h2>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <label className="block text-gray-600">
                  Nombre
                  <input
                    type="text"
                    required
                    value={datosForm.nombre}
                    onChange={(e) => setDatosForm((f) => ({ ...f, nombre: e.target.value }))}
                    className="w-full border border-sand-200 rounded-xl p-2.5 mt-1 text-sm bg-sand-50/40"
                  />
                </label>

                <label className="block text-gray-600">
                  Apellido paterno
                  <input
                    type="text"
                    required
                    value={datosForm.apellido_paterno}
                    onChange={(e) => setDatosForm((f) => ({ ...f, apellido_paterno: e.target.value }))}
                    className="w-full border border-sand-200 rounded-xl p-2.5 mt-1 text-sm bg-sand-50/40"
                  />
                </label>

                <label className="block text-gray-600">
                  Apellido materno
                  <input
                    type="text"
                    required
                    value={datosForm.apellido_materno}
                    onChange={(e) => setDatosForm((f) => ({ ...f, apellido_materno: e.target.value }))}
                    className="w-full border border-sand-200 rounded-xl p-2.5 mt-1 text-sm bg-sand-50/40"
                  />
                </label>

                <label className="block text-gray-600">
                  Teléfono
                  <input
                    type="tel"
                    placeholder="+56912345678"
                    value={datosForm.telefono}
                    onChange={(e) => setDatosForm((f) => ({ ...f, telefono: e.target.value }))}
                    className="w-full border border-sand-200 rounded-xl p-2.5 mt-1 text-sm bg-sand-50/40"
                  />
                </label>
              </div>

              <button
                type="submit"
                disabled={guardandoDatos}
                className="bg-forest-800 text-white text-xs font-semibold px-5 py-2.5 rounded-full hover:bg-forest-900 disabled:opacity-50"
              >
                {guardandoDatos ? 'Guardando…' : 'Guardar cambios'}
              </button>
            </form>

            {/* Contraseña y cierre de cuenta */}
            <div className="lg:col-span-5 space-y-6">
              <form
                onSubmit={actualizarPassword}
                className="bg-white p-6 rounded-3xl border border-sand-200 shadow-sm space-y-4"
              >
                <div className="flex items-center gap-2 text-forest-900">
                  <KeyRound size={18} />
                  <h2 className="font-serif text-xl font-semibold">Cambiar contraseña</h2>
                </div>

                <label className="block text-xs text-gray-600">
                  Contraseña actual
                  <input
                    type="password"
                    required
                    value={passwordForm.password_actual}
                    onChange={(e) => setPasswordForm((f) => ({ ...f, password_actual: e.target.value }))}
                    className="w-full border border-sand-200 rounded-xl p-2.5 mt-1 text-sm bg-sand-50/40"
                  />
                </label>

                <label className="block text-xs text-gray-600">
                  Nueva contraseña (mín. 10 caracteres)
                  <input
                    type="password"
                    required
                    minLength={10}
                    value={passwordForm.password_nueva}
                    onChange={(e) => setPasswordForm((f) => ({ ...f, password_nueva: e.target.value }))}
                    className="w-full border border-sand-200 rounded-xl p-2.5 mt-1 text-sm bg-sand-50/40"
                  />
                </label>

                <button
                  type="submit"
                  disabled={cambiandoClave}
                  className="bg-forest-800 text-white text-xs font-semibold px-5 py-2.5 rounded-full hover:bg-forest-900 disabled:opacity-50"
                >
                  {cambiandoClave ? 'Actualizando…' : 'Actualizar contraseña'}
                </button>
              </form>

              {perfil?.rol !== 'Admin' && (
                <section id="mis-consultas" className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <h2 className="font-serif text-lg font-semibold">Mis consultas</h2>
                    <Link to="/#contacto" className="text-xs font-semibold text-forest-800 hover:underline">
                      Nueva consulta
                    </Link>
                  </div>
                  {consultas.length === 0 && (
                    <p className="text-xs text-gray-600">Aún no has enviado consultas.</p>
                  )}
                  <ul className="space-y-3">
                    {consultas.map((c) => (
                      <li key={c.id_consulta} className="rounded-2xl border border-sand-200 bg-sand-50/60 p-4 text-xs space-y-2">
                        <div className="flex justify-between gap-2">
                          <span className="font-semibold text-forest-900">{c.asunto}</span>
                          <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold shrink-0 ${
                            c.estado === 'Respondida' ? 'bg-forest-100 text-forest-800' : 'bg-amber-100 text-amber-800'
                          }`}>
                            {c.estado}
                          </span>
                        </div>
                        <p className="text-gray-700 whitespace-pre-line">{c.mensaje}</p>
                        <p className="text-[11px] text-gray-400">Enviada el {new Date(c.fecha_envio).toLocaleString('es-CL')}</p>
                        {c.respuesta ? (
                          <div className="bg-white border border-forest-200 rounded-xl p-3">
                            <span className="block text-[11px] font-semibold text-forest-800 mb-1">
                              Respuesta de Altos de Carpay · {new Date(c.fecha_respuesta).toLocaleString('es-CL')}
                            </span>
                            <p className="text-gray-800 whitespace-pre-line">{c.respuesta}</p>
                          </div>
                        ) : (
                          <p className="text-[11px] text-amber-800">Te avisaremos por correo cuando la respondamos.</p>
                        )}
                      </li>
                    ))}
                  </ul>
                </section>
              )}

              <section className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-3">
                <h2 className="font-serif text-lg font-semibold">Mis datos personales</h2>
                <p className="text-xs text-gray-600 leading-relaxed">
                  {consentimiento?.consentimiento_datos
                    ? `Aceptaste la política de datos (versión ${consentimiento.consentimiento_version || 'anterior'}) el ${new Date(consentimiento.consentimiento_datos).toLocaleString('es-CL')}.`
                    : 'Aún no registras tu consentimiento para el tratamiento de tus datos.'}{' '}
                  Conforme a la Ley N° 21.719 puedes descargar una copia de tus datos, corregirlos arriba o cerrar tu cuenta.
                </p>
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={descargarMisDatos}
                    disabled={descargandoDatos}
                    className="bg-forest-800 text-white text-xs font-semibold px-5 py-2.5 rounded-full hover:bg-forest-900 disabled:opacity-50"
                  >
                    {descargandoDatos ? 'Preparando…' : 'Descargar mis datos (PDF)'}
                  </button>
                  <Link to="/privacidad" className="text-xs font-semibold text-forest-800 underline self-center">
                    Ver política de datos
                  </Link>
                </div>
              </section>

              {!esAdmin && (
                <section className="bg-white p-6 rounded-3xl border border-red-200/80 shadow-sm space-y-3">
                  <div className="flex items-center gap-2 text-red-800">
                    <ShieldAlert size={18} />
                    <h2 className="font-serif text-lg font-semibold">Cerrar cuenta</h2>
                  </div>
                  <p className="text-xs text-gray-600 leading-relaxed">
                    Puedes desactivar permanentemente tu cuenta conforme a la Ley N° 21.719. Se cerrarán todas tus sesiones activas.
                  </p>

                  {!confirmandoCierreCuenta ? (
                    <button
                      type="button"
                      onClick={() => setConfirmandoCierreCuenta(true)}
                      className="border border-red-300 text-red-700 hover:bg-red-50 text-xs font-semibold px-4 py-2 rounded-full"
                    >
                      Cerrar mi cuenta
                    </button>
                  ) : (
                    <div className="bg-red-50 border border-red-200 rounded-xl p-3 space-y-2">
                      <p className="text-xs text-red-900 font-medium">
                        ¿Confirmas que deseas desactivar tu cuenta?
                      </p>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          disabled={cerrandoCuenta}
                          onClick={manejarDesactivarCuenta}
                          className="bg-red-700 text-white text-xs font-semibold px-3 py-1.5 rounded-full hover:bg-red-800 disabled:opacity-50"
                        >
                          {cerrandoCuenta ? 'Cerrando…' : 'Sí, cerrar cuenta'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmandoCierreCuenta(false)}
                          className="border border-sand-300 bg-white text-gray-700 text-xs px-3 py-1.5 rounded-full"
                        >
                          Cancelar
                        </button>
                      </div>
                    </div>
                  )}
                </section>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

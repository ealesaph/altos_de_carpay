import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, ShieldCheck } from 'lucide-react';
import { acceptConsent, getPrivacyPolicy, hasSession, logout } from '../api.js';

/**
 * Política de tratamiento de datos personales (Ley N° 21.719).
 * Es pública; si llega desde el inicio de sesión con `state.aceptar`, solicita el consentimiento
 * de la versión vigente antes de continuar.
 */
export default function Privacidad() {
  const navigate = useNavigate();
  const location = useLocation();
  const solicitarAceptacion = Boolean(location.state?.aceptar) && hasSession();
  const destino = location.state?.next || '/';

  const [politica, setPolitica] = useState(null);
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    getPrivacyPolicy()
      .then(setPolitica)
      .catch((requestError) => setError(requestError.message));
  }, []);

  const aceptar = async () => {
    setError('');
    setGuardando(true);
    try {
      await acceptConsent(politica.version);
      navigate(destino, { replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setGuardando(false);
    }
  };

  const rechazar = async () => {
    await logout();
    navigate('/', { replace: true });
  };

  return (
    <div className="min-h-screen bg-sand-50 p-6 md:p-10 font-sans text-gray-800 antialiased">
      <header className="max-w-3xl mx-auto flex justify-between items-center mb-8">
        <Link to="/" className="font-serif text-xl font-bold tracking-tight text-forest-900">
          Altos de carpay
        </Link>
        {!solicitarAceptacion && (
          <Link to="/" className="text-xs text-gray-500 hover:text-forest-900 flex items-center gap-1.5">
            <ArrowLeft size={14} /> Volver al inicio
          </Link>
        )}
      </header>

      <main className="max-w-3xl mx-auto bg-white p-6 md:p-10 rounded-3xl border border-sand-200/80 shadow-sm space-y-6">
        <div className="flex items-center gap-3 text-forest-900">
          <ShieldCheck size={24} />
          <h1 className="font-serif text-2xl font-semibold">
            {politica?.titulo || 'Política de tratamiento de datos personales'}
          </h1>
        </div>

        {solicitarAceptacion && (
          <p role="status" className="text-sm bg-amber-50 border border-amber-200 text-amber-900 rounded-2xl p-4">
            Actualizamos nuestra política conforme a la Ley N° 21.719. Para seguir usando tu cuenta debes leerla y aceptarla.
          </p>
        )}

        {!politica && !error && <p className="text-sm text-gray-500">Cargando política…</p>}

        {politica && (
          <>
            <p className="text-xs text-gray-500">
              Versión {politica.version} · Responsable: {politica.responsable?.nombre} ({politica.responsable?.domicilio}) ·
              Contacto: {politica.responsable?.contacto}
            </p>
            {politica.secciones.map((seccion) => (
              <section key={seccion.titulo} className="space-y-1">
                <h2 className="font-serif text-lg font-semibold text-forest-900">{seccion.titulo}</h2>
                <p className="text-sm leading-relaxed text-gray-700">{seccion.contenido}</p>
              </section>
            ))}
          </>
        )}

        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}

        {solicitarAceptacion && politica && (
          <div className="flex flex-wrap gap-3 pt-2">
            <button
              type="button"
              onClick={aceptar}
              disabled={guardando}
              className="bg-forest-800 text-white text-sm font-semibold px-6 py-3 rounded-full hover:bg-forest-900 disabled:opacity-50"
            >
              {guardando ? 'Guardando…' : 'Acepto la política de datos'}
            </button>
            <button
              type="button"
              onClick={rechazar}
              disabled={guardando}
              className="text-sm font-semibold px-6 py-3 rounded-full border border-red-200 text-red-800 hover:bg-red-50"
            >
              No acepto (cerrar sesión)
            </button>
          </div>
        )}
      </main>
    </div>
  );
}

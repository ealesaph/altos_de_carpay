import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Lock, ArrowLeft } from 'lucide-react';
import { login } from '../api.js';

export default function Login() {
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [credenciales, setCredenciales] = useState({ correo: '', password: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleChange = (e) => {
    setCredenciales({
      ...credenciales,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      const usuario = await login(credenciales.correo, credenciales.password);
      navigate(
        location.state?.next || (usuario.rol === 'Admin' ? '/admin' : '/reservar/espacio'),
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="min-h-screen bg-sand-50 flex flex-col justify-between p-6 md:p-10 font-sans text-gray-800 antialiased selection:bg-forest-100 selection:text-forest-900">
      
      {/* Barra superior */}
      <header className="max-w-6xl w-full mx-auto flex justify-between items-center">
        <Link to="/" className="flex items-center gap-2 text-forest-900 group">
          <span className="w-7 h-7 rounded-full bg-forest-100 flex items-center justify-center text-forest-800 text-sm">
            🍃
          </span>
          <span className="font-serif text-xl font-bold tracking-tight">
            Altos de carpay
          </span>
        </Link>
        <Link 
          to="/" 
          className="text-xs text-gray-500 hover:text-forest-900 flex items-center gap-1.5 transition-colors"
        >
          <ArrowLeft size={14} /> Volver al inicio
        </Link>
      </header>

      {/* Contenedor central (Grid con tarjeta y formulario) */}
      <main className="max-w-5xl w-full mx-auto grid grid-cols-1 md:grid-cols-12 gap-8 items-center my-8">
        
        {/* Lado izquierdo: Tarjeta con fotografía referencial (Fix de dimensiones rígidas para Gecko) */}
        <div className="hidden md:block md:col-span-5 min-h-0 w-full">
          <div className="relative rounded-3xl overflow-hidden h-[540px] w-full shadow-lg bg-sand-100 border border-sand-200">
            <img 
              src="https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=800&q=80" 
              alt="Quincho Altos de Carpay" 
              className="w-full h-full object-cover block"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-forest-950/85 via-forest-950/25 to-transparent flex flex-col justify-end p-8 text-white">
              <span className="text-xl mb-1">🍃</span>
              <h3 className="font-serif text-2xl font-normal leading-snug mb-2">
                El encuentro empieza mucho antes de llegar.
              </h3>
              <p className="text-xs text-gray-300">Naturaleza para celebrar y compartir.</p>
              <span className="text-[10px] text-gray-400 mt-3 block">Imagen ambiental referencial.</span>
            </div>
          </div>
        </div>

        {/* Lado derecho: Formulario de inicio de sesión */}
        <div className="md:col-span-7 bg-white p-8 md:p-12 rounded-3xl border border-sand-200/90 shadow-sm max-w-lg mx-auto w-full">
          <span className="text-[11px] uppercase tracking-widest text-forest-700 font-semibold block mb-2">
            TU ESPACIO EN CARPAY
          </span>
          <h1 className="font-serif text-3xl font-semibold text-forest-900 mb-2">
            Qué bueno verte
          </h1>
          <p className="text-xs text-gray-500 mb-8 leading-relaxed">
            Inicia sesión para continuar con tu reserva o revisar tus próximos encuentros.
          </p>
          {location.state?.registroExitoso && (
            <p className="text-sm text-green-700 mb-4">Cuenta creada. Inicia sesión para continuar.</p>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="text-xs font-semibold text-gray-700 block mb-1.5">
                Correo electrónico
              </label>
              <input 
                name="correo"
                type="email"
                required
                value={credenciales.correo}
                onChange={handleChange}
                placeholder="nombre@ejemplo.cl"
                className="w-full text-sm border border-sand-200 rounded-xl px-4 py-3 focus:outline-none focus:border-forest-800 transition-colors bg-sand-50/20"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-gray-700 block mb-1.5">
                Contraseña
              </label>
              <div className="relative">
                <input 
                  type={mostrarPassword ? "text" : "password"} 
                  name="password"
                  required
                  value={credenciales.password}
                  onChange={handleChange}
                  placeholder="Ingresa tu contraseña" 
                  className="w-full text-sm border border-sand-200 rounded-xl px-4 py-3 pr-11 focus:outline-none focus:border-forest-800 transition-colors bg-sand-50/20"
                />
                <button 
                  type="button" 
                  onClick={() => setMostrarPassword(!mostrarPassword)}
                  className="absolute right-3.5 top-3.5 text-gray-400 hover:text-forest-900 transition-colors"
                >
                  {mostrarPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}

            <button 
              type="submit" 
              disabled={enviando}
              className="w-full bg-forest-800 text-white font-medium text-sm py-3.5 rounded-xl hover:bg-forest-900 transition-colors shadow-sm"
            >
              {enviando ? 'Ingresando…' : 'Iniciar sesión'}
            </button>
          </form>

          <p className="text-[11px] text-gray-400 text-center mt-3">
            Según tu cuenta, accederás a tu espacio de cliente o de administración.
          </p>

          <div className="mt-8 pt-6 border-t border-sand-100 text-center space-y-3">
            <p className="text-xs text-gray-600">
              ¿Aún no tienes cuenta?{' '}
              <Link to="/registro" className="font-semibold text-forest-800 hover:underline">
                Registrarse
              </Link>
            </p>
            <p className="text-[11px] text-gray-400 flex items-center justify-center gap-1.5">
              <Lock size={13} className="text-forest-700" /> Tus reservas y datos solo son visibles en tu cuenta.
            </p>
          </div>
        </div>

      </main>

      {/* Footer minimalista */}
      <footer className="max-w-6xl w-full mx-auto flex flex-col sm:flex-row justify-between items-center text-[11px] text-gray-400 border-t border-sand-200/60 pt-4 gap-2">
        <span>Altos de carpay · La Reina, Santiago</span>
        <div className="flex gap-4">
          <a href="#" className="hover:underline">Privacidad</a>
          <a href="#" className="hover:underline">Condiciones de reserva</a>
        </div>
      </footer>

    </div>
  );
}
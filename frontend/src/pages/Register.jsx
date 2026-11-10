import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, ArrowLeft } from 'lucide-react';

export default function Register() {
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [mostrarConfirm, setMostrarConfirm] = useState(false);
  const [formulario, setFormulario] = useState({
    nombre: '',
    apellidos: '',
    correo: '',
    telefono: '',
    password: '',
    confirmPassword: '',
    terminos: false
  });

  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormulario({
      ...formulario,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (formulario.password !== formulario.confirmPassword) {
      alert("Las contraseñas no coinciden");
      return;
    }
    // Simula registro exitoso y pasa a login o reserva
    navigate('/login');
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

      {/* Contenedor central */}
      <main className="max-w-5xl w-full mx-auto grid grid-cols-1 md:grid-cols-12 gap-8 items-center my-8">
        
        {/* Lado izquierdo: Tarjeta visual (Sendero de eucaliptos) */}
        <div className="hidden md:block md:col-span-5 min-h-0 w-full">
          <div className="relative rounded-3xl overflow-hidden h-[620px] w-full shadow-lg bg-sand-100 border border-sand-200">
            <img 
              src="https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=800&q=80" 
              alt="Sendero Altos de Carpay" 
              className="w-full h-full object-cover block"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-forest-950/85 via-forest-950/20 to-transparent flex flex-col justify-end p-8 text-white">
              <h3 className="font-serif text-2xl font-normal leading-snug mb-2">
                Un lugar para tus próximos recuerdos.
              </h3>
              <p className="text-xs text-gray-300 leading-relaxed">
                Crea tu cuenta para reservar con calma y tener a mano cada detalle de tu encuentro.
              </p>
              
              <ul className="text-xs text-gray-300 space-y-2 mt-4 pt-4 border-t border-white/20">
                <li className="flex items-center gap-2">✓ Consulta tus reservas y áreas incluidas.</li>
                <li className="flex items-center gap-2">✓ Revisa tu saldo y la fecha límite de pago.</li>
                <li className="flex items-center gap-2">✓ Conversa con el equipo sobre tu evento.</li>
              </ul>
              
              <span className="text-[10px] text-gray-400 mt-4 block">Imagen ambiental referencial.</span>
            </div>
          </div>
        </div>

        {/* Lado derecho: Formulario de Registro */}
        <div className="md:col-span-7 bg-white p-8 md:p-10 rounded-3xl border border-sand-200/90 shadow-sm max-w-xl mx-auto w-full">
          <span className="text-[11px] uppercase tracking-widest text-forest-700 font-semibold block mb-1">
            Cuenta de cliente
          </span>
          <h1 className="font-serif text-3xl font-semibold text-forest-900 mb-2">
            Sé parte del encuentro
          </h1>
          <p className="text-xs text-gray-500 mb-6 leading-relaxed">
            Completa tus datos para crear tu cuenta. Los campos marcados con * son obligatorios.
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Nombre y Apellidos */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Nombre *</label>
                <input 
                  type="text" 
                  name="nombre"
                  required
                  value={formulario.nombre}
                  onChange={handleChange}
                  placeholder="Tu nombre" 
                  className="w-full text-sm border border-sand-200 rounded-xl px-3.5 py-2.5 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Apellidos *</label>
                <input 
                  type="text" 
                  name="apellidos"
                  required
                  value={formulario.apellidos}
                  onChange={handleChange}
                  placeholder="Tus apellidos" 
                  className="w-full text-sm border border-sand-200 rounded-xl px-3.5 py-2.5 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                />
              </div>
            </div>

            {/* Correo electrónico */}
            <div>
              <label className="text-xs font-semibold text-gray-700 block mb-1">Correo electrónico *</label>
              <input 
                type="email" 
                name="correo"
                required
                value={formulario.correo}
                onChange={handleChange}
                placeholder="nombre@ejemplo.cl" 
                className="w-full text-sm border border-sand-200 rounded-xl px-3.5 py-2.5 focus:outline-none focus:border-forest-800 bg-sand-50/20"
              />
              <span className="text-[10px] text-gray-400 mt-1 block">Aquí recibirás las confirmaciones de reserva y pago.</span>
            </div>

            {/* Teléfono */}
            <div>
              <label className="text-xs font-semibold text-gray-700 block mb-1">Teléfono (opcional)</label>
              <input 
                type="tel" 
                name="telefono"
                value={formulario.telefono}
                onChange={handleChange}
                placeholder="+56 · Tu número de contacto" 
                className="w-full text-sm border border-sand-200 rounded-xl px-3.5 py-2.5 focus:outline-none focus:border-forest-800 bg-sand-50/20"
              />
            </div>

            {/* Contraseñas */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Contraseña *</label>
                <div className="relative">
                  <input 
                    type={mostrarPassword ? "text" : "password"} 
                    name="password"
                    required
                    value={formulario.password}
                    onChange={handleChange}
                    placeholder="Crea tu contraseña" 
                    className="w-full text-sm border border-sand-200 rounded-xl px-3.5 py-2.5 pr-10 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                  />
                  <button 
                    type="button" 
                    onClick={() => setMostrarPassword(!mostrarPassword)}
                    className="absolute right-3 top-3 text-gray-400 hover:text-forest-900"
                  >
                    {mostrarPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Confirmar contraseña *</label>
                <div className="relative">
                  <input 
                    type={mostrarConfirm ? "text" : "password"} 
                    name="confirmPassword"
                    required
                    value={formulario.confirmPassword}
                    onChange={handleChange}
                    placeholder="Repite tu contraseña" 
                    className="w-full text-sm border border-sand-200 rounded-xl px-3.5 py-2.5 pr-10 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                  />
                  <button 
                    type="button" 
                    onClick={() => setMostrarConfirm(!mostrarConfirm)}
                    className="absolute right-3 top-3 text-gray-400 hover:text-forest-900"
                  >
                    {mostrarConfirm ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
            </div>

            {/* Checkbox términos */}
            <div className="pt-2">
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input 
                  type="checkbox" 
                  name="terminos"
                  required
                  checked={formulario.terminos}
                  onChange={handleChange}
                  className="mt-1 rounded text-forest-800 focus:ring-forest-700" 
                />
                <span className="text-xs text-gray-600 leading-snug">
                  Acepto los términos de uso y he leído la política de privacidad. *
                </span>
              </label>
              <span className="text-[10px] text-gray-400 block mt-1 ml-6">
                Usaremos tus datos para gestionar tu cuenta, tus reservas y las comunicaciones asociadas.
              </span>
            </div>

            <button 
              type="submit" 
              className="w-full bg-forest-800 text-white font-medium text-sm py-3.5 rounded-xl hover:bg-forest-900 transition-colors shadow-sm mt-3"
            >
              Crear mi cuenta de cliente
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-sand-100 text-center">
            <p className="text-xs text-gray-600">
              ¿Ya tienes cuenta?{' '}
              <Link to="/login" className="font-semibold text-forest-800 hover:underline">
                Iniciar sesión
              </Link>
            </p>
            <p className="text-[11px] text-gray-400 mt-2">
              Este registro es solo para clientes. Los accesos de administración son gestionados por el equipo.
            </p>
          </div>
        </div>

      </main>

      {/* Footer minimalista */}
      <footer className="max-w-6xl w-full mx-auto flex flex-col sm:flex-row justify-between items-center text-[11px] text-gray-400 border-t border-sand-200/60 pt-4 gap-2">
        <span>Naturaleza para celebrar y compartir.</span>
        <a href="#" className="hover:underline">Política de privacidad ↗</a>
      </footer>

    </div>
  );
}
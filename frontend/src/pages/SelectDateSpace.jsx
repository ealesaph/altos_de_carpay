import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Calendar as CalendarIcon, Clock, Users, ArrowRight, Check } from 'lucide-react';

export default function SelectDateSpace() {
  const navigate = useNavigate();

  const [espacioSeleccionado, setEspacioSeleccionado] = useState('terraza');
  const [fecha, setFecha] = useState('2026-12-12');
  const [bloque, setBloque] = useState('diurno');
  const [nombreEvento, setNombreEvento] = useState('Un almuerzo en familia');
  const [asistentes, setAsistentes] = useState('30');

  const espacios = {
    terraza: {
      nombre: 'Terraza superior',
      capacidad: 'Hasta 40 personas',
      precioHora: 60000,
      horas: 5,
      total: 300000,
      anticipo: 90000,
      img: 'https://images.unsplash.com/photo-1544984243-ec57ea16fe25?auto=format&fit=crop&w=800&q=80',
      descripcion: 'Parrilla amplia, mesones de roble, cocina equipada y vista al valle.'
    },
    mirador: {
      nombre: 'Mirador del Valle',
      capacidad: 'Hasta 20 personas',
      precioHora: 36000,
      horas: 5,
      total: 180000,
      anticipo: 54000,
      img: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80',
      descripcion: 'Área lounge al aire libre con fogón central y bancas de madera.'
    }
  };

  const actual = espacios[espacioSeleccionado];

  const handleContinuar = (e) => {
    e.preventDefault();
    if (!fecha) {
      alert("Por favor selecciona una fecha para tu evento.");
      return;
    }
    
    navigate('/reservar/pago', {
      state: {
        espacioId: espacioSeleccionado,
        nombreEspacio: actual.nombre,
        precioTotal: actual.total,
        anticipo: actual.anticipo,
        img: actual.img,
        fecha: fecha,
        bloque: bloque === 'diurno' ? '13:00–18:00 (5 horas)' : '18:30–23:30 (5 horas)',
        nombreEvento: nombreEvento,
        asistentes: asistentes
      }
    });
  };

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased selection:bg-forest-100 selection:text-forest-900">
      
      {/* Header con enlace corregido a /mis-reservas */}
      <header className="bg-sand-50/95 backdrop-blur-md border-b border-sand-200/60 sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-forest-900">
            <span className="w-7 h-7 rounded-full bg-forest-100 flex items-center justify-center text-forest-800 text-sm">
              🍃
            </span>
            <span className="font-serif text-xl font-bold tracking-tight">
              Altos de carpay
            </span>
          </Link>

          <div className="flex items-center gap-6 text-xs text-gray-600 font-medium">
            <span className="bg-sand-100 text-forest-900 px-3 py-1.5 rounded-full font-semibold">
              Mi cuenta · Camila
            </span>
            <Link to="/mis-reservas" className="text-forest-800 hover:underline">
              Mis reservas
            </Link>
          </div>
        </div>
      </header>

      {/* Stepper (Paso 1) */}
      <div className="border-b border-sand-200/80 bg-white/50 py-4">
        <div className="max-w-6xl mx-auto px-6 flex items-center justify-center gap-4 sm:gap-10 text-xs">
          <div className="flex items-center gap-2 text-forest-900 font-bold">
            <span className="w-5 h-5 rounded-full bg-forest-800 text-white flex items-center justify-center text-[10px]">
              1
            </span>
            <span>Espacio y fecha</span>
          </div>

          <div className="w-8 h-px bg-gray-200"></div>

          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-gray-200 text-gray-600 flex items-center justify-center text-[10px]">
              2
            </span>
            <span className="hidden sm:inline">Tu cuenta</span>
          </div>

          <div className="w-8 h-px bg-gray-200"></div>

          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-gray-200 text-gray-600 flex items-center justify-center text-[10px]">
              3
            </span>
            <span className="hidden sm:inline">Revisión y pago</span>
          </div>

          <div className="w-8 h-px bg-gray-200"></div>

          <div className="flex items-center gap-2 text-gray-400">
            <span className="w-5 h-5 rounded-full bg-gray-200 text-gray-600 flex items-center justify-center text-[10px]">
              4
            </span>
            <span className="hidden sm:inline">Confirmación</span>
          </div>
        </div>
      </div>

      {/* Contenido Principal */}
      <main className="max-w-6xl mx-auto px-6 py-10">
        <div className="mb-8">
          <span className="text-xs uppercase font-semibold tracking-wider text-forest-700 block mb-1">
            Paso 1 de 4 · Configuración
          </span>
          <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mb-2">
            Selecciona tu espacio y fecha
          </h1>
          <p className="text-xs text-gray-500">
            Define las opciones de tu encuentro para verificar la disponibilidad en tiempo real.
          </p>
        </div>

        <form onSubmit={handleContinuar} className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-7 space-y-6">
            
            {/* 1. Selector de Espacio */}
            <div className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
              <h2 className="font-serif text-xl font-semibold text-forest-900">
                1. Elige el espacio
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div 
                  onClick={() => setEspacioSeleccionado('terraza')}
                  className={`p-4 rounded-2xl border-2 cursor-pointer transition-all ${
                    espacioSeleccionado === 'terraza' 
                      ? 'border-forest-800 bg-forest-50/40 shadow-sm' 
                      : 'border-sand-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex justify-between items-center mb-2">
                    <span className="font-serif font-bold text-forest-900 text-base">Terraza superior</span>
                    <input 
                      type="radio" 
                      name="espacio" 
                      checked={espacioSeleccionado === 'terraza'} 
                      onChange={() => setEspacioSeleccionado('terraza')}
                      className="text-forest-800"
                    />
                  </div>
                  <span className="text-[11px] bg-sand-100 text-forest-900 px-2 py-0.5 rounded-full font-medium">Hasta 40 pers.</span>
                  <div className="text-lg font-bold text-forest-900 mt-3 font-sans">$300.000 <span className="text-xs text-gray-400 font-normal">/ bloque</span></div>
                </div>

                <div 
                  onClick={() => setEspacioSeleccionado('mirador')}
                  className={`p-4 rounded-2xl border-2 cursor-pointer transition-all ${
                    espacioSeleccionado === 'mirador' 
                      ? 'border-forest-800 bg-forest-50/40 shadow-sm' 
                      : 'border-sand-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex justify-between items-center mb-2">
                    <span className="font-serif font-bold text-forest-900 text-base">Mirador del Valle</span>
                    <input 
                      type="radio" 
                      name="espacio" 
                      checked={espacioSeleccionado === 'mirador'} 
                      onChange={() => setEspacioSeleccionado('mirador')}
                      className="text-forest-800"
                    />
                  </div>
                  <span className="text-[11px] bg-sand-100 text-forest-900 px-2 py-0.5 rounded-full font-medium">Hasta 20 pers.</span>
                  <div className="text-lg font-bold text-forest-900 mt-3 font-sans">$180.000 <span className="text-xs text-gray-400 font-normal">/ bloque</span></div>
                </div>
              </div>
            </div>

            {/* 2. Selector de Fecha y Horario */}
            <div className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-5">
              <h2 className="font-serif text-xl font-semibold text-forest-900">
                2. Fecha y bloque de horario
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5 flex items-center gap-1.5">
                    <CalendarIcon size={14} className="text-forest-800" /> Fecha del encuentro *
                  </label>
                  <input 
                    type="date" 
                    required
                    value={fecha}
                    onChange={(e) => setFecha(e.target.value)}
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/20 font-medium text-gray-800"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5 flex items-center gap-1.5">
                    <Clock size={14} className="text-forest-800" /> Bloque de arriendo (5 hrs)
                  </label>
                  <select 
                    value={bloque}
                    onChange={(e) => setBloque(e.target.value)}
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 bg-white focus:outline-none focus:border-forest-800 font-medium text-gray-800"
                  >
                    <option value="diurno">Diurno: 13:00 a 18:00 hrs</option>
                    <option value="vespertino">Vespertino: 18:30 a 23:30 hrs</option>
                  </select>
                </div>
              </div>

              <div className="p-3.5 bg-forest-50/60 rounded-xl border border-sand-200/80 text-xs text-forest-900 flex items-center gap-2">
                <Check size={16} className="text-forest-700 shrink-0" />
                <span>Bloque disponible para reserva inmediata sin conflicto de agenda.</span>
              </div>
            </div>

            {/* 3. Detalles */}
            <div className="bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
              <h2 className="font-serif text-xl font-semibold text-forest-900">
                3. Detalles del encuentro
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5">Nombre o motivo del evento</label>
                  <input 
                    type="text" 
                    value={nombreEvento}
                    onChange={(e) => setNombreEvento(e.target.value)}
                    placeholder="Ej: Cumpleaños familiar" 
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5 flex items-center gap-1.5">
                    <Users size={14} className="text-forest-800" /> Cantidad estimada de asistentes
                  </label>
                  <input 
                    type="number" 
                    value={asistentes}
                    onChange={(e) => setAsistentes(e.target.value)}
                    placeholder="30" 
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                  />
                </div>
              </div>
            </div>

          </div>

          {/* COLUMNA DERECHA */}
          <div className="lg:col-span-5 bg-white p-6 md:p-8 rounded-3xl border border-sand-200/80 shadow-sm space-y-6 sticky top-28">
            <div className="rounded-2xl overflow-hidden h-44 w-full bg-sand-100 border border-sand-200">
              <img 
                src={actual.img} 
                alt={actual.nombre} 
                className="w-full h-full object-cover block"
              />
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <h3 className="font-serif text-2xl font-semibold text-forest-900">
                  {actual.nombre}
                </h3>
                <span className="text-xs text-forest-800 font-semibold bg-forest-100 px-2.5 py-0.5 rounded-full">
                  {actual.capacidad}
                </span>
              </div>
              <p className="text-xs text-gray-500 leading-relaxed">
                {actual.descripcion}
              </p>
            </div>

            <div className="text-xs text-gray-600 border-t border-b border-sand-100 py-3 space-y-1.5">
              <div className="flex justify-between font-medium">
                <span>Fecha seleccionada:</span>
                <span className="text-forest-900 font-bold">{fecha || 'No seleccionada'}</span>
              </div>
              <div className="flex justify-between font-medium">
                <span>Horario:</span>
                <span className="text-forest-900 font-bold">
                  {bloque === 'diurno' ? '13:00 – 18:00 (5 hrs)' : '18:30 – 23:30 (5 hrs)'}
                </span>
              </div>
              <div className="flex justify-between font-medium">
                <span>Asistentes:</span>
                <span className="text-forest-900 font-bold">{asistentes} personas aprox.</span>
              </div>
            </div>

            <div className="space-y-1.5 text-xs text-gray-500">
              <div className="flex justify-between">
                <span>Tarifa arriendo (5 hrs)</span>
                <span className="font-semibold text-gray-700">${actual.total.toLocaleString('es-CL')}</span>
              </div>
              <div className="flex justify-between pt-2 border-t border-sand-100 text-sm font-bold text-forest-900">
                <span>Total estimado</span>
                <span>${actual.total.toLocaleString('es-CL')} CLP</span>
              </div>
              <span className="text-[10px] text-forest-700 block">
                Anticipo para reservar (30%): ${actual.anticipo.toLocaleString('es-CL')}
              </span>
            </div>

            <button 
              type="submit" 
              className="w-full bg-forest-800 text-white font-medium text-sm py-4 rounded-xl hover:bg-forest-900 transition-colors shadow-sm flex items-center justify-center gap-2"
            >
              Continuar a revisión y pago
              <ArrowRight size={16} />
            </button>
          </div>

        </form>

      </main>

      <footer className="bg-forest-900 text-white py-10 border-t border-forest-800 mt-16 text-xs text-gray-400">
        <div className="max-w-6xl mx-auto px-6 flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-2 text-white font-serif font-bold text-base">
            <span>🍃</span> Altos de carpay
          </div>
          <div className="flex gap-6">
            <a href="#" className="hover:underline">Condiciones de reserva</a>
            <a href="#" className="hover:underline">Privacidad</a>
            <a href="#" className="hover:underline">Contacto</a>
          </div>
        </div>
      </footer>

    </div>
  );
}
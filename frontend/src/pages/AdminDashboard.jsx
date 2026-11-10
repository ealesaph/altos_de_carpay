import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  LayoutDashboard, 
  CalendarDays, 
  Tags, 
  MessageSquare, 
  Settings, 
  ExternalLink, 
  LogOut, 
  Plus, 
  Clock, 
  Search, 
  ChevronRight, 
  Send,
  Bell
} from 'lucide-react';

export default function AdminDashboard() {
  const [seccionActiva, setSeccionActiva] = useState('dashboard');
  const [respuestaConsulta, setRespuestaConsulta] = useState(
    'Hola, Camila. El jardín no está incluido en el arriendo. Revisaremos disponibilidad y te enviaremos una cotización antes de agregarlo.'
  );

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
          <Link to="/login" className="text-gray-400 hover:text-red-300 flex items-center gap-2">
            <LogOut size={14} /> Cerrar sesión
          </Link>

          <div className="p-3 rounded-xl bg-forest-950/60 border border-forest-800/60 text-[10px] text-gray-400 leading-relaxed">
            Entorno de ejemplo. Los datos, pagos y respuestas son para pruebas académicas.
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
          
          {/* Header de bienvenida y botón de acción */}
          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
            <div>
              <h1 className="font-serif text-3xl font-bold text-forest-900">
                Cada encuentro, en orden.
              </h1>
              <p className="text-xs text-gray-500 mt-1">
                1 noviembre 2026 · Resumen del catálogo y próximas reservas · Ejemplo
              </p>
            </div>

            <button className="bg-forest-800 text-white text-xs font-semibold px-4 py-2.5 rounded-xl hover:bg-forest-900 transition-colors shadow-sm inline-flex items-center gap-2 self-start sm:self-auto">
              <Plus size={14} />
              Nueva reserva
            </button>
          </div>

          {/* Tarjetas de Métricas (KPIs) */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">3</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Próximas reservas</span>
              <span className="text-[10px] text-gray-400">Diciembre 2026</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">$210.000</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Saldo por cobrar</span>
              <span className="text-[10px] text-forest-700">1 reserva con anticipo</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">2</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Consultas pendientes</span>
              <span className="text-[10px] text-amber-700">Esperando respuesta</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-sand-200/80 shadow-sm">
              <span className="text-3xl font-serif font-bold text-forest-900 block">2</span>
              <span className="text-xs font-semibold text-gray-700 mt-1 block">Espacios publicados</span>
              <span className="text-[10px] text-gray-400">1 borrador de catálogo</span>
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
                  placeholder="Buscar por cliente, evento o reserva..." 
                  className="w-full bg-white border border-sand-200 rounded-lg pl-9 pr-3 py-1.5 text-xs focus:outline-none focus:border-forest-800"
                />
              </div>

              <div className="flex gap-2">
                <span className="bg-white border border-sand-200 px-3 py-1.5 rounded-lg text-gray-600 font-medium">Todas · 3</span>
                <span className="bg-white border border-sand-200 px-3 py-1.5 rounded-lg text-gray-600 font-medium">Con saldo · 1</span>
                <span className="bg-white border border-sand-200 px-3 py-1.5 rounded-lg text-gray-600 font-medium">Diciembre 2026</span>
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
                  {/* Fila 1: Camila Rojas (Con saldo pendiente) */}
                  <tr className="hover:bg-sand-50/60 transition-colors">
                    <td className="py-4 px-6 font-mono text-gray-500">RES-0261</td>
                    <td className="py-4 px-6">
                      <div className="font-semibold text-gray-800">Camila Rojas</div>
                      <div className="text-[11px] text-gray-400">Terraza superior</div>
                    </td>
                    <td className="py-4 px-6 text-gray-600">12 dic · 13–18 h</td>
                    <td className="py-4 px-6 font-medium text-gray-800">$300.000</td>
                    <td className="py-4 px-6 font-bold text-forest-900">$210.000</td>
                    <td className="py-4 px-6">
                      <span className="bg-amber-100 text-amber-900 text-[10px] font-semibold px-2.5 py-1 rounded-full">
                        Saldo pendiente
                      </span>
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button className="text-forest-800 font-semibold hover:underline">Revisar →</button>
                    </td>
                  </tr>

                  {/* Fila 2: Diego Muñoz (Pagado total) */}
                  <tr className="hover:bg-sand-50/60 transition-colors">
                    <td className="py-4 px-6 font-mono text-gray-500">RES-0262</td>
                    <td className="py-4 px-6">
                      <div className="font-semibold text-gray-800">Diego Muñoz</div>
                      <div className="text-[11px] text-gray-400">Mirador</div>
                    </td>
                    <td className="py-4 px-6 text-gray-600">13 dic · 13–18 h</td>
                    <td className="py-4 px-6 font-medium text-gray-800">$180.000</td>
                    <td className="py-4 px-6 font-medium text-gray-500">$0</td>
                    <td className="py-4 px-6">
                      <span className="bg-green-100 text-green-900 text-[10px] font-semibold px-2.5 py-1 rounded-full">
                        Pago completo
                      </span>
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button className="text-forest-800 font-semibold hover:underline">Revisar →</button>
                    </td>
                  </tr>

                  {/* Fila 3: Valentina Soto (Pagado total) */}
                  <tr className="hover:bg-sand-50/60 transition-colors">
                    <td className="py-4 px-6 font-mono text-gray-500">RES-0263</td>
                    <td className="py-4 px-6">
                      <div className="font-semibold text-gray-800">Valentina Soto</div>
                      <div className="text-[11px] text-gray-400">Terraza superior</div>
                    </td>
                    <td className="py-4 px-6 text-gray-600">19 dic · 13–18 h</td>
                    <td className="py-4 px-6 font-medium text-gray-800">$300.000</td>
                    <td className="py-4 px-6 font-medium text-gray-500">$0</td>
                    <td className="py-4 px-6">
                      <span className="bg-green-100 text-green-900 text-[10px] font-semibold px-2.5 py-1 rounded-full">
                        Pago completo
                      </span>
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button className="text-forest-800 font-semibold hover:underline">Revisar →</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            {/* Aviso de caducidad 24 horas */}
            <div className="p-3 bg-amber-50/80 border-t border-amber-200/80 text-[11px] text-amber-900 flex items-center gap-2 px-6">
              <Clock size={13} />
              <span>RES-0261 · Saldo $210.000: Vence el 11 dic, 2026 a las 13:00, 24 h antes del evento. Caduca por impago si no se completa en plazo.</span>
            </div>
          </div>

          {/* GRID INFERIOR: Catálogo/Edición a la izquierda, Consultas y Bloques a la derecha */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            
            {/* LADO IZQUIERDO: Catálogo de espacios y Edición */}
            <div className="lg:col-span-6 space-y-6">
              
              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <div className="flex justify-between items-center">
                  <h3 className="font-serif text-lg font-semibold text-forest-900">Catálogo y precios</h3>
                  <button className="text-xs text-forest-800 font-medium hover:underline">+ Nuevo espacio</button>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 rounded-xl border border-sand-200 bg-sand-50/30">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg overflow-hidden bg-sand-200 shrink-0">
                        <img 
                          src="https://images.unsplash.com/photo-1544984243-ec57ea16fe25?auto=format&fit=crop&w=200&q=80" 
                          alt="Terraza" 
                          className="w-full h-full object-cover block"
                        />
                      </div>
                      <div>
                        <span className="font-semibold text-xs block text-gray-800">Terraza superior</span>
                        <span className="text-[10px] text-gray-400">$60.000 / hora · Bloque 5 h</span>
                      </div>
                    </div>
                    <span className="bg-forest-100 text-forest-900 text-[10px] font-semibold px-2 py-0.5 rounded-full">Publicado</span>
                  </div>

                  <div className="flex items-center justify-between p-3 rounded-xl border border-sand-200 bg-sand-50/30">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg overflow-hidden bg-sand-200 shrink-0">
                        <img 
                          src="https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=200&q=80" 
                          alt="Mirador" 
                          className="w-full h-full object-cover block"
                        />
                      </div>
                      <div>
                        <span className="font-semibold text-xs block text-gray-800">Mirador</span>
                        <span className="text-[10px] text-gray-400">$36.000 / hora · Bloque 5 h</span>
                      </div>
                    </div>
                    <span className="bg-forest-100 text-forest-900 text-[10px] font-semibold px-2 py-0.5 rounded-full">Publicado</span>
                  </div>

                  <div className="flex items-center justify-between p-3 rounded-xl border border-sand-200 bg-sand-50/30">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg overflow-hidden bg-sand-200 shrink-0">
                        <div className="w-full h-full bg-sand-200 flex items-center justify-center text-xs">🌿</div>
                      </div>
                      <div>
                        <span className="font-semibold text-xs block text-gray-800">Zona 1</span>
                        <span className="text-[10px] text-gray-400">Tarifa y capacidad por definir</span>
                      </div>
                    </div>
                    <span className="bg-gray-100 text-gray-600 text-[10px] font-semibold px-2 py-0.5 rounded-full">Borrador</span>
                  </div>
                </div>
              </div>

              {/* Formulario de edición rápida del espacio */}
              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <div className="flex justify-between items-center">
                  <h3 className="font-serif text-lg font-semibold text-forest-900">Editar 'Terraza superior'</h3>
                  <span className="text-[10px] bg-sand-100 text-gray-600 px-2 py-0.5 rounded-full">En edición</span>
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div>
                    <label className="text-gray-600 block mb-1">Nombre del espacio</label>
                    <input type="text" defaultValue="Terraza superior" className="w-full border border-sand-200 rounded-lg p-2" />
                  </div>
                  <div>
                    <label className="text-gray-600 block mb-1">Capacidad referencial</label>
                    <input type="text" defaultValue="30 personas" className="w-full border border-sand-200 rounded-lg p-2" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div>
                    <label className="text-gray-600 block mb-1">Tarifa por hora · CLP</label>
                    <input type="text" defaultValue="$60.000" className="w-full border border-sand-200 rounded-lg p-2" />
                  </div>
                  <div>
                    <label className="text-gray-600 block mb-1">Duración del bloque</label>
                    <input type="text" defaultValue="5 horas" className="w-full border border-sand-200 rounded-lg p-2" />
                  </div>
                </div>

                <div className="text-xs text-forest-900 font-semibold pt-1">
                  Total del bloque: $300.000 CLP · Ejemplo
                </div>

                <div className="flex gap-3 pt-2">
                  <button className="bg-forest-800 text-white text-xs font-semibold px-4 py-2 rounded-xl hover:bg-forest-900">
                    Guardar cambios
                  </button>
                  <button className="border border-sand-200 text-gray-600 text-xs px-4 py-2 rounded-xl hover:bg-sand-50">
                    Archivar espacio
                  </button>
                </div>
              </div>

            </div>

            {/* LADO DERECHO: Consultas de clientes y Disponibilidad */}
            <div className="lg:col-span-6 space-y-6">
              
              {/* Caja de Consultas */}
              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <div className="flex justify-between items-center">
                  <h3 className="font-serif text-lg font-semibold text-forest-900">Consultas</h3>
                  <span className="text-[10px] bg-amber-100 text-amber-900 px-2 py-0.5 rounded-full font-semibold">2 pendientes</span>
                </div>

                <div className="p-3 rounded-xl border border-sand-200 bg-sand-50/20 text-xs space-y-1">
                  <div className="flex justify-between font-semibold text-gray-800">
                    <span>Camila Rojas · RES-0261</span>
                  </div>
                  <p className="text-gray-600 italic">"¿Podemos usar el jardín durante el almuerzo? Me gustaría conocer su disponibilidad y valor."</p>
                </div>

                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1">Respuesta a Camila</label>
                  <textarea 
                    rows="3" 
                    value={respuestaConsulta}
                    onChange={(e) => setRespuestaConsulta(e.target.value)}
                    className="w-full text-xs border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/20"
                  ></textarea>
                </div>

                <button 
                  onClick={() => alert("Respuesta enviada")}
                  className="w-full bg-forest-800 text-white text-xs font-semibold py-2.5 rounded-xl hover:bg-forest-900 transition-colors flex items-center justify-center gap-2"
                >
                  <Send size={13} />
                  Enviar respuesta
                </button>
              </div>

              {/* Disponibilidad por bloque */}
              <div className="bg-white p-6 rounded-3xl border border-sand-200/80 shadow-sm space-y-4">
                <h3 className="font-serif text-lg font-semibold text-forest-900">Disponibilidad por bloque</h3>
                
                <div className="text-xs text-gray-500">
                  <span className="block font-medium text-gray-700">Terraza superior · 12 dic 2026</span>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between items-center p-3 rounded-xl border border-amber-200 bg-amber-50/40">
                    <div>
                      <span className="font-bold text-gray-800 block">13:00–18:00</span>
                      <span className="text-[10px] text-gray-500">RES-0261 · No admite otra reserva</span>
                    </div>
                    <span className="bg-amber-100 text-amber-900 text-[10px] font-semibold px-2 py-0.5 rounded-full">Reservado</span>
                  </div>

                  <div className="flex justify-between items-center p-3 rounded-xl border border-green-200 bg-green-50/40">
                    <div>
                      <span className="font-bold text-gray-800 block">18:00–23:00</span>
                      <span className="text-[10px] text-gray-500">Bloque vespertino disponible</span>
                    </div>
                    <span className="bg-green-100 text-green-900 text-[10px] font-semibold px-2 py-0.5 rounded-full">Disponible</span>
                  </div>
                </div>

                <p className="text-[10px] text-gray-400">
                  No se permiten bloques superpuestos para un mismo espacio.
                </p>
              </div>

            </div>

          </div>

        </main>

      </div>

    </div>
  );
}
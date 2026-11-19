import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getSectors } from '../api.js';
import { 
  Sparkles, 
  MapPin, 
  Clock, 
  ShieldCheck, 
  ChevronDown, 
  ChevronUp, 
  ArrowRight,
  Flame,
  Car,
  Compass
} from 'lucide-react';

export default function Home() {
  const [faqAbierta, setFaqAbierta] = useState(null);
  const [sectores, setSectores] = useState([]);
  const [errorCatalogo, setErrorCatalogo] = useState('');
  const [cargandoCatalogo, setCargandoCatalogo] = useState(true);

  useEffect(() => {
    let activo = true;
    getSectors()
      .then((catalogo) => {
        if (activo) setSectores(catalogo);
      })
      .catch((requestError) => {
        if (activo) setErrorCatalogo(requestError.message);
      })
      .finally(() => {
        if (activo) setCargandoCatalogo(false);
      });

    return () => {
      activo = false;
    };
  }, []);

  const sectoresDisponibles = sectores.filter((sector) => sector.estado === 'Disponible');

  const toggleFaq = (index) => {
    setFaqAbierta(faqAbierta === index ? null : index);
  };

  const preguntasFrecuentes = [
    {
      q: "¿Cuándo se considera confirmada la reserva?",
      a: "La reserva queda confirmada una vez recibido el comprobante de transferencia o pago simulado del anticipo correspondiente al 30% del valor total."
    },
    {
      q: "¿Qué sucede en caso de lluvia?",
      a: "Nuestros espacios principales disponen de amplias zonas techadas y resguardadas para garantizar que el encuentro se desarrolle sin inconvenientes."
    },
    {
      q: "¿Se permite música en vivo o parlantes?",
      a: "Sí, permitimos música a volumen moderado dentro del recinto, respetando siempre los horarios de tranquilidad acordados."
    },
    {
      q: "¿Puedo agregar horas extra el día del evento?",
      a: "El arriendo contempla bloques de 5 horas. La adición de horas extras estará siempre sujeta a la disponibilidad del recinto posterior a tu bloque."
    },
    {
      q: "¿Tienen estacionamiento privado?",
      a: "Disponemos de estacionamientos interiores amplios y cerrados dentro de la propiedad para la seguridad de todos los asistentes."
    }
  ];

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased selection:bg-forest-100 selection:text-forest-900">
      
      {/* 1. NAVEGACIÓN SUPERIOR */}
      <header className="sticky top-0 z-50 bg-sand-50/95 backdrop-blur-md border-b border-sand-200/60">
        <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
          
          <Link to="/" className="flex items-center gap-2.5 text-forest-900 group">
            <span className="w-8 h-8 rounded-full bg-forest-100 flex items-center justify-center text-forest-800 font-serif font-bold text-lg group-hover:bg-forest-800 group-hover:text-white transition-colors">
              🍃
            </span>
            <span className="font-serif text-2xl font-bold tracking-tight">
              Altos de carpay
            </span>
          </Link>

          <nav className="hidden md:flex items-center gap-7 text-xs font-medium text-gray-600">
            <a href="#espacios" className="hover:text-forest-900 transition-colors">Espacios</a>
            <a href="#experiencia" className="hover:text-forest-900 transition-colors">Experiencia</a>
            <a href="#pasos" className="hover:text-forest-900 transition-colors">Cómo es</a>
            <a href="#faq" className="hover:text-forest-900 transition-colors">Preguntas</a>
            <a href="#contacto" className="hover:text-forest-900 transition-colors">Contacto</a>
          </nav>

          <div className="flex items-center gap-3 sm:gap-4">
            <Link 
              to="/login" 
              className="text-xs font-medium text-gray-700 hover:text-forest-900 px-3 py-1.5 rounded-full hover:bg-sand-100 transition-colors"
            >
              Mi cuenta
            </Link>

            <a 
              href="#espacios" 
              className="bg-forest-800 text-white text-xs font-semibold uppercase tracking-wider px-5 py-2.5 rounded-full hover:bg-forest-900 transition-all shadow-sm active:scale-95"
            >
              Explorar espacios
            </a>
          </div>
        </div>
      </header>

      {/* 2. HERO PRINCIPAL */}
      <section className="max-w-6xl mx-auto px-6 pt-12 pb-16">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
          
          <div className="lg:col-span-6 space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-forest-100 text-forest-800 text-xs font-semibold tracking-wider uppercase">
              <Sparkles size={13} />
              Encuentros al aire libre
            </div>
            
            <h1 className="font-serif text-5xl md:text-6xl text-forest-900 leading-[1.12] tracking-tight">
              Un lugar para <br />
              estar juntos.
            </h1>
            
            <p className="text-gray-600 text-base md:text-lg max-w-lg leading-relaxed">
              Un entorno natural único diseñado para celebrar y compartir momentos memorables con familia y amigos, rodeado de calma y naturaleza en La Reina.
            </p>

            <div className="flex flex-wrap items-center gap-4 pt-2">
              <Link 
                to="/reservar/espacio" 
                className="bg-forest-800 text-white text-sm font-medium px-6 py-3.5 rounded-full hover:bg-forest-900 transition-all shadow-sm inline-flex items-center gap-2"
              >
                Ver espacios disponibles
                <ArrowRight size={15} />
              </Link>
              <a 
                href="#contacto" 
                className="border border-sand-200 text-gray-700 bg-white/70 text-sm font-medium px-6 py-3.5 rounded-full hover:bg-white hover:border-gray-300 transition-all"
              >
                Consultar fecha
              </a>
            </div>

            <div className="pt-2 flex items-center gap-2 text-xs text-gray-500">
              <ShieldCheck size={16} className="text-forest-700" />
              <span>Espacios privados de arriendo exclusivo por bloque horario.</span>
            </div>
          </div>

          <div className="lg:col-span-6 min-h-0 w-full">
            <div className="relative rounded-3xl overflow-hidden shadow-xl border-4 border-white h-72 sm:h-80 md:h-[420px] w-full bg-sand-100">
              <img 
                src="https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=80" 
                alt="Quincho y pérgola Altos de Carpay" 
                className="w-full h-full object-cover block"
                loading="eager"
              />
              <div className="absolute bottom-3 right-4 bg-forest-900/80 backdrop-blur-sm text-white text-[11px] px-3 py-1 rounded-full">
                La Reina, Santiago
              </div>
            </div>
          </div>

        </div>
      </section>

      {/* 3. BARRA DE BENEFICIOS */}
      <section className="border-y border-sand-200/80 bg-forest-50/70 py-6">
        <div className="max-w-6xl mx-auto px-6 grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="flex items-center justify-center md:justify-start gap-3 text-sm text-forest-900 font-medium">
            <div className="w-10 h-10 rounded-full bg-forest-100 flex items-center justify-center text-forest-800">
              <Compass size={18} />
            </div>
            <span>Entorno 100% natural, privado y aislado del ruido</span>
          </div>

          <div className="flex items-center justify-center md:justify-start gap-3 text-sm text-forest-900 font-medium">
            <div className="w-10 h-10 rounded-full bg-forest-100 flex items-center justify-center text-forest-800">
              <Flame size={18} />
            </div>
            <span>Quinchos y parrillas completamente equipados</span>
          </div>

          <div className="flex items-center justify-center md:justify-start gap-3 text-sm text-forest-900 font-medium">
            <div className="w-10 h-10 rounded-full bg-forest-100 flex items-center justify-center text-forest-800">
              <Car size={18} />
            </div>
            <span>Estacionamiento interior cerrado y seguro</span>
          </div>
        </div>
      </section>

      {/* 4. CATÁLOGO DE ESPACIOS */}
      <section id="espacios" className="max-w-6xl mx-auto px-6 py-20">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-12 gap-4">
          <div>
            <span className="text-xs uppercase font-semibold tracking-widest text-forest-700 block mb-1">
              Catálogo disponible
            </span>
            <h2 className="font-serif text-3xl md:text-4xl text-forest-900">
              Elige dónde crear recuerdos.
            </h2>
          </div>
          <p className="text-xs text-gray-500 max-w-xs">
            Valores referenciales con modalidad de anticipo del 30% para asegurar tu fecha.
          </p>
        </div>

        {cargandoCatalogo && <p role="status">Cargando espacios disponibles…</p>}
        {errorCatalogo && <p role="alert" className="text-red-700">{errorCatalogo}</p>}
        {!cargandoCatalogo && !errorCatalogo && !sectoresDisponibles.length && (
          <p>No hay espacios disponibles en este momento.</p>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {sectoresDisponibles.map((sector) => (
            <article key={sector.id_sector} className="bg-white rounded-3xl overflow-hidden border border-sand-200/80 shadow-sm hover:shadow-md transition-all flex flex-col">
              <div className="h-64 w-full overflow-hidden bg-sand-100">
                {sector.fotografias?.[0] ? (
                  <img
                    src={sector.fotografias[0]}
                    alt={sector.nombre}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-forest-100 to-sand-200 text-forest-700">
                    <Compass size={56} aria-hidden="true" />
                  </div>
                )}
              </div>

              <div className="p-7 flex-1 flex flex-col justify-between">
                <div>
                  <h3 className="font-serif text-2xl font-semibold text-forest-900 mb-2">
                    {sector.nombre}
                  </h3>
                  <p className="text-sm text-gray-600 leading-relaxed mb-6">
                    {sector.descripcion || 'Consulta la disponibilidad y duración al iniciar tu reserva.'}
                  </p>
                  <span className="text-[11px] bg-sand-100 text-gray-700 px-2.5 py-1 rounded-md">
                    Disponible
                  </span>
                </div>

                <div className="pt-5 mt-6 border-t border-gray-100 flex items-center justify-between gap-4">
                  <div>
                    <span className="text-[11px] uppercase tracking-wider text-gray-400 block font-semibold">
                      Valor por hora
                    </span>
                    <span className="text-xl font-bold text-forest-900">
                      ${Number(sector.valor_hora).toLocaleString('es-CL')} CLP
                    </span>
                    <span className="text-[11px] text-forest-700 block mt-0.5">
                      Anticipo calculado al reservar
                    </span>
                  </div>
                  <Link
                    to="/reservar/espacio"
                    className="bg-forest-100 text-forest-900 hover:bg-forest-800 hover:text-white px-4 py-2.5 rounded-full text-xs font-semibold transition-colors"
                  >
                    Reservar espacio
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>

      {/* 5. SECCIÓN EXPERIENCIA */}
      <section id="experiencia" className="bg-forest-100/50 py-20 border-y border-sand-200/80">
        <div className="max-w-6xl mx-auto px-6 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          
          <div className="lg:col-span-5 space-y-4">
            <span className="text-xs uppercase font-semibold tracking-widest text-forest-700">
              La experiencia Carpay
            </span>
            <h2 className="font-serif text-3xl md:text-4xl text-forest-900 leading-tight">
              Respirar aire puro. <br /> Compartir sin prisa.
            </h2>
            <p className="text-sm text-gray-600 leading-relaxed">
              Diseñamos cada espacio para que tu única preocupación sea disfrutar con los tuyos. Olvídate de coordinaciones engorrosas y celebra en un entorno natural preparado para recibirte.
            </p>
          </div>

          <div className="lg:col-span-4 min-h-0 w-full">
            <div className="rounded-2xl overflow-hidden h-72 w-full shadow-sm bg-sand-100">
              <img 
                src="https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=800&q=80" 
                alt="Sendero verde natural" 
                className="w-full h-full object-cover block"
              />
            </div>
          </div>

          <div className="lg:col-span-3 min-h-0 w-full">
            <div className="rounded-2xl overflow-hidden h-72 w-full shadow-sm bg-sand-100">
              <img 
                src="https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=600&q=80" 
                alt="Mesa decorada al aire libre" 
                className="w-full h-full object-cover block"
              />
            </div>
          </div>

        </div>
      </section>

      {/* 6. PASO A PASO */}
      <section id="pasos" className="max-w-6xl mx-auto px-6 py-20">
        <div className="flex flex-col md:flex-row justify-between md:items-end mb-12 gap-4">
          <div>
            <span className="text-xs uppercase font-semibold tracking-widest text-forest-700 block mb-1">
              Flujo transparente
            </span>
            <h2 className="font-serif text-3xl md:text-4xl text-forest-900">
              Tu próximo encuentro, paso a paso.
            </h2>
          </div>
          <Link 
            to="/reservar/espacio" 
            className="bg-forest-800 text-white text-xs font-semibold uppercase tracking-wider px-5 py-2.5 rounded-full hover:bg-forest-900 self-start md:self-auto transition-colors"
          >
            Iniciar reserva
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="p-7 bg-white rounded-3xl border border-sand-200/80 shadow-sm space-y-3">
            <span className="text-3xl font-serif text-forest-800 font-bold block">01</span>
            <h3 className="text-lg font-semibold text-forest-900">Elige tu espacio y fecha</h3>
            <p className="text-xs text-gray-600 leading-relaxed">
              Elige un espacio disponible en el catálogo y selecciona la fecha y el horario de tu reunión.
            </p>
          </div>

          <div className="p-7 bg-white rounded-3xl border border-sand-200/80 shadow-sm space-y-3">
            <span className="text-3xl font-serif text-forest-800 font-bold block">02</span>
            <h3 className="text-lg font-semibold text-forest-900">Abona el 30% de anticipo</h3>
            <p className="text-xs text-gray-600 leading-relaxed">
              Bloquea tu fecha de inmediato pagando solo el 30% del valor total mediante el simulador o transferencia.
            </p>
          </div>

          <div className="p-7 bg-white rounded-3xl border border-sand-200/80 shadow-sm space-y-3">
            <span className="text-3xl font-serif text-forest-800 font-bold block">03</span>
            <h3 className="text-lg font-semibold text-forest-900">Liquida 24h antes y disfruta</h3>
            <p className="text-xs text-gray-600 leading-relaxed">
              Completa el 70% restante hasta 24 horas antes del evento. Llegarás con todo coordinado para disfrutar.
            </p>
          </div>
        </div>
      </section>

      {/* 7. PREGUNTAS FRECUENTES */}
      <section id="faq" className="bg-forest-50/80 py-20 border-t border-sand-200/80">
        <div className="max-w-6xl mx-auto px-6 grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
          
          <div className="lg:col-span-5 space-y-3">
            <span className="text-xs uppercase font-semibold tracking-widest text-forest-700">Dudas habituales</span>
            <h2 className="font-serif text-3xl md:text-4xl text-forest-900 leading-tight">
              Todo claro, <br /> desde el comienzo.
            </h2>
            <p className="text-sm text-gray-600 leading-relaxed">
              Queremos que tu experiencia sea totalmente transparente. Si tienes una consulta particular sobre horarios o condiciones, no dudes en escribirnos.
            </p>
          </div>

          <div className="lg:col-span-7 space-y-3.5">
            {preguntasFrecuentes.map((faq, index) => (
              <div 
                key={index} 
                className="bg-white rounded-2xl border border-sand-200/90 overflow-hidden transition-all duration-200"
              >
                <button
                  type="button"
                  onClick={() => toggleFaq(index)}
                  className="w-full p-5 text-left flex justify-between items-center gap-4 hover:bg-sand-50/50"
                >
                  <span className="text-sm font-semibold text-forest-900">{faq.q}</span>
                  <span className="text-forest-700 shrink-0">
                    {faqAbierta === index ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                  </span>
                </button>
                {faqAbierta === index && (
                  <div className="px-5 pb-5 pt-1 text-xs text-gray-600 border-t border-sand-100 leading-relaxed">
                    {faq.a}
                  </div>
                )}
              </div>
            ))}
          </div>

        </div>
      </section>

      {/* 8. FORMULARIO DE CONTACTO */}
      <section id="contacto" className="max-w-6xl mx-auto px-6 py-20">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12">
          
          <div className="lg:col-span-5 space-y-6">
            <div>
              <span className="text-xs uppercase font-semibold tracking-widest text-forest-700 block mb-1">
                Atención directa
              </span>
              <h2 className="font-serif text-4xl text-forest-900">Conversemos.</h2>
            </div>
            
            <p className="text-sm text-gray-600 leading-relaxed">
              Cuéntanos qué tienes en mente: un almuerzo familiar, un bautizo o una jornada corporativa de equipo. Te responderemos a la brevedad.
            </p>

            <div className="rounded-2xl overflow-hidden h-48 w-full border border-sand-200 shadow-sm bg-sand-100">
              <img 
                src="https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=800&q=80" 
                alt="Predio Altos de Carpay en La Reina" 
                className="w-full h-full object-cover block"
              />
            </div>

            <div className="text-xs text-gray-600 space-y-2 border-t border-sand-200/80 pt-4">
              <p className="flex items-center gap-2">
                <MapPin size={15} className="text-forest-800" />
                <span>Camino Carpay, La Reina Alta, Santiago</span>
              </p>
              <p className="flex items-center gap-2">
                <Clock size={15} className="text-forest-800" />
                <span>Bloques disponibles: Diurno (13:00 - 18:00) y Vespertino</span>
              </p>
            </div>
          </div>

          <div className="lg:col-span-7 bg-white p-8 md:p-10 rounded-3xl border border-sand-200 shadow-sm">
            <h3 className="font-serif text-xl font-semibold text-forest-900 mb-6">
              Consulta disponibilidad de fecha
            </h3>
            
            <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5">Nombre completo</label>
                  <input 
                    type="text" 
                    name="nombre"
                    placeholder="Ej: Camila Rojas" 
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/30" 
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5">Teléfono / WhatsApp</label>
                  <input 
                    type="tel" 
                    name="telefono"
                    placeholder="+56 9 1234 5678" 
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/30" 
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5">Espacio de interés</label>
                  <select 
                    name="espacio"
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 bg-white focus:outline-none focus:border-forest-800 text-gray-700"
                  >
                    <option value="">
                      {cargandoCatalogo ? 'Cargando espacios…' : 'Selecciona un espacio'}
                    </option>
                    {sectoresDisponibles.map((sector) => (
                      <option key={sector.id_sector} value={sector.id_sector}>
                        {sector.nombre}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-gray-700 block mb-1.5">Fecha tentativa</label>
                  <input 
                    type="date" 
                    name="fecha"
                    className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/30 text-gray-700" 
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1.5">Mensaje o consulta</label>
                <textarea 
                  rows="4" 
                  name="mensaje"
                  placeholder="Detállanos cantidad de asistentes, tipo de celebración o cualquier duda adicional..." 
                  className="w-full text-sm border border-sand-200 rounded-xl p-3 focus:outline-none focus:border-forest-800 bg-sand-50/30"
                ></textarea>
              </div>

              <button 
                type="submit" 
                className="w-full bg-forest-800 text-white font-medium text-sm py-3.5 rounded-xl hover:bg-forest-900 transition-all shadow-sm"
              >
                Enviar consulta
              </button>
            </form>
          </div>

        </div>
      </section>

      {/* 9. FOOTER */}
      <footer className="bg-forest-900 text-white py-14 border-t border-forest-800">
        <div className="max-w-6xl mx-auto px-6 flex flex-col md:flex-row justify-between items-center gap-8">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="font-serif text-2xl font-bold tracking-tight">Altos de carpay</span>
            </div>
            <p className="text-xs text-gray-300 max-w-sm leading-relaxed">
              Plataforma transaccional para la gestión de arriendos, motor de reservas y control de pagos en La Reina, Santiago.
            </p>
          </div>

          <div className="text-xs text-gray-400 text-center md:text-right space-y-1">
            <p>© {new Date().getFullYear()} Altos de carpay. Proyecto Inacap - Ingeniería de Software.</p>
            <p>Cumplimiento de estándares ISO/IEC 25010 y WCAG 2.1.</p>
          </div>
        </div>
      </footer>

    </div>
  );
}
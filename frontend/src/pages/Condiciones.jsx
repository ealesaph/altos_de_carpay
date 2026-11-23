import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Footer from '../components/Footer.jsx';
import { DIAS_DEVOLUCION_PARCIAL, PLAZO_SALDO_HORAS, textoBloques } from '../reglas.js';

/**
 * Condiciones de reserva (observación 1 del cliente).
 * Recoge las reglas definidas en la reunión de revisión; se completará con el texto del contrato de arriendo.
 */
export default function Condiciones() {
  const secciones = [
    {
      titulo: 'Horario de arriendo',
      puntos: [
        `El recinto se arrienda en dos bloques: ${textoBloques()}.`,
        'Se reserva por horas completas y ninguna reserva se extiende después de la medianoche.',
      ],
    },
    {
      titulo: 'Anticipo y saldo',
      puntos: [
        'La reserva se confirma al pagar el anticipo del 30% del valor total.',
        'Si el anticipo no se paga dentro de 24 horas desde que se creó la reserva, la fecha se libera automáticamente.',
        `El saldo debe pagarse hasta ${PLAZO_SALDO_HORAS} horas antes del inicio del evento.`,
      ],
    },
    {
      titulo: 'Cancelaciones y devoluciones',
      puntos: [
        `Si el arrendatario cancela con más de ${DIAS_DEVOLUCION_PARCIAL} días de anticipación, se devuelve todo lo pagado excepto el anticipo.`,
        `Si el arrendatario cancela con ${DIAS_DEVOLUCION_PARCIAL} días o menos de anticipación, no se devuelve lo pagado.`,
        'Si cancela Altos de Carpay, se devuelve el total pagado.',
        'Si el arrendatario no se presenta el día del evento, la reserva se cierra como «No se presentó» y se conserva el anticipo.',
      ],
    },
    {
      titulo: 'Día del evento',
      puntos: [
        'El encargado registra la entrada con el código QR de la reserva desde una hora antes del inicio.',
        'Usar áreas que no forman parte de la reserva puede generar un cobro adicional.',
      ],
    },
  ];

  return (
    <div className="min-h-screen bg-sand-50 text-gray-800 font-sans antialiased">
      <header className="bg-white border-b border-sand-200">
        <div className="max-w-4xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="font-serif text-xl font-bold text-forest-900">Altos de carpay</Link>
          <Link to="/" className="text-sm text-forest-800 hover:underline inline-flex items-center gap-1">
            <ArrowLeft size={14} /> Volver al inicio
          </Link>
        </div>
      </header>
      <main className="max-w-4xl mx-auto px-6 py-10 space-y-6">
        <div>
          <span className="text-xs uppercase font-semibold tracking-wider text-forest-700">Antes de reservar</span>
          <h1 className="font-serif text-3xl md:text-4xl text-forest-900 mt-1">Condiciones de reserva</h1>
          <p className="text-sm text-gray-500 mt-2">
            Al reservar aceptas estas condiciones. Si tienes dudas, escríbenos desde el formulario de contacto.
          </p>
        </div>
        {secciones.map((seccion) => (
          <section key={seccion.titulo} className="bg-white p-6 rounded-3xl border border-sand-200 shadow-sm">
            <h2 className="font-serif text-xl font-semibold text-forest-900 mb-3">{seccion.titulo}</h2>
            <ul className="list-disc pl-5 space-y-2 text-sm text-gray-700 leading-relaxed">
              {seccion.puntos.map((punto) => <li key={punto}>{punto}</li>)}
            </ul>
          </section>
        ))}
      </main>
      <Footer />
    </div>
  );
}

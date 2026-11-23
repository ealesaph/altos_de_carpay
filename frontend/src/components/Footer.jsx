import React from 'react';
import { Link } from 'react-router-dom';

// Pie de página común: los enlaces legales llevan a páginas reales (antes apuntaban a «#»).
export default function Footer() {
  return (
    <footer className="bg-forest-900 text-white py-10 border-t border-forest-800 mt-16 text-xs text-gray-300">
      <div className="max-w-6xl mx-auto px-6 flex flex-col sm:flex-row justify-between items-center gap-4">
        <div className="flex items-center gap-2 text-white font-serif font-bold text-base">
          <span aria-hidden="true">🍃</span> Altos de carpay
        </div>
        <nav aria-label="Información legal" className="flex flex-wrap justify-center gap-6">
          <Link to="/condiciones" className="hover:underline">Condiciones de reserva</Link>
          <Link to="/privacidad" className="hover:underline">Privacidad</Link>
          <Link to="/#contacto" className="hover:underline">Contacto</Link>
        </nav>
      </div>
    </footer>
  );
}

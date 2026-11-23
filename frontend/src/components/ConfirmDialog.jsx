import React, { useCallback, useRef, useState } from 'react';

/**
 * Confirmación antes de cualquier acción irreversible (observación 15 del cliente).
 * Uso: const [dialogo, confirmar] = useConfirmacion();
 *      if (await confirmar({ titulo, mensaje, textoConfirmar })) { ... }
 *      ... y renderizar {dialogo} en la página.
 */
export function useConfirmacion() {
  const [opciones, setOpciones] = useState(null);
  const resolver = useRef(null);

  const confirmar = useCallback((opts) => new Promise((resolve) => {
    resolver.current = resolve;
    setOpciones(opts);
  }), []);

  const cerrar = (respuesta) => {
    setOpciones(null);
    resolver.current?.(respuesta);
    resolver.current = null;
  };

  const dialogo = opciones ? (
    <div
      className="fixed inset-0 z-[100] bg-black/40 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-confirmacion"
      onKeyDown={(e) => e.key === 'Escape' && cerrar(false)}
    >
      <div className="bg-white rounded-3xl shadow-xl max-w-md w-full p-6 space-y-4">
        <h2 id="titulo-confirmacion" className="font-serif text-xl font-semibold text-forest-900">
          {opciones.titulo}
        </h2>
        <div className="text-sm text-gray-700 space-y-2 leading-relaxed">{opciones.mensaje}</div>
        {opciones.contenido}
        <div className="flex flex-col-reverse sm:flex-row justify-end gap-2 pt-2">
          <button
            type="button"
            autoFocus
            onClick={() => cerrar(false)}
            className="px-4 py-2.5 rounded-xl border border-sand-300 text-gray-700 text-sm font-medium hover:bg-sand-50"
          >
            {opciones.textoCancelar || 'Volver'}
          </button>
          <button
            type="button"
            onClick={() => cerrar(true)}
            className={`px-4 py-2.5 rounded-xl text-white text-sm font-semibold ${
              opciones.peligro ? 'bg-red-700 hover:bg-red-800' : 'bg-forest-800 hover:bg-forest-900'
            }`}
          >
            {opciones.textoConfirmar || 'Confirmar'}
          </button>
        </div>
      </div>
    </div>
  ) : null;

  return [dialogo, confirmar];
}

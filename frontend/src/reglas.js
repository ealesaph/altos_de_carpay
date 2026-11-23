// Reglas de arriendo acordadas con el cliente (bitácora de revisión del prototipo, 23-11-2026).
// Deben coincidir con BLOQUES_HORARIOS y las demás constantes de backend/altosDeCarpay/altosDeCarpay/settings.py.
export const BLOQUES_HORARIOS = [
  { nombre: 'Diurno', desde: 13, hasta: 18 },
  { nombre: 'Vespertino', desde: 19, hasta: 24 },
];

export const PLAZO_SALDO_HORAS = 24;
export const DIAS_DEVOLUCION_PARCIAL = 7;

const dosDigitos = (hora) => String(hora % 24).padStart(2, '0');

export function textoBloques() {
  return BLOQUES_HORARIOS.map((b) => `${b.nombre} (${dosDigitos(b.desde)}:00 – ${dosDigitos(b.hasta)}:00)`).join(' y ');
}

// Horas de inicio válidas: cada hora en punto dentro de un bloque.
export function horasDeInicio() {
  return BLOQUES_HORARIOS.flatMap((b) =>
    Array.from({ length: b.hasta - b.desde }, (_, i) => ({
      valor: `${dosDigitos(b.desde + i)}:00`,
      etiqueta: `${dosDigitos(b.desde + i)}:00 · bloque ${b.nombre.toLowerCase()}`,
    })),
  );
}

// Máximo de horas que caben desde una hora de inicio sin salir de su bloque.
export function maximoHoras(horaInicio) {
  const hora = Number(String(horaInicio).slice(0, 2));
  const bloque = BLOQUES_HORARIOS.find((b) => hora >= b.desde && hora < b.hasta);
  return bloque ? bloque.hasta - hora : 0;
}

export function horaTermino(horaInicio, horas) {
  const hora = Number(String(horaInicio).slice(0, 2)) + Number(horas);
  return `${dosDigitos(hora)}:00`;
}

export function formatoCLP(valor) {
  return `$${Math.round(Number(valor || 0)).toLocaleString('es-CL')}`;
}

export function formatoFechaHora(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString('es-CL', { dateStyle: 'medium', timeStyle: 'short' });
}

// Desglose de arriendo y extras desde los datos de la reserva que entrega el servidor.
export function desgloseReserva(reserva) {
  const arriendo = (reserva?.sectores || []).reduce((t, s) => t + Number(s.subtotal || 0), 0);
  const extras = (reserva?.servicios || []).reduce((t, s) => t + Number(s.subtotal || 0), 0);
  if (!arriendo && !extras) {
    return { arriendo: Number(reserva?.precioArriendo ?? reserva?.monto_total ?? 0), extras: Number(reserva?.precioExtras || 0) };
  }
  return { arriendo, extras };
}

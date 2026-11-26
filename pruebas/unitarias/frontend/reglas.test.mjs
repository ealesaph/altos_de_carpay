// PRUEBAS UNITARIAS · Frontend web (frontend/src/reglas.js)
// Usa el ejecutor de pruebas incluido en Node (node --test), sin instalar dependencias.
// Ejecutar:  docker run --rm -v "${PWD}:/w" -w /w node:22-alpine node --test "pruebas/unitarias/frontend/*.test.mjs"
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  BLOQUES_HORARIOS, codigoReserva, desgloseReserva, formatoCLP, horaTermino, horasDeInicio, maximoHoras, textoBloques,
} from '../../../frontend/src/reglas.js';

test('los bloques coinciden con el backend (13–18 y 19–24)', () => {
  assert.deepEqual(BLOQUES_HORARIOS.map((b) => [b.desde, b.hasta]), [[13, 18], [19, 24]]);
  assert.match(textoBloques(), /13:00 – 18:00/);
  assert.match(textoBloques(), /19:00 – 00:00/);
});

test('horasDeInicio entrega solo horas en punto dentro de los bloques', () => {
  const valores = horasDeInicio().map((h) => h.valor);
  assert.deepEqual(valores, ['13:00', '14:00', '15:00', '16:00', '17:00', '19:00', '20:00', '21:00', '22:00', '23:00']);
  assert.ok(!valores.includes('18:00'), '18:00 es el intervalo entre bloques');
});

test('maximoHoras no deja salir del bloque', () => {
  assert.equal(maximoHoras('13:00'), 5);
  assert.equal(maximoHoras('17:00'), 1);
  assert.equal(maximoHoras('19:00'), 5);
  assert.equal(maximoHoras('23:00'), 1);
  assert.equal(maximoHoras('18:00'), 0);
  assert.equal(maximoHoras('10:00'), 0);
});

test('horaTermino muestra medianoche como 00:00', () => {
  assert.equal(horaTermino('13:00', 5), '18:00');
  assert.equal(horaTermino('19:00', 5), '00:00');
});

test('codigoReserva usa el formato RES-0000 igual que la app y el QR', () => {
  assert.equal(codigoReserva(8), 'RES-0008');
  assert.equal(codigoReserva(1234), 'RES-1234');
});

test('formatoCLP redondea y usa separador de miles chileno', () => {
  assert.equal(formatoCLP(76500), '$76.500');
  assert.equal(formatoCLP('22950.4'), '$22.950');
  assert.equal(formatoCLP(null), '$0');
});

test('desgloseReserva separa arriendo y servicios extra', () => {
  const reserva = { sectores: [{ subtotal: '125000.00' }], servicios: [{ subtotal: '10000.00' }, { subtotal: '5000' }] };
  assert.deepEqual(desgloseReserva(reserva), { arriendo: 125000, extras: 15000 });
  assert.deepEqual(desgloseReserva({ monto_total: '90000' }), { arriendo: 90000, extras: 0 });
});

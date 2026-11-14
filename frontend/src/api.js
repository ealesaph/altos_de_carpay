const API_BASE_URL = (import.meta.env.VITE_API_URL || '/api/v1').replace(/\/$/, '');
const ACCESS_TOKEN_KEY = 'altos_de_carpay_access';
const REFRESH_TOKEN_KEY = 'altos_de_carpay_refresh';

function getErrorMessage(data, status) {
  if (data && typeof data === 'object') {
    const messages = Object.values(data).flatMap((value) =>
      Array.isArray(value) ? value : [value],
    );
    const message = messages.find((value) => typeof value === 'string');
    if (message) return message;
  }
  return `La solicitud falló (HTTP ${status}).`;
}

function parseResponse(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    throw new Error('El servidor devolvió una respuesta que no es JSON válido.');
  }
}

async function sendRequest(path, options, accessToken) {
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  return fetch(`${API_BASE_URL}${path}`, { ...options, headers });
}

async function refreshAccessToken() {
  const refresh = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!refresh) return null;

  const response = await sendRequest(
    '/auth/refresh/',
    { method: 'POST', body: JSON.stringify({ refresh }) },
    null,
  );
  if (!response.ok) {
    clearSession();
    return null;
  }

  const tokens = parseResponse(await response.text());
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.access);
  if (tokens.refresh) localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refresh);
  return tokens.access;
}

export async function apiRequest(path, options = {}) {
  let response = await sendRequest(path, options, localStorage.getItem(ACCESS_TOKEN_KEY));
  if (response.status === 401 && !path.startsWith('/auth/')) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) response = await sendRequest(path, options, newAccessToken);
  }

  const data = parseResponse(await response.text());
  if (!response.ok) throw new Error(getErrorMessage(data, response.status));
  return data;
}

export async function login(correo, password) {
  const data = await apiRequest('/auth/login/', {
    method: 'POST',
    body: JSON.stringify({ correo, password }),
  });
  localStorage.setItem(ACCESS_TOKEN_KEY, data.access);
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refresh);
  return data.usuario;
}

export async function registerCustomer(form) {
  return apiRequest('/auth/registro/', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export async function getSectors() {
  const data = await apiRequest('/sectores/');
  return Array.isArray(data) ? data : data.results;
}

export async function updateSector(id, changes) {
  return apiRequest(`/sectores/${id}/`, {
    method: 'PATCH',
    body: JSON.stringify(changes),
  });
}

export async function createReservation(form) {
  return apiRequest('/reservas/', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export async function getReservations() {
  const data = await apiRequest('/reservas/');
  return Array.isArray(data) ? data : data.results;
}

export async function getReservation(id) {
  return apiRequest(`/reservas/${id}/`);
}

export async function createPayment(id, form) {
  return apiRequest(`/reservas/${id}/pagos/`, {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export async function cancelReservation(id) {
  return apiRequest(`/reservas/${id}/cancelar/`, { method: 'POST' });
}

export async function logout() {
  const refresh = localStorage.getItem(REFRESH_TOKEN_KEY);
  try {
    if (refresh) {
      await apiRequest('/auth/logout/', {
        method: 'POST',
        body: JSON.stringify({ refresh }),
      });
    }
  } finally {
    clearSession();
  }
}

export function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function hasSession() {
  return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY));
}

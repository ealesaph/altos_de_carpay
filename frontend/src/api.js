const API_BASE_URL = (import.meta.env.VITE_API_URL || '/api/v1').replace(/\/$/, '');
const ACCESS_TOKEN_KEY = 'altos_de_carpay_access';
const REFRESH_TOKEN_KEY = 'altos_de_carpay_refresh';
const USER_KEY = 'altos_de_carpay_user';

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
  localStorage.setItem(USER_KEY, JSON.stringify(data.usuario));
  return data.usuario;
}

export async function getProfile() {
  const perfil = await apiRequest('/auth/perfil/');
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      id_usuario: perfil.id_usuario,
      nombre: perfil.nombre,
      rol: perfil.rol,
      is_superuser: Boolean(perfil.is_superuser),
    }),
  );
  return perfil;
}

export async function updateProfile(changes) {
  const perfil = await apiRequest('/auth/perfil/', {
    method: 'PATCH',
    body: JSON.stringify(changes),
  });
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      id_usuario: perfil.id_usuario,
      nombre: perfil.nombre,
      rol: perfil.rol,
      is_superuser: Boolean(perfil.is_superuser),
    }),
  );
  return perfil;
}

export async function changePassword(password_actual, password_nueva) {
  return apiRequest('/auth/cambiar-password/', {
    method: 'POST',
    body: JSON.stringify({ password_actual, password_nueva }),
  });
}

export async function deactivateAccount() {
  const refresh = localStorage.getItem(REFRESH_TOKEN_KEY);
  try {
    return await apiRequest('/auth/desactivar-cuenta/', {
      method: 'POST',
      body: JSON.stringify(refresh ? { refresh } : {}),
    });
  } finally {
    clearSession();
  }
}

// ── Ley N° 21.719: política de datos, consentimiento y portabilidad ──

export async function getPrivacyPolicy() {
  return apiRequest('/auth/politica-privacidad/');
}

export async function getConsentStatus() {
  return apiRequest('/auth/consentimiento/');
}

export async function acceptConsent(version) {
  const data = await apiRequest('/auth/consentimiento/', {
    method: 'POST',
    body: JSON.stringify({ acepta: true, version }),
  });
  const guardado = JSON.parse(localStorage.getItem(USER_KEY) || 'null');
  if (guardado) {
    localStorage.setItem(USER_KEY, JSON.stringify({ ...guardado, requiere_consentimiento: false }));
  }
  return data;
}

export async function getMyData() {
  return apiRequest('/auth/mis-datos/');
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

export async function uploadSectorImage(id, file) {
  const formData = new FormData();
  formData.append('imagen', file);
  let response = await fetch(`${API_BASE_URL}/sectores/${id}/subir-imagen/`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem(ACCESS_TOKEN_KEY) || ''}` },
    body: formData,
  });
  if (response.status === 401) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) {
      response = await fetch(`${API_BASE_URL}/sectores/${id}/subir-imagen/`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${newAccessToken}` },
        body: formData,
      });
    }
  }
  const data = parseResponse(await response.text());
  if (!response.ok) throw new Error(getErrorMessage(data, response.status));
  return data;
}

export async function getUsers() {
  const data = await apiRequest('/usuarios/');
  return Array.isArray(data) ? data : data.results;
}

export async function createStaffAdmin(form) {
  return apiRequest('/usuarios/', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export async function updateUser(id, changes) {
  return apiRequest(`/usuarios/${id}/`, {
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

export async function changeReservationStatus(id, estado) {
  return apiRequest(`/reservas/${id}/estado/`, {
    method: 'POST',
    body: JSON.stringify({ estado }),
  });
}

export async function sendReservationEmail(id) {
  return apiRequest(`/reservas/${id}/enviar-correo/`, { method: 'POST' });
}

export async function getConsultas() {
  const data = await apiRequest('/consultas/');
  return Array.isArray(data) ? data : data.results;
}

export async function createConsulta(form) {
  return apiRequest('/consultas/', {
    method: 'POST',
    body: JSON.stringify(form),
  });
}

export async function replyConsulta(id, respuesta) {
  return apiRequest(`/consultas/${id}/responder/`, {
    method: 'POST',
    body: JSON.stringify({ respuesta }),
  });
}

const REPORTES = {
  reservas: { ruta: '/reservas/exportar/', archivo: 'reservas_altos_de_carpay' },
  usuarios: { ruta: '/usuarios/exportar/', archivo: 'usuarios_altos_de_carpay' },
  incidencias: { ruta: '/incidencias/exportar/', archivo: 'incidencias_altos_de_carpay' },
};

// HU-08: descarga un reporte en Excel (.xlsx, por defecto) o CSV.
export async function exportarReporte(tipo = 'reservas', formato = 'xlsx') {
  const reporte = REPORTES[tipo];
  if (!reporte) throw new Error('Reporte no disponible.');
  const ruta = `${reporte.ruta}?formato=${formato}`;
  let response = await sendRequest(ruta, { method: 'GET' }, localStorage.getItem(ACCESS_TOKEN_KEY));
  if (response.status === 401) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) {
      response = await sendRequest(ruta, { method: 'GET' }, newAccessToken);
    }
  }
  if (!response.ok) {
    throw new Error(`No se pudo exportar el archivo (HTTP ${response.status}).`);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${reporte.archivo}.${formato}`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export async function exportReservationsCsv() {
  return exportarReporte('reservas', 'csv');
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
  localStorage.removeItem(USER_KEY);
}

export function hasSession() {
  return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY));
}

export function getSessionUser() {
  if (!hasSession()) return null;
  try {
    const saved = localStorage.getItem(USER_KEY);
    if (saved) return JSON.parse(saved);
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const payload = JSON.parse(atob(token.split('.')[1]));
    return {
      id_usuario: payload.user_id,
      nombre: payload.nombre,
      rol: payload.rol,
      is_superuser: Boolean(payload.is_superuser),
    };
  } catch {
    return null;
  }
}

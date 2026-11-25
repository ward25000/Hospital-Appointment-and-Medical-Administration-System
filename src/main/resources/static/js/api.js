import { t, getLang } from './i18n.js';

const API_BASE = '/api';

export class ApiError extends Error {
  constructor(message, status, fieldErrors = null, payload = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.payload = payload;
  }
}

let csrf = null;

export function getErrorMessage(error) {
  if (error instanceof ApiError || error instanceof Error) {
    return error.message || t('Не удалось выполнить операцию');
  }
  return t('Не удалось выполнить операцию');
}

export async function ensureCsrf(force = false) {
  if (csrf && !force) {
    return csrf;
  }
  let response;
  try {
    response = await fetch('/api/auth/csrf', {
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json', 'Accept-Language': getLang() },
    });
  } catch {
    throw new ApiError(t('Сервер недоступен. Проверьте соединение и повторите попытку'), 0);
  }
  if (!response.ok) {
    csrf = null;
    throw new ApiError(t('Не удалось получить защитный токен сессии'), response.status);
  }
  csrf = await response.json();
  return csrf;
}

export function resetCsrf() {
  csrf = null;
}

async function parseResponse(response) {
  if (response.status === 204) {
    return null;
  }
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    return response.json().catch(() => null);
  }
  const text = await response.text().catch(() => '');
  return text || null;
}

function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    query.set(key, String(value));
  });
  const serialized = query.toString();
  return serialized ? `?${serialized}` : '';
}

async function send(path, options = {}, retry = true) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = { Accept: 'application/json', 'Accept-Language': getLang(), ...(options.headers || {}) };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = await ensureCsrf();
    headers[token.headerName || 'X-XSRF-TOKEN'] = token.token;
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      credentials: 'same-origin',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    });
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw error;
    }
    throw new ApiError(t('Сервер недоступен. Проверьте соединение и повторите попытку'), 0);
  }

  if (response.ok && options.blob) {
    return response.blob();
  }
  const payload = await parseResponse(response);

  if (!response.ok) {
    if (response.status === 403 && !options.noCsrfRetry && retry && !['GET', 'HEAD'].includes(method)) {
      await ensureCsrf(true);
      return send(path, { ...options, noCsrfRetry: true }, false);
    }
    const message = payload?.message
      || (response.status === 401 ? t('Требуется авторизация') : t('Не удалось выполнить операцию'));
    if (response.status === 401 && !path.startsWith('/auth/')) {
      resetCsrf();
      document.dispatchEvent(new CustomEvent('auth:expired'));
    }
    throw new ApiError(message, response.status, payload?.fieldErrors || null, payload);
  }

  return payload;
}

async function downloadPdf(path, filename) {
  const blob = await send(path, { blob: true, headers: { Accept: 'application/pdf, application/json' } });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(link.href), 2000);
}

const auth = {
  csrf: ensureCsrf,
  lookupIin(iin) {
    return send('/auth/iin', { method: 'POST', body: { iin } });
  },
  async login(credentials) {
    const user = await send('/auth/login', { method: 'POST', body: credentials });
    await ensureCsrf(true);
    return user;
  },
  async register(data) {
    const user = await send('/auth/register', { method: 'POST', body: data });
    await ensureCsrf(true);
    return user;
  },
  async logout() {
    await send('/auth/logout', { method: 'POST' });
    resetCsrf();
    await ensureCsrf(true).catch(() => null);
  },
  me() {
    return send('/auth/me');
  },
};

const appointments = {
  list(params = {}) {
    return send(`/appointments${buildQuery(params)}`);
  },
  get(id) {
    return send(`/appointments/${id}`);
  },
  create(data) {
    return send('/appointments', { method: 'POST', body: data });
  },
  reschedule(id, data) {
    return send(`/appointments/${id}/reschedule`, { method: 'PUT', body: data });
  },
  complete(id, data) {
    return send(`/appointments/${id}/complete`, { method: 'POST', body: data });
  },
  referrals(id) {
    return send(`/appointments/${id}/referrals`);
  },
  certificate(id) {
    return downloadPdf(`/appointments/${id}/certificate`, `medflow-certificate-${id}-${getLang()}.pdf`);
  },
  noShow(id) {
    return send(`/appointments/${id}/no-show`, { method: 'POST' });
  },
  cancel(id, reason) {
    return send(`/appointments/${id}/cancel`, { method: 'POST', body: { reason: reason || null } });
  },
};

const patients = {
  list(query = '') {
    return send(`/patients${buildQuery({ query })}`);
  },
  get(id) {
    return send(`/patients/${id}`);
  },
  create(data) {
    return send('/patients', { method: 'POST', body: data });
  },
  update(id, data) {
    return send(`/patients/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return send(`/patients/${id}`, { method: 'DELETE' });
  },
};

const doctors = {
  list(params = {}) {
    return send(`/doctors${buildQuery(params)}`);
  },
  get(id) {
    return send(`/doctors/${id}`);
  },
  create(data) {
    return send('/doctors', { method: 'POST', body: data });
  },
  update(id, data) {
    return send(`/doctors/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return send(`/doctors/${id}`, { method: 'DELETE' });
  },
  availability(doctorId, params = {}) {
    return send(`/doctors/${doctorId}/availability${buildQuery(params)}`);
  },
};

const specializations = {
  list() {
    return send('/specializations');
  },
  create(data) {
    return send('/specializations', { method: 'POST', body: data });
  },
  update(id, data) {
    return send(`/specializations/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return send(`/specializations/${id}`, { method: 'DELETE' });
  },
};

const services = {
  list() {
    return send('/services');
  },
  create(data) {
    return send('/services', { method: 'POST', body: data });
  },
  update(id, data) {
    return send(`/services/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return send(`/services/${id}`, { method: 'DELETE' });
  },
};

const schedules = {
  byDoctor(doctorId) {
    return send(`/doctors/${doctorId}/schedules`);
  },
  create(doctorId, data) {
    return send(`/doctors/${doctorId}/schedules`, { method: 'POST', body: data });
  },
  update(id, data) {
    return send(`/schedules/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return send(`/schedules/${id}`, { method: 'DELETE' });
  },
};

const reports = {
  get(params = {}) {
    return send(`/reports${buildQuery(params)}`);
  },
};

const me = {
  profile() {
    return send('/me/profile');
  },
  updateProfile(data) {
    return send('/me/profile', { method: 'PUT', body: data });
  },
  visits() {
    return send('/me/visits');
  },
  setLanguage(lang) {
    return send('/me/language', { method: 'PUT', body: { lang } });
  },
  payables() {
    return send('/me/payables');
  },
  payments() {
    return send('/me/payments');
  },
  pay(data) {
    return send('/me/payments', { method: 'POST', body: data });
  },
  receipt(id) {
    return downloadPdf(`/me/payments/${id}/receipt`, `medflow-receipt-${id}-${getLang()}.pdf`);
  },
  resend(id) {
    return send(`/me/payments/${id}/resend`, { method: 'POST' });
  },
};

const payments = {
  list(params = {}) {
    return send(`/payments${buildQuery(params)}`);
  },
  payables(patientId) {
    return send(`/payments/payables${buildQuery({ patientId })}`);
  },
  cashDesk(data) {
    return send('/payments/cash-desk', { method: 'POST', body: data });
  },
  receipt(id) {
    return downloadPdf(`/payments/${id}/receipt`, `medflow-receipt-${id}-${getLang()}.pdf`);
  },
  resend(id) {
    return send(`/payments/${id}/resend`, { method: 'POST' });
  },
};

const referrals = {
  incoming(status) {
    return send(`/referrals/incoming${buildQuery({ status })}`);
  },
  outgoing() {
    return send('/referrals/outgoing');
  },
  mine() {
    return send('/referrals/mine');
  },
  create(data) {
    return send('/referrals', { method: 'POST', body: data });
  },
  updateStatus(id, status) {
    return send(`/referrals/${id}/status`, { method: 'PUT', body: { status } });
  },
};

export const api = {
  auth,
  appointments,
  patients,
  doctors,
  specializations,
  services,
  schedules,
  reports,
  me,
  payments,
  referrals,
  clinic() {
    return send('/clinic');
  },
  dashboard() {
    return send('/dashboard');
  },
  health() {
    return send('/health');
  },
  buildQuery,
};

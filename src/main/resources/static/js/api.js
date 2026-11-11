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
    return error.message || 'Не удалось выполнить операцию';
  }
  return 'Не удалось выполнить операцию';
}

export async function ensureCsrf(force = false) {
  if (csrf && !force) {
    return csrf;
  }
  const response = await fetch('/api/auth/csrf', {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    csrf = null;
    throw new ApiError('Не удалось получить защитный токен сессии', response.status);
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
  const headers = { Accept: 'application/json', ...(options.headers || {}) };
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
    throw new ApiError('Сервер недоступен. Проверьте соединение и повторите попытку', 0);
  }

  const payload = await parseResponse(response);

  if (!response.ok) {
    if (response.status === 403 && !options.noCsrfRetry && retry && !['GET', 'HEAD'].includes(method)) {
      await ensureCsrf(true);
      return send(path, { ...options, noCsrfRetry: true }, false);
    }
    const message = payload?.message
      || (response.status === 401 ? 'Требуется авторизация' : 'Не удалось выполнить операцию');
    if (response.status === 401 && !path.startsWith('/auth/')) {
      resetCsrf();
      document.dispatchEvent(new CustomEvent('auth:expired'));
    }
    throw new ApiError(message, response.status, payload?.fieldErrors || null, payload);
  }

  return payload;
}

const auth = {
  csrf: ensureCsrf,
  async login(credentials) {
    const user = await send('/auth/login', { method: 'POST', body: credentials });
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
  complete(id) {
    return send(`/appointments/${id}/complete`, { method: 'POST' });
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

export const api = {
  auth,
  appointments,
  patients,
  doctors,
  specializations,
  services,
  schedules,
  reports,
  dashboard() {
    return send('/dashboard');
  },
  health() {
    return send('/health');
  },
  buildQuery,
};

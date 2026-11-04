import { api, getErrorMessage } from './api.js';
import {
  icon,
  esc,
  toast,
  avatar,
  roleLabel,
  formatDateLong,
  todayISO,
  loadingState,
  errorState,
} from './ui.js';
import { store } from './store.js';
import * as dashboardView from './views/dashboard.js';
import * as calendarView from './views/calendar.js';
import * as appointmentsView from './views/appointments.js';
import * as patientsView from './views/patients.js';
import * as doctorsView from './views/doctors.js';
import * as schedulesView from './views/schedules.js';
import * as catalogView from './views/catalog.js';
import * as reportsView from './views/reports.js';

const ROUTES = {
  dashboard: {
    title: 'Дашборд',
    subtitle: 'Ключевые показатели клиники и приёмы на сегодня',
    breadcrumb: 'Обзор клиники',
    module: dashboardView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'dashboard',
  },
  calendar: {
    title: 'Календарь приёмов',
    subtitle: 'Загрузка врачей по дням и запись в свободные слоты',
    breadcrumb: 'Расписание',
    module: calendarView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'calendar',
  },
  appointments: {
    title: 'Записи на приём',
    subtitle: 'Все визиты, статусы и управление записями',
    breadcrumb: 'Работа с пациентами',
    module: appointmentsView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'appointments',
  },
  patients: {
    title: 'Пациенты',
    subtitle: 'Картотека пациентов клиники и история обращений',
    breadcrumb: 'Работа с пациентами',
    module: patientsView,
    roles: ['ADMIN', 'RECEPTIONIST'],
    nav: 'patients',
  },
  doctors: {
    title: 'Врачи',
    subtitle: 'Специалисты клиники, направления и контактные данные',
    breadcrumb: 'Справочники',
    module: doctorsView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'doctors',
  },
  schedules: {
    title: 'Расписание врачей',
    subtitle: 'Недельные смены, интервалы приёма и доступность',
    breadcrumb: 'Управление расписанием',
    module: schedulesView,
    roles: ['ADMIN', 'DOCTOR'],
    nav: 'schedules',
  },
  catalog: {
    title: 'Справочники',
    subtitle: 'Специализации и медицинские услуги клиники',
    breadcrumb: 'Справочники',
    module: catalogView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'catalog',
  },
  reports: {
    title: 'Отчёты',
    subtitle: 'Статистика приёмов, нагрузка врачей и выручка за период',
    breadcrumb: 'Аналитика',
    module: reportsView,
    roles: ['ADMIN', 'RECEPTIONIST'],
    nav: 'reports',
  },
};

const NAV_GROUPS = [
  {
    label: 'Клиника',
    items: [
      { route: 'dashboard', label: 'Дашборд', icon: 'dashboard' },
      { route: 'calendar', label: 'Календарь', icon: 'calendar' },
      { route: 'appointments', label: 'Записи', icon: 'appointments' },
    ],
  },
  {
    label: 'Управление',
    items: [
      { route: 'patients', label: 'Пациенты', icon: 'patients', roles: ['ADMIN', 'RECEPTIONIST'] },
      { route: 'doctors', label: 'Врачи', icon: 'doctors' },
      { route: 'schedules', label: 'Расписание', icon: 'schedules', roles: ['ADMIN', 'DOCTOR'] },
      { route: 'catalog', label: 'Справочники', icon: 'catalog' },
      { route: 'reports', label: 'Отчёты', icon: 'reports', roles: ['ADMIN', 'RECEPTIONIST'] },
    ],
  },
];

const loginScreen = document.getElementById('login-screen');
const appElement = document.getElementById('app');
const viewElement = document.getElementById('view');
const navElement = document.getElementById('nav');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const loginSubmit = document.getElementById('login-submit');
const userDropdown = document.getElementById('user-dropdown');
const userButton = document.getElementById('user-button');
const sidebar = document.getElementById('sidebar');
const sidebarOverlay = document.getElementById('sidebar-overlay');

let currentCleanup = null;
let routeToken = 0;
let healthTimer = null;
let sessionExpiredHandlerBound = false;

function parseRoute() {
  const raw = window.location.hash.replace(/^#\/?/, '');
  const [pathPart, queryPart] = raw.split('?');
  const segments = pathPart.split('/').filter(Boolean);
  return {
    name: segments[0] || 'dashboard',
    params: new URLSearchParams(queryPart || ''),
  };
}

function canAccess(config) {
  return Boolean(store.user && config.roles.includes(store.user.role));
}

function renderForbidden(config) {
  viewElement.innerHTML = `
    <div class="card">
      <div class="empty-state">
        <span class="empty-icon">${icon('shield')}</span>
        <strong>Раздел недоступен</strong>
        <p>Ваша роль не позволяет открывать «${esc(config.title)}». Вернитесь на дашборд или обратитесь к администратору.</p>
        <a class="btn btn-secondary btn-sm" href="#/dashboard">${icon('arrow-right')}<span>На дашборд</span></a>
      </div>
    </div>
  `;
}

function updatePageMeta(config) {
  document.getElementById('page-title').textContent = config.title;
  document.getElementById('page-subtitle').textContent = config.subtitle;
  document.getElementById('breadcrumbs').textContent = `MedFlow · ${config.breadcrumb}`;
  document.title = `${config.title} — MedFlow`;
  navElement.querySelectorAll('.nav-item').forEach((item) => {
    item.classList.toggle('is-active', item.dataset.route === config.nav);
  });
}

async function handleRoute() {
  if (!store.user) {
    return;
  }
  const token = ++routeToken;
  const { name, params } = parseRoute();
  const config = ROUTES[name];

  if (!config) {
    window.location.hash = '#/dashboard';
    return;
  }

  if (typeof currentCleanup === 'function') {
    try {
      currentCleanup();
    } catch {
      // ignore view cleanup errors
    }
  }
  currentCleanup = null;

  updatePageMeta(config);
  closeSidebar();

  if (!canAccess(config)) {
    renderForbidden(config);
    return;
  }

  viewElement.innerHTML = loadingState();
  try {
    const cleanup = await config.module.render(viewElement, { user: store.user, params });
    if (token !== routeToken) {
      if (typeof cleanup === 'function') {
        cleanup();
      }
      return;
    }
    if (typeof cleanup === 'function') {
      currentCleanup = cleanup;
    }
  } catch (error) {
    if (token !== routeToken) {
      return;
    }
    if (error?.name === 'AbortError') {
      return;
    }
    viewElement.innerHTML = errorState(getErrorMessage(error));
    const retry = viewElement.querySelector('[data-retry]');
    if (retry) {
      retry.addEventListener('click', handleRoute);
    }
  }
}

function renderNav() {
  if (!store.user) {
    navElement.innerHTML = '';
    return;
  }
  const groups = NAV_GROUPS.map((group, index) => {
    const items = group.items.filter((item) => !item.roles || item.roles.includes(store.user.role));
    if (items.length === 0) {
      return '';
    }
    return `
      ${index > 0 ? `<div class="sidebar-label" style="margin: 18px 10px 6px">${esc(group.label)}</div>` : ''}
      ${items.map((item) => `
        <a class="nav-item" href="#/${item.route}" data-route="${item.route}">
          ${icon(item.icon)}
          <span>${esc(item.label)}</span>
        </a>
      `).join('')}
    `;
  }).join('');
  navElement.innerHTML = groups;
}

function setUserInterface(user) {
  const initialsAvatar = avatar(user.fullName, '');
  const role = roleLabel(user.role);

  document.getElementById('user-avatar').outerHTML = initialsAvatar
    .replace('class="avatar ', `id="user-avatar" class="avatar `);
  const dropdownAvatar = document.getElementById('dropdown-avatar');
  if (dropdownAvatar) {
    dropdownAvatar.outerHTML = avatar(user.fullName, 'avatar-lg')
      .replace('class="avatar ', `id="dropdown-avatar" class="avatar `);
  }
  document.getElementById('user-name').textContent = user.fullName;
  document.getElementById('user-role').textContent = role;
  document.getElementById('dropdown-name').textContent = user.fullName;
  document.getElementById('dropdown-role').textContent = role;
  document.getElementById('footer-date').textContent = formatDateLong(todayISO());
}

function showApp(user) {
  store.user = user;
  loginScreen.classList.add('hidden');
  appElement.classList.remove('hidden');
  setUserInterface(user);
  renderNav();
  if (!window.location.hash || window.location.hash === '#') {
    history.replaceState(null, '', '#/dashboard');
  }
  handleRoute();
  checkHealth();
  if (!healthTimer) {
    healthTimer = window.setInterval(checkHealth, 60000);
  }
}

function showLogin(message = '') {
  store.user = null;
  appElement.classList.add('hidden');
  loginScreen.classList.remove('hidden');
  document.title = 'Вход — MedFlow';
  if (message) {
    loginError.textContent = message;
    loginError.classList.remove('hidden');
  } else {
    loginError.classList.add('hidden');
    loginError.textContent = '';
  }
  window.setTimeout(() => document.getElementById('login-username')?.focus(), 40);
}

async function checkHealth() {
  const dot = document.getElementById('health-dot');
  const text = document.getElementById('health-text');
  if (!dot || !text) {
    return;
  }
  try {
    const health = await api.health();
    store.health = health;
    dot.className = 'health-dot is-up';
    text.textContent = 'Система работает';
  } catch {
    store.health = null;
    dot.className = 'health-dot is-down';
    text.textContent = 'API недоступен';
  }
}

function closeSidebar() {
  sidebar.classList.remove('is-open');
  sidebarOverlay.classList.remove('is-visible');
}

function closeUserDropdown() {
  userDropdown.classList.add('hidden');
  userButton.setAttribute('aria-expanded', 'false');
}

function bindLogin() {
  document.querySelectorAll('.demo-account').forEach((button) => {
    button.addEventListener('click', () => {
      document.getElementById('login-username').value = button.dataset.username || '';
      document.getElementById('login-password').value = button.dataset.password || '';
      loginError.classList.add('hidden');
      loginSubmit.focus();
    });
  });

  document.getElementById('toggle-password').addEventListener('click', () => {
    const input = document.getElementById('login-password');
    const wrap = input.closest('.input-wrap');
    const visible = input.type === 'text';
    input.type = visible ? 'password' : 'text';
    wrap.classList.toggle('password-visible', !visible);
    document.getElementById('toggle-password').setAttribute(
      'aria-label',
      visible ? 'Показать пароль' : 'Скрыть пароль',
    );
  });

  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    if (!username || !password) {
      loginError.textContent = 'Введите логин и пароль';
      loginError.classList.remove('hidden');
      return;
    }
    loginError.classList.add('hidden');
    loginSubmit.disabled = true;
    const original = loginSubmit.innerHTML;
    loginSubmit.innerHTML = '<span class="spinner spinner-light"></span><span>Проверяем доступ…</span>';
    try {
      const user = await api.auth.login({ username, password });
      loginForm.reset();
      toast(`Добро пожаловать, ${user.fullName.split(' ')[0]}`, 'success', 'Вход выполнен');
      showApp(user);
    } catch (error) {
      loginError.textContent = error?.status === 401
        ? 'Неверный логин или пароль. Проверьте раскладку и попробуйте снова.'
        : getErrorMessage(error);
      loginError.classList.remove('hidden');
    } finally {
      loginSubmit.disabled = false;
      loginSubmit.innerHTML = original;
    }
  });
}

function bindApp() {
  document.getElementById('menu-toggle').addEventListener('click', () => {
    sidebar.classList.add('is-open');
    sidebarOverlay.classList.add('is-visible');
  });
  document.getElementById('sidebar-close').addEventListener('click', closeSidebar);
  sidebarOverlay.addEventListener('click', closeSidebar);

  userButton.addEventListener('click', (event) => {
    event.stopPropagation();
    const isHidden = userDropdown.classList.contains('hidden');
    userDropdown.classList.toggle('hidden', !isHidden);
    userButton.setAttribute('aria-expanded', String(isHidden));
  });

  document.addEventListener('click', (event) => {
    if (!userDropdown.contains(event.target) && !userButton.contains(event.target)) {
      closeUserDropdown();
    }
  });

  document.getElementById('logout-button').addEventListener('click', async () => {
    closeUserDropdown();
    try {
      await api.auth.logout();
    } catch {
      // session may already be expired; still drop local state
    }
    if (healthTimer) {
      window.clearInterval(healthTimer);
      healthTimer = null;
    }
    if (typeof currentCleanup === 'function') {
      try {
        currentCleanup();
      } catch {
        // ignore
      }
      currentCleanup = null;
    }
    history.replaceState(null, '', window.location.pathname);
    viewElement.innerHTML = '';
    showLogin();
    toast('Вы вышли из системы', 'info');
  });

  window.addEventListener('hashchange', handleRoute);
  window.addEventListener('app:route', handleRoute);
}

function bindSessionExpiry() {
  if (sessionExpiredHandlerBound) {
    return;
  }
  sessionExpiredHandlerBound = true;
  document.addEventListener('auth:expired', () => {
    if (!store.user) {
      return;
    }
    if (healthTimer) {
      window.clearInterval(healthTimer);
      healthTimer = null;
    }
    history.replaceState(null, '', window.location.pathname);
    viewElement.innerHTML = '';
    showLogin('Сессия истекла. Войдите снова, чтобы продолжить работу.');
    toast('Сессия истекла. Выполните вход повторно.', 'info', 'Требуется вход');
  });
}

async function boot() {
  bindLogin();
  bindApp();
  bindSessionExpiry();
  try {
    const user = await api.auth.me();
    showApp(user);
  } catch {
    showLogin();
  }
}

boot();

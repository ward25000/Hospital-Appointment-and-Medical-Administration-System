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
  setButtonLoading,
  clearFieldErrors,
  showFormAlert,
  applyFieldErrors,
  formValue,
  readAddress,
  parseIin,
} from './ui.js';
import { store } from './store.js';
import { LANGS, changeLang, getLang, loadDictionary, t, translateStatic } from './i18n.js';
import * as dashboardView from './views/dashboard.js';
import * as calendarView from './views/calendar.js';
import * as appointmentsView from './views/appointments.js';
import * as patientsView from './views/patients.js';
import * as doctorsView from './views/doctors.js';
import * as schedulesView from './views/schedules.js';
import * as catalogView from './views/catalog.js';
import * as reportsView from './views/reports.js';
import * as patientProfileView from './views/patientProfile.js';
import * as patientVisitsView from './views/patientVisits.js';
import * as patientReferralsView from './views/patientReferrals.js';
import * as patientPaymentsView from './views/patientPayments.js';
import * as cashDeskView from './views/cashDesk.js';
import * as referralInboxView from './views/referralInbox.js';

const ROUTES = {
  dashboard: {
    title: 'Сегодня',
    subtitle: 'Приёмы на сегодня и показатели месяца',
    module: dashboardView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'dashboard',
  },
  calendar: {
    title: 'Календарь приёмов',
    subtitle: 'Загрузка врачей по дням и запись на свободное время',
    module: calendarView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'calendar',
  },
  appointments: {
    title: 'Записи на приём',
    subtitle: 'Все визиты, статусы и управление записями',
    module: appointmentsView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'appointments',
  },
  patients: {
    title: 'Пациенты',
    subtitle: 'Картотека пациентов клиники и история обращений',
    module: patientsView,
    roles: ['ADMIN', 'RECEPTIONIST'],
    nav: 'patients',
  },
  doctors: {
    title: 'Врачи',
    subtitle: 'Специалисты клиники и их контакты',
    module: doctorsView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'doctors',
  },
  schedules: {
    title: 'Расписание врачей',
    subtitle: 'Недельные смены, интервалы приёма и доступность',
    module: schedulesView,
    roles: ['ADMIN', 'DOCTOR'],
    nav: 'schedules',
  },
  catalog: {
    title: 'Справочники',
    subtitle: 'Специализации и медицинские услуги клиники',
    module: catalogView,
    roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
    nav: 'catalog',
  },
  reports: {
    title: 'Отчёты',
    subtitle: 'Статистика приёмов, нагрузка врачей и выручка за период',
    module: reportsView,
    roles: ['ADMIN', 'RECEPTIONIST'],
    nav: 'reports',
  },
  profile: {
    title: 'Мой профиль',
    subtitle: 'Личные данные, адрес и контакты',
    module: patientProfileView,
    roles: ['PATIENT'],
    nav: 'profile',
  },
  visits: {
    title: 'История посещений',
    subtitle: 'Визиты, заключения врачей и справки',
    module: patientVisitsView,
    roles: ['PATIENT'],
    nav: 'visits',
  },
  referrals: {
    title: 'Мои направления',
    subtitle: 'Электронные направления, выписанные во время приёмов',
    module: patientReferralsView,
    roles: ['PATIENT'],
    nav: 'referrals',
  },
  payments: {
    title: 'Оплата',
    subtitle: 'Счета за приёмы и направления, квитанции',
    module: patientPaymentsView,
    roles: ['PATIENT'],
    nav: 'payments',
  },
  'cash-desk': {
    title: 'Касса',
    subtitle: 'Приём оплаты в клинике и журнал платежей',
    module: cashDeskView,
    roles: ['ADMIN', 'RECEPTIONIST'],
    nav: 'cash-desk',
  },
  'referral-inbox': {
    title: 'Направления',
    subtitle: 'Пациенты, направленные к вам, и направления, выписанные вами',
    module: referralInboxView,
    roles: ['DOCTOR'],
    nav: 'referral-inbox',
  },
};

const NAV_GROUPS = [
  {
    label: 'Клиника',
    items: [
      { route: 'dashboard', label: 'Сегодня', icon: 'dashboard', roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'] },
      { route: 'calendar', label: 'Календарь', icon: 'calendar', roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'] },
      { route: 'appointments', label: 'Записи', icon: 'appointments', roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'] },
      { route: 'cash-desk', label: 'Касса', icon: 'wallet', roles: ['ADMIN', 'RECEPTIONIST'] },
      { route: 'referral-inbox', label: 'Направления', icon: 'inbox', roles: ['DOCTOR'] },
    ],
  },
  {
    label: 'Управление',
    items: [
      { route: 'patients', label: 'Пациенты', icon: 'patients', roles: ['ADMIN', 'RECEPTIONIST'] },
      { route: 'doctors', label: 'Врачи', icon: 'doctors', roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'] },
      { route: 'schedules', label: 'Расписание', icon: 'schedules', roles: ['ADMIN', 'DOCTOR'] },
      { route: 'catalog', label: 'Справочники', icon: 'catalog', roles: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'] },
      { route: 'reports', label: 'Отчёты', icon: 'reports', roles: ['ADMIN', 'RECEPTIONIST'] },
    ],
  },
  {
    label: 'Личный кабинет',
    items: [
      { route: 'profile', label: 'Мой профиль', icon: 'profile', roles: ['PATIENT'] },
      { route: 'visits', label: 'История посещений', icon: 'visits', roles: ['PATIENT'] },
      { route: 'referrals', label: 'Мои направления', icon: 'referrals', roles: ['PATIENT'] },
      { route: 'payments', label: 'Оплата', icon: 'wallet', roles: ['PATIENT'] },
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
const registerForm = document.getElementById('register-form');
const registerError = document.getElementById('register-error');
const registerSubmit = document.getElementById('register-submit');
const userDropdown = document.getElementById('user-dropdown');
const userButton = document.getElementById('user-button');
const sidebar = document.getElementById('sidebar');
const sidebarOverlay = document.getElementById('sidebar-overlay');

let currentCleanup = null;
let routeToken = 0;
let healthTimer = null;
let sessionExpiredHandlerBound = false;

function defaultRouteName() {
  return store.user?.role === 'PATIENT' ? 'visits' : 'dashboard';
}

function parseRoute() {
  const raw = window.location.hash.replace(/^#\/?/, '');
  const [pathPart, queryPart] = raw.split('?');
  const segments = pathPart.split('/').filter(Boolean);
  return {
    name: segments[0] || defaultRouteName(),
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
        <strong>${t('Раздел недоступен')}</strong>
        <p>${t('Для этого раздела нужны другие права доступа.')}</p>
        <a class="btn btn-secondary btn-sm" href="#/${defaultRouteName()}">${icon('arrow-right')}<span>${t('На главную')}</span></a>
      </div>
    </div>
  `;
}

function updatePageMeta(config) {
  document.getElementById('page-title').textContent = t(config.title);
  document.getElementById('page-subtitle').textContent = t(config.subtitle);
  document.title = `${t(config.title)} | MedFlow`;
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
    window.location.hash = `#/${defaultRouteName()}`;
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
      ${index === 1 ? `<div class="sidebar-label">${t(group.label)}</div>` : ''}
      ${items.map((item) => `
        <a class="nav-item" href="#/${item.route}" data-route="${item.route}">
          ${icon(item.icon)}
          <span>${t(item.label)}</span>
          ${item.route === 'referral-inbox' ? '<span class="nav-badge hidden" id="referral-badge"></span>' : ''}
        </a>
      `).join('')}
    `;
  }).join('');
  navElement.innerHTML = groups;
}

function setUserInterface(user) {
  document.getElementById('user-avatar').outerHTML = avatar(user.fullName)
    .replace('class="avatar ', 'id="user-avatar" class="avatar ');
  document.getElementById('user-name').textContent = user.fullName;
  document.getElementById('user-role').textContent = roleLabel(user.role);
  document.getElementById('dropdown-name').textContent = user.fullName;
  document.getElementById('dropdown-role').textContent = roleLabel(user.role);
  document.getElementById('footer-date').textContent = formatDateLong(todayISO());
}

function renderLangSwitch() {
  const container = document.getElementById('lang-switch');
  container.innerHTML = LANGS.map((item) => `
    <button type="button" data-lang="${item.code}" lang="${item.code}" title="${item.name}" aria-pressed="${item.code === getLang()}">${item.label}</button>
  `).join('');
  container.querySelectorAll('[data-lang]').forEach((button) => {
    button.addEventListener('click', () => switchLanguage(button.dataset.lang));
  });
}

/** Names of services and departments, so that open forms can follow the language. */
async function directoryNames(from, to) {
  if (!store.user) return [];
  const suffix = { ru: 'Ru', kk: 'Kk', en: 'En' };
  const [services, specializations] = await Promise.all([api.services.list(), api.specializations.list()]);
  return [...services, ...specializations].map((item) => {
    const name = (lang) => item[`name${suffix[lang]}`] || item.nameRu;
    return [name(from), name(to)];
  });
}

async function switchLanguage(lang) {
  const modalRoot = document.getElementById('modal-root');
  const modalOpen = modalRoot.childElementCount > 0;
  const changed = await changeLang(lang, {
    openRoots: [modalRoot],
    loadEntities: directoryNames,
  });
  if (!changed) return;
  renderLangSwitch();
  loadClinic();
  if (!loginScreen.classList.contains('hidden')) {
    document.title = `${t('Вход')} | MedFlow`;
    clearFieldErrors(registerForm);
    loginError.classList.add('hidden');
    if (registerForm.elements.namedItem('iin').value.length === 12) lookupIin();
    return;
  }
  if (store.user?.role === 'PATIENT') {
    api.me.setLanguage(lang).catch(() => null);
  }
  setUserInterface(store.user);
  renderNav();
  checkHealth();
  refreshReferralBadge();
  if (modalOpen) {
    // The page behind an open form is redrawn when the form closes.
    updatePageMeta(ROUTES[parseRoute().name] || ROUTES[defaultRouteName()]);
    return;
  }
  const typed = [...viewElement.querySelectorAll('input[id], select[id], textarea[id]')]
    .map((element) => [element.id, element.type === 'checkbox' ? element.checked : element.value]);
  const marks = [...viewElement.querySelectorAll('input[type="checkbox"]:not([id]), input[type="radio"]')]
    .map((element) => [`input[value="${element.value}"]`, element.checked]);
  await handleRoute();
  typed.forEach(([id, value]) => {
    const element = document.getElementById(id);
    if (!element || !viewElement.contains(element)) return;
    if (element.type === 'checkbox') element.checked = value;
    else element.value = value;
  });
  marks.forEach(([selector, checked]) => {
    const element = viewElement.querySelector(selector);
    if (element && element.checked !== checked) {
      element.checked = checked;
      element.dispatchEvent(new Event('change', { bubbles: true }));
    }
  });
}

async function loadClinic() {
  try {
    const clinic = await api.clinic();
    document.getElementById('header-clinic').textContent = clinic.name;
    const phone = document.getElementById('header-phone');
    phone.textContent = clinic.phone;
    phone.href = `tel:${clinic.phone.replace(/[^+\d]/g, '')}`;
    document.getElementById('login-contacts').innerHTML = `<strong>${esc(clinic.name)}</strong>${esc(clinic.address)}<br>${esc(clinic.email)}`;
    document.getElementById('footer-clinic').textContent = `${clinic.name}, ${clinic.address}`;
  } catch {
    // contacts are decorative here; the page works without them
  }
}

let lastReferralCount = null;

/** New referrals show up for the receiving doctor without a page reload. */
async function refreshReferralBadge() {
  if (store.user?.role !== 'DOCTOR') return;
  try {
    const pending = (await api.referrals.incoming('PENDING')).length;
    const badge = document.getElementById('referral-badge');
    if (badge) {
      badge.textContent = pending;
      badge.classList.toggle('hidden', pending === 0);
    }
    if (lastReferralCount !== null && pending > lastReferralCount) {
      toast(t('К вам направлен пациент'), 'info', t('Новое направление'));
      document.dispatchEvent(new CustomEvent('referrals:arrived'));
    }
    lastReferralCount = pending;
  } catch {
    // the next poll will retry
  }
}

function showApp(user) {
  store.user = user;
  loginScreen.classList.add('hidden');
  appElement.classList.remove('hidden');
  document.getElementById('user-menu').classList.remove('hidden');
  document.getElementById('menu-toggle').classList.remove('hidden');
  document.getElementById('header-phone').classList.add('hidden');
  lastReferralCount = null;
  setUserInterface(user);
  renderNav();
  if (!window.location.hash || window.location.hash === '#') {
    history.replaceState(null, '', `#/${defaultRouteName()}`);
  }
  handleRoute();
  checkHealth();
  refreshReferralBadge();
  if (user.role === 'PATIENT') {
    api.me.setLanguage(getLang()).catch(() => null);
  }
  if (!healthTimer) {
    healthTimer = window.setInterval(() => {
      checkHealth();
      refreshReferralBadge();
    }, 20000);
  }
}

function showLogin(message = '') {
  store.user = null;
  appElement.classList.add('hidden');
  loginScreen.classList.remove('hidden');
  document.getElementById('user-menu').classList.add('hidden');
  document.getElementById('menu-toggle').classList.add('hidden');
  document.getElementById('header-phone').classList.remove('hidden');
  document.getElementById('modal-root').replaceChildren();
  document.body.style.overflow = '';
  document.title = `${t('Вход')} | MedFlow`;
  setAuthMode('login');
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
    text.textContent = t('Система работает');
  } catch {
    store.health = null;
    dot.className = 'health-dot is-down';
    text.textContent = t('API недоступен');
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

function setAuthMode(mode) {
  const registering = mode === 'register';
  document.querySelectorAll('[data-auth-mode]').forEach((button) => {
    const active = button.dataset.authMode === mode;
    button.classList.toggle('is-active', active);
    button.setAttribute('aria-selected', String(active));
  });
  loginForm.classList.toggle('is-active', !registering);
  registerForm.classList.toggle('is-active', registering);
  if (registering) {
    loginError.classList.add('hidden');
  } else {
    registerError.classList.add('hidden');
    registerError.textContent = '';
  }
}

async function lookupIin() {
  const status = document.getElementById('register-iin-status');
  const iin = registerForm.elements.namedItem('iin').value;
  try {
    const result = await api.auth.lookupIin(iin);
    if (registerForm.elements.namedItem('iin').value !== iin) return;
    status.className = `form-alert ${result.status === 'REGISTERED' ? '' : result.status === 'CARD_FOUND' ? 'form-alert-success' : 'form-alert-info'}`;
    status.textContent = {
      NEW: t('Карты с этим ИИН в клинике нет, мы создадим новую.'),
      CARD_FOUND: t('Ваша карта есть в клинике. Укажите фамилию и телефон или email из карты, и мы привяжем её к кабинету.'),
      REGISTERED: t('С этим ИИН уже есть личный кабинет. Войдите с паролем.'),
    }[result.status];
  } catch {
    status.classList.add('hidden');
  }
}

function bindIin() {
  const input = registerForm.elements.namedItem('iin');
  input.addEventListener('input', () => {
    input.value = input.value.replace(/\D/g, '').slice(0, 12);
    document.getElementById('register-iin-status').classList.add('hidden');
    if (input.value.length !== 12) return;
    const parsed = parseIin(input.value);
    if (parsed) {
      registerForm.elements.namedItem('birthDate').value = parsed.birthDate;
      registerForm.elements.namedItem('gender').value = parsed.gender;
    }
    lookupIin();
  });
}

function bindLogin() {
  bindIin();
  document.querySelectorAll('[data-auth-mode]').forEach((button) => {
    button.addEventListener('click', () => setAuthMode(button.dataset.authMode));
  });

  document.querySelectorAll('.demo-account').forEach((button) => {
    button.addEventListener('click', () => {
      setAuthMode('login');
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
    const toggle = document.getElementById('toggle-password');
    toggle.dataset.i18nLabel = visible ? 'Показать пароль' : 'Скрыть пароль';
    toggle.setAttribute('aria-label', t(toggle.dataset.i18nLabel));
  });

  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    if (!username || !password) {
      loginError.textContent = t('Введите логин и пароль');
      loginError.classList.remove('hidden');
      return;
    }
    loginError.classList.add('hidden');
    loginSubmit.disabled = true;
    loginSubmit.innerHTML = '<span class="spinner spinner-light"></span><span>' + t('Проверяем доступ…') + '</span>';
    try {
      const user = await api.auth.login({ username, password });
      loginForm.reset();
      registerForm.reset();
      document.getElementById('register-iin-status').classList.add('hidden');
      toast(user.fullName, 'success', t('Вход выполнен'));
      showApp(user);
    } catch (error) {
      loginError.textContent = error?.status === 401
        ? t('Неверный логин или пароль. Проверьте раскладку и попробуйте снова.')
        : getErrorMessage(error);
      loginError.classList.remove('hidden');
    } finally {
      loginSubmit.disabled = false;
      loginSubmit.innerHTML = `<span data-i18n="Войти">${t('Войти')}</span>`;
    }
  });

  registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(registerForm);
    setButtonLoading(registerSubmit, true, t('Создаём кабинет…'));
    const payload = {
      iin: formValue(registerForm, 'iin'),
      lastName: formValue(registerForm, 'lastName'),
      firstName: formValue(registerForm, 'firstName'),
      middleName: formValue(registerForm, 'middleName') || null,
      birthDate: formValue(registerForm, 'birthDate') || null,
      gender: formValue(registerForm, 'gender') || null,
      phone: formValue(registerForm, 'phone'),
      email: formValue(registerForm, 'email'),
      password: registerForm.elements.namedItem('password').value,
      ...readAddress(registerForm),
    };
    try {
      const user = await api.auth.register(payload);
      registerForm.reset();
      document.getElementById('register-iin-status').classList.add('hidden');
      toast(user.fullName, 'success', t('Личный кабинет создан'));
      showApp(user);
    } catch (error) {
      showFormAlert(registerForm, getErrorMessage(error));
      applyFieldErrors(registerForm, error?.fieldErrors);
    } finally {
      setButtonLoading(registerSubmit, false);
    }
  });
}

function bindApp() {
  document.getElementById('menu-toggle').addEventListener('click', () => {
    const open = sidebar.classList.toggle('is-open');
    sidebarOverlay.classList.toggle('is-visible', open);
  });
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
    toast(t('Вы вышли из системы'), 'info');
  });

  window.addEventListener('hashchange', handleRoute);
  window.addEventListener('app:route', handleRoute);
  document.addEventListener('referrals:changed', refreshReferralBadge);
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
    showLogin(t('Сессия истекла. Войдите снова, чтобы продолжить работу.'));
    toast(t('Сессия истекла. Выполните вход повторно.'), 'info', t('Требуется вход'));
  });
}

async function boot() {
  await loadDictionary();
  translateStatic();
  renderLangSwitch();
  loadClinic();
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

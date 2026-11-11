const ICONS = {
  dashboard: '<rect x="3" y="3" width="7" height="9" rx="1.6"/><rect x="14" y="3" width="7" height="5" rx="1.6"/><rect x="14" y="12" width="7" height="9" rx="1.6"/><rect x="3" y="16" width="7" height="5" rx="1.6"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2.5"/><path d="M16 3v4M8 3v4M3 11h18"/><path d="M8 15h.01M12 15h.01M16 15h.01"/>',
  appointments: '<rect x="4" y="4" width="16" height="17" rx="2.5"/><path d="M9 4V2.8M15 4V2.8M8.5 10h7M8.5 14h7M8.5 18h4"/>',
  patients: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
  doctors: '<path d="M6 3v5a5 5 0 0 0 10 0V3"/><path d="M6 3H4.8M6 3h1.2M16 3h1.2M16 3h-1.2"/><path d="M11 13v3a5 5 0 0 0 5 5h.5"/><circle cx="19" cy="18" r="3"/>',
  schedules: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.2 1.9"/>',
  catalog: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z"/><path d="M8 7h8M8 11h6"/>',
  reports: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  x: '<path d="m6 6 12 12M18 6 6 18"/>',
  'chevron-left': '<path d="m15 18-6-6 6-6"/>',
  'chevron-right': '<path d="m9 18 6-6-6-6"/>',
  'chevron-down': '<path d="m6 9 6 6 6-6"/>',
  check: '<path d="m5 12 4.5 4.5L19 7"/>',
  'check-circle': '<circle cx="12" cy="12" r="9"/><path d="m8.5 12 2.5 2.5 4.5-5"/>',
  edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
  trash: '<path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2M19 6l-1 15a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1L5 6"/><path d="M10 11v6M14 11v6"/>',
  eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
  phone: '<path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2Z"/>',
  mail: '<rect x="2" y="4" width="20" height="16" rx="2.5"/><path d="m22 7-10 6L2 7"/>',
  'map-pin': '<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.8"/>',
  'alert-circle': '<circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16.5h.01"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.5h.01"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5M21 12H9"/>',
  menu: '<path d="M3 6h18M3 12h18M3 18h18"/>',
  refresh: '<path d="M3 12a9 9 0 0 1 15.5-6.2L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-15.5 6.2L3 16"/><path d="M3 21v-5h5"/>',
  download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5M12 15V3"/>',
  filter: '<path d="M22 3H2l8 9.5V19l4 2v-8.5L22 3Z"/>',
  'arrow-right': '<path d="M5 12h14M13 6l6 6-6 6"/>',
  ban: '<circle cx="12" cy="12" r="9"/><path d="m5.6 5.6 12.8 12.8"/>',
  wallet: '<path d="M20 7H5a2 2 0 0 1 0-4h13v4"/><path d="M3 5v14a2 2 0 0 0 2 2h15V7H5"/><path d="M16 14h.01"/>',
  activity: '<path d="M3 12h4l2.5-6 4.5 12 2.5-6h4.5"/>',
  'user-plus': '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/>',
  user: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  database: '<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v6c0 1.7 3.6 3 8 3s8-1.3 8-3V5"/><path d="M4 11v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6"/>',
  building: '<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 7h1M14 7h1M9 11h1M14 11h1M9 15h1M14 15h1M10 21v-3h4v3"/>',
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  briefcase: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M3 12h18"/>',
  'file-text': '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6M8 13h8M8 17h5"/>',
  'clipboard-check': '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V2.8M15 4V2.8M9 12l2 2 4-4"/>',
  'rotate-ccw': '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/>',
  'external-link': '<path d="M15 3h6v6M10 14 21 3"/><path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5"/>',
  printer: '<path d="M6 9V2h12v7"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8" rx="1"/>',
  sparkles: '<path d="m12 3 1.8 4.2L18 9l-4.2 1.8L12 15l-1.8-4.2L6 9l4.2-1.8L12 3Z"/><path d="m19 15 .8 1.9 1.9.8-1.9.8L19 21l-.8-1.9-1.9-.8 1.9-.8L19 15Z"/>',
  'trend-up': '<path d="m3 17 6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
};

export function icon(name, className = 'icon') {
  const body = ICONS[name] || ICONS.info;
  return `<svg class="${className}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${body}</svg>`;
}

export function esc(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

const MONTHS_GENITIVE = [
  'января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
  'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря',
];
const MONTHS_NOMINATIVE = [
  'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
  'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь',
];
export const WEEKDAYS_SHORT = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
export const WEEKDAYS_FULL = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];
export const WEEKDAYS_API = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export function parseISODate(value) {
  if (!value) {
    return null;
  }
  if (value instanceof Date) {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate());
  }
  const part = String(value).slice(0, 10);
  const [year, month, day] = part.split('-').map(Number);
  if (!year || !month || !day) {
    return null;
  }
  return new Date(year, month - 1, day);
}

export function toISODate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function todayISO() {
  return toISODate(new Date());
}

export function addDays(isoDate, days) {
  const date = parseISODate(isoDate || todayISO());
  date.setDate(date.getDate() + days);
  return toISODate(date);
}

export function formatDate(value) {
  const date = parseISODate(value);
  if (!date) {
    return '—';
  }
  return `${String(date.getDate()).padStart(2, '0')}.${String(date.getMonth() + 1).padStart(2, '0')}.${date.getFullYear()}`;
}

export function formatDateLong(value) {
  const date = parseISODate(value);
  if (!date) {
    return '—';
  }
  return `${date.getDate()} ${MONTHS_GENITIVE[date.getMonth()]} ${date.getFullYear()}`;
}

export function formatDateShort(value) {
  const date = parseISODate(value);
  if (!date) {
    return '—';
  }
  return `${date.getDate()} ${MONTHS_GENITIVE[date.getMonth()].slice(0, 3)}`;
}

export function formatTime(value) {
  if (!value) {
    return '—';
  }
  if (value instanceof Date) {
    return `${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
  }
  const text = String(value);
  if (text.includes('T')) {
    return text.slice(11, 16);
  }
  return text.slice(0, 5);
}

export function formatDateTime(value) {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.includes('T')) {
    return `${formatDate(text)} · ${text.slice(11, 16)}`;
  }
  return formatDate(text);
}

export function weekdayIndex(value) {
  const date = parseISODate(value);
  return date ? (date.getDay() + 6) % 7 : 0;
}

export function weekdayName(value, short = false) {
  const index = weekdayIndex(value);
  return (short ? WEEKDAYS_SHORT : WEEKDAYS_FULL)[index];
}

export function monthTitle(year, monthIndex) {
  return `${MONTHS_NOMINATIVE[monthIndex]} ${year}`;
}

export function monthGenitive(monthIndex) {
  return MONTHS_GENITIVE[monthIndex];
}

export function formatMoney(value) {
  const number = Number(value ?? 0);
  return `${new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(number)} ₽`;
}

export function formatNumber(value) {
  return new Intl.NumberFormat('ru-RU').format(Number(value ?? 0));
}

export function pluralRu(count, forms) {
  const number = Math.abs(Number(count) || 0) % 100;
  const last = number % 10;
  if (number > 10 && number < 20) {
    return forms[2];
  }
  if (last > 1 && last < 5) {
    return forms[1];
  }
  if (last === 1) {
    return forms[0];
  }
  return forms[2];
}

export function initials(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return '?';
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

export function avatarTone(seed) {
  const sum = String(seed || '').split('').reduce((total, char) => total + char.charCodeAt(0), 0);
  return ['', 'avatar-blue', 'avatar-warm', 'avatar-rose'][sum % 4];
}

export function avatar(name, extraClass = '') {
  return `<span class="avatar ${avatarTone(name)} ${extraClass}">${esc(initials(name))}</span>`;
}

export const STATUS_LABELS = {
  SCHEDULED: 'Запланирован',
  COMPLETED: 'Завершён',
  CANCELLED: 'Отменён',
};

export const STATUS_CLASSES = {
  SCHEDULED: 'status-scheduled',
  COMPLETED: 'status-completed',
  CANCELLED: 'status-cancelled',
};

export function statusBadge(status, label) {
  const className = STATUS_CLASSES[status] || 'badge-neutral';
  return `<span class="badge ${className} badge-dot">${esc(label || STATUS_LABELS[status] || status)}</span>`;
}

export function statusChip(status, label) {
  const className = STATUS_CLASSES[status] || 'badge-neutral';
  return `<span class="calendar-chip ${className}">${esc(label || STATUS_LABELS[status] || status)}</span>`;
}

export function roleLabel(role) {
  return {
    ADMIN: 'Администратор',
    DOCTOR: 'Врач',
    RECEPTIONIST: 'Регистратор',
  }[role] || role;
}

export function genderLabel(gender) {
  return { MALE: 'Мужской', FEMALE: 'Женский' }[gender] || '—';
}

export function dayOfWeekLabel(day) {
  return {
    MONDAY: 'Понедельник',
    TUESDAY: 'Вторник',
    WEDNESDAY: 'Среда',
    THURSDAY: 'Четверг',
    FRIDAY: 'Пятница',
    SATURDAY: 'Суббота',
    SUNDAY: 'Воскресенье',
  }[day] || day;
}

export function debounce(fn, delay = 300) {
  let timer = null;
  return (...args) => {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => fn(...args), delay);
  };
}

let toastCounter = 0;

export function toast(message, type = 'success', title = null) {
  const root = document.getElementById('toast-root');
  if (!root) {
    return;
  }
  const iconName = type === 'error' ? 'alert-circle' : type === 'info' ? 'info' : 'check-circle';
  const heading = title || (type === 'error' ? 'Ошибка' : type === 'info' ? 'Информация' : 'Готово');
  const element = document.createElement('div');
  toastCounter += 1;
  element.className = `toast toast-${type}`;
  element.setAttribute('role', type === 'error' ? 'alert' : 'status');
  element.innerHTML = `
    <span class="toast-icon">${icon(iconName)}</span>
    <div class="toast-copy">
      <strong>${esc(heading)}</strong>
      <span>${esc(message)}</span>
    </div>
    <button class="toast-close" type="button" aria-label="Закрыть уведомление">${icon('x')}</button>
  `;
  const remove = () => {
    if (!element.isConnected) {
      return;
    }
    element.classList.add('is-leaving');
    window.setTimeout(() => element.remove(), 210);
  };
  element.querySelector('.toast-close').addEventListener('click', remove);
  root.appendChild(element);
  window.setTimeout(remove, 4600 + toastCounter * 20);
}

const modalStack = [];
let modalKeyHandlerBound = false;

function syncBodyScroll() {
  document.body.style.overflow = modalStack.length > 0 ? 'hidden' : '';
}

function bindModalKeyboard() {
  if (modalKeyHandlerBound) {
    return;
  }
  modalKeyHandlerBound = true;
  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape' || modalStack.length === 0) {
      return;
    }
    const top = modalStack[modalStack.length - 1];
    if (!top.persistent) {
      top.close();
    }
  });
}

export function openModal({
  title,
  subtitle = '',
  body = '',
  footer = '',
  size = '',
  persistent = false,
  onClose = null,
}) {
  bindModalKeyboard();
  const previouslyFocused = document.activeElement;
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  const sizeClass = size ? `modal-${size}` : '';
  overlay.innerHTML = `
    <div class="modal ${sizeClass}" role="dialog" aria-modal="true" aria-label="${esc(title)}">
      <div class="modal-head">
        <div class="modal-head-copy">
          <h3>${esc(title)}</h3>
          ${subtitle ? `<p>${esc(subtitle)}</p>` : ''}
        </div>
        <button type="button" class="icon-btn modal-close" aria-label="Закрыть">${icon('x')}</button>
      </div>
      <div class="modal-body"></div>
      ${footer ? '<div class="modal-foot"></div>' : ''}
    </div>
  `;
  const modal = overlay.querySelector('.modal');
  const bodyElement = overlay.querySelector('.modal-body');
  const footerElement = overlay.querySelector('.modal-foot');
  if (typeof body === 'string') {
    bodyElement.innerHTML = body;
  } else if (body instanceof Node) {
    bodyElement.appendChild(body);
  }
  if (footerElement && footer) {
    if (typeof footer === 'string') {
      footerElement.innerHTML = footer;
    } else if (footer instanceof Node) {
      footerElement.appendChild(footer);
    }
  }

  let closed = false;
  const entry = {
    overlay,
    persistent,
    close() {
      if (closed) {
        return;
      }
      closed = true;
      const index = modalStack.indexOf(entry);
      if (index >= 0) {
        modalStack.splice(index, 1);
      }
      overlay.remove();
      syncBodyScroll();
      if (previouslyFocused && typeof previouslyFocused.focus === 'function'
          && document.contains(previouslyFocused)) {
        previouslyFocused.focus({ preventScroll: true });
      }
      if (typeof onClose === 'function') {
        onClose();
      }
    },
  };

  overlay.querySelector('.modal-close').addEventListener('click', () => entry.close());
  overlay.addEventListener('mousedown', (event) => {
    if (event.target === overlay && !persistent) {
      entry.close();
    }
  });

  document.getElementById('modal-root').appendChild(overlay);
  modalStack.push(entry);
  syncBodyScroll();

  window.setTimeout(() => {
    const focusable = modal.querySelector('input:not([type="hidden"]), select, textarea, button.btn-primary');
    if (focusable) {
      focusable.focus({ preventScroll: true });
    }
  }, 30);

  return {
    overlay,
    modal,
    body: bodyElement,
    footer: footerElement,
    close: entry.close,
  };
}

export function confirmDialog({
  title,
  message,
  confirmText = 'Подтвердить',
  cancelText = 'Отмена',
  danger = true,
  iconName = 'alert-circle',
}) {
  return new Promise((resolve) => {
    let settled = false;
    const settle = (value) => {
      if (!settled) {
        settled = true;
        resolve(value);
      }
    };
    const modal = openModal({
      title,
      size: 'sm',
      body: `
        <div class="info-note">
          ${icon(iconName)}
          <span>${esc(message)}</span>
        </div>
      `,
      footer: `
        <button type="button" class="btn btn-secondary" data-cancel>${esc(cancelText)}</button>
        <button type="button" class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-confirm>${esc(confirmText)}</button>
      `,
      onClose: () => settle(false),
    });
    modal.footer.querySelector('[data-cancel]').addEventListener('click', () => modal.close());
    modal.footer.querySelector('[data-confirm]').addEventListener('click', () => {
      settle(true);
      modal.close();
    });
  });
}

export function emptyState({ iconName = 'search', title, text = '', action = '' }) {
  return `
    <div class="empty-state">
      <span class="empty-icon">${icon(iconName)}</span>
      <strong>${esc(title)}</strong>
      ${text ? `<p>${esc(text)}</p>` : ''}
      ${action}
    </div>
  `;
}

export function loadingState(text = 'Загружаем данные…') {
  return `<div class="page-loading"><span class="spinner"></span><span>${esc(text)}</span></div>`;
}

export function skeletonCards(count = 4) {
  return `
    <div class="skeleton-grid">
      ${Array.from({ length: count }, () => `
        <div class="skeleton-card">
          <div class="skeleton skeleton-line w40"></div>
          <div class="skeleton skeleton-line h24 w70"></div>
          <div class="skeleton skeleton-line w90"></div>
        </div>
      `).join('')}
    </div>
  `;
}

export function errorState(message, { retry = true } = {}) {
  return `
    <div class="card">
      ${emptyState({
        iconName: 'alert-circle',
        title: 'Не удалось загрузить данные',
        text: message,
        action: retry ? `<button type="button" class="btn btn-secondary btn-sm" data-retry>${icon('refresh')}<span>Повторить</span></button>` : '',
      })}
    </div>
  `;
}

export function setButtonLoading(button, loading, text = 'Подождите…') {
  if (!button) {
    return;
  }
  if (loading) {
    if (!button.dataset.originalHtml) {
      button.dataset.originalHtml = button.innerHTML;
    }
    button.disabled = true;
    button.innerHTML = `<span class="spinner spinner-light"></span><span>${esc(text)}</span>`;
  } else {
    button.disabled = false;
    if (button.dataset.originalHtml) {
      button.innerHTML = button.dataset.originalHtml;
      delete button.dataset.originalHtml;
    }
  }
}

export function clearFieldErrors(form) {
  form.querySelectorAll('.is-invalid').forEach((element) => element.classList.remove('is-invalid'));
  form.querySelectorAll('.field-error[data-generated]').forEach((element) => element.remove());
  const alert = form.querySelector('[data-form-alert]');
  if (alert) {
    alert.classList.add('hidden');
    alert.textContent = '';
  }
}

export function showFormAlert(form, message) {
  const alert = form.querySelector('[data-form-alert]');
  if (!alert) {
    toast(message, 'error');
    return;
  }
  alert.classList.remove('hidden');
  alert.classList.add('form-alert');
  alert.textContent = message;
}

export function applyFieldErrors(form, fieldErrors) {
  if (!fieldErrors) {
    return;
  }
  Object.entries(fieldErrors).forEach(([field, message]) => {
    const input = form.elements.namedItem(field);
    if (!input) {
      return;
    }
    const element = input instanceof RadioNodeList ? input[0] : input;
    element.classList.add('is-invalid');
    const fieldWrapper = element.closest('.field');
    if (!fieldWrapper) {
      return;
    }
    const error = document.createElement('div');
    error.className = 'field-error';
    error.dataset.generated = 'true';
    error.textContent = message;
    fieldWrapper.appendChild(error);
  });
}

export function formValue(form, name) {
  const element = form.elements.namedItem(name);
  if (!element) {
    return '';
  }
  if (element instanceof RadioNodeList) {
    return element.value;
  }
  return typeof element.value === 'string' ? element.value.trim() : element.value;
}

export function setFormValues(form, values) {
  Object.entries(values || {}).forEach(([name, value]) => {
    const element = form.elements.namedItem(name);
    if (!element) {
      return;
    }
    if (element instanceof RadioNodeList) {
      Array.from(element).forEach((radio) => {
        radio.checked = String(radio.value) === String(value);
      });
      return;
    }
    if (element.type === 'checkbox') {
      element.checked = Boolean(value);
      return;
    }
    element.value = value ?? '';
  });
}

export function csvDownload(filename, rows) {
  const csv = rows
    .map((row) => row.map((cell) => {
      const value = String(cell ?? '').replaceAll('"', '""');
      return `"${value}"`;
    }).join(';'))
    .join('\r\n');
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}

export function headerCell(label, className = '') {
  return `<th ${className ? `class="${className}"` : ''}>${esc(label)}</th>`;
}

export function segmentedControl(items, active) {
  return `
    <div class="tabs" role="tablist">
      ${items.map((item) => `
        <button type="button" class="tab ${item.value === active ? 'is-active' : ''}" data-tab="${esc(item.value)}" role="tab" aria-selected="${item.value === active}">
          ${esc(item.label)}
          ${item.count !== undefined ? `<span class="tab-count">${item.count}</span>` : ''}
        </button>
      `).join('')}
    </div>
  `;
}

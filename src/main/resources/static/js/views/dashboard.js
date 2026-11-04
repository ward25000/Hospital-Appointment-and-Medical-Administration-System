import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  formatTime,
  formatDateLong,
  formatDateShort,
  formatMoney,
  formatNumber,
  todayISO,
  statusBadge,
  initials,
  avatarTone,
  emptyState,
  errorState,
  pluralRu,
  WEEKDAYS_SHORT,
  weekdayIndex,
} from '../ui.js';
import { canSchedule, hasRole, navigate } from '../store.js';
import { openBooking, openDetails } from './appointments.js';

function greeting() {
  const hour = new Date().getHours();
  if (hour < 5) return 'Доброй ночи';
  if (hour < 12) return 'Доброе утро';
  if (hour < 18) return 'Добрый день';
  return 'Добрый вечер';
}

function firstName(fullName) {
  return String(fullName || '').split(/\s+/)[0] || 'коллега';
}

function quickAction({ action, iconName, title, subtitle, route = null }) {
  return `
    <button type="button" class="quick-action" ${action ? `data-action="${action}"` : ''} ${route ? `data-route="${route}"` : ''}>
      <span class="quick-action-icon">${icon(iconName)}</span>
      <span><strong>${esc(title)}</strong><small>${esc(subtitle)}</small></span>
      ${icon('chevron-right')}
    </button>
  `;
}

function appointmentRow(appointment) {
  return `
    <div class="appointment-item" role="button" tabindex="0" data-appointment-id="${appointment.id}">
      <div class="appointment-time">
        ${formatTime(appointment.startTime)}
        <small>${formatTime(appointment.endTime)}</small>
      </div>
      <div class="appointment-main">
        <strong>${esc(appointment.patientName)}</strong>
        <small>${esc(appointment.doctorName)} · ${esc(appointment.serviceName)}</small>
      </div>
      <div class="appointment-side">
        <span class="appointment-price">${formatMoney(appointment.price)}</span>
        ${statusBadge(appointment.status, appointment.statusLabel)}
      </div>
    </div>
  `;
}

function upcomingRow(appointment) {
  const day = formatDateShort(appointment.startTime);
  const weekday = WEEKDAYS_SHORT[weekdayIndex(appointment.startTime)];
  return `
    <div class="upcoming-item" role="button" tabindex="0" data-appointment-id="${appointment.id}">
      <div class="upcoming-date">
        <strong>${day.split(' ')[0]}</strong>
        <small>${weekday}</small>
      </div>
      <div class="upcoming-body">
        <strong>${esc(appointment.patientName)}</strong>
        <small>${formatTime(appointment.startTime)} · ${esc(appointment.doctorName)}</small>
      </div>
      ${icon('chevron-right', 'icon-sm')}
    </div>
  `;
}

function buildMarkup(data, user) {
  const isDoctor = user.role === 'DOCTOR';
  const appointmentsToday = data.todayAppointments || [];
  const upcoming = data.upcomingAppointments || [];

  const kpis = [
    {
      label: 'Приёмов сегодня',
      value: formatNumber(data.todayTotal),
      iconName: 'calendar',
      tone: '',
      foot: `${pluralRu(data.todayTotal, ['запись', 'записи', 'записей'])} в расписании`,
    },
    {
      label: 'Ближайшие приёмы',
      value: formatNumber(data.upcomingTotal),
      iconName: 'clock',
      tone: 'blue',
      foot: 'запланировано на будущее',
    },
    {
      label: 'Завершено за месяц',
      value: formatNumber(data.completedThisMonth),
      iconName: 'clipboard-check',
      tone: 'amber',
      foot: `<strong>${formatNumber(data.uniquePatientsThisMonth)}</strong> уникальных пациентов`,
    },
    {
      label: 'Выручка за месяц',
      value: formatMoney(data.revenueThisMonth),
      iconName: 'wallet',
      tone: 'rose',
      foot: 'по завершённым приёмам',
      compact: true,
    },
  ];

  const actions = [
    ...(canSchedule() ? [
      quickAction({
        action: 'booking',
        iconName: 'plus',
        title: 'Новая запись',
        subtitle: 'Подобрать свободный слот врача',
      }),
      quickAction({
        route: 'patients?new=1',
        iconName: 'user-plus',
        title: 'Добавить пациента',
        subtitle: 'Создать карточку в картотеке',
      }),
    ] : []),
    quickAction({
      route: 'calendar',
      iconName: 'calendar',
      title: 'Календарь приёмов',
      subtitle: 'Загрузка клиники по дням',
    }),
    ...(isDoctor ? [
      quickAction({
        route: 'schedules',
        iconName: 'schedules',
        title: 'Моё расписание',
        subtitle: 'Смены и интервалы приёма',
      }),
    ] : []),
    ...(hasRole('ADMIN') ? [
      quickAction({
        route: 'reports',
        iconName: 'reports',
        title: 'Отчёты клиники',
        subtitle: 'Выручка и нагрузка врачей',
      }),
    ] : []),
    ...(hasRole('RECEPTIONIST') ? [
      quickAction({
        route: 'reports',
        iconName: 'reports',
        title: 'Отчёты',
        subtitle: 'Статистика приёмов за период',
      }),
    ] : []),
  ];

  return `
    <div class="stack">
      <section class="greeting-strip">
        <div>
          <span class="greeting-badge">${esc(greeting())}, ${esc(firstName(user.fullName))}</span>
          <h2>${isDoctor ? 'Ваш приёмный день под контролем' : 'Клиника работает по плану'}</h2>
          <p>${isDoctor
            ? 'Ниже — ваши приёмы на сегодня и ближайшие записи пациентов.'
            : 'Сводка по записям, загрузке специалистов и выручке за текущий месяц.'}</p>
        </div>
        <div class="greeting-date">${formatDateLong(data.date || todayISO())}</div>
      </section>

      <section class="kpi-grid">
        ${kpis.map((kpi) => `
          <article class="kpi-card">
            <div class="kpi-top">
              <span class="kpi-label">${esc(kpi.label)}</span>
              <span class="kpi-icon ${kpi.tone}">${icon(kpi.iconName)}</span>
            </div>
            <div class="kpi-value" ${kpi.compact ? 'style="font-size:23px"' : ''}>${esc(kpi.value)}</div>
            <div class="kpi-foot">${kpi.foot}</div>
          </article>
        `).join('')}
      </section>

      <div class="dashboard-grid">
        <div class="stack">
          <section class="card">
            <div class="card-header">
              <div>
                <div class="card-title">Приёмы сегодня</div>
                <div class="card-subtitle">${appointmentsToday.length
                  ? `${appointmentsToday.length} ${pluralRu(appointmentsToday.length, ['запись', 'записи', 'записей'])} в расписании`
                  : 'На сегодня записей нет'}</div>
              </div>
              <a class="link-button" href="#/appointments">Все записи ${icon('arrow-right')}</a>
            </div>
            ${appointmentsToday.length
              ? `<div class="appointment-list">${appointmentsToday.map(appointmentRow).join('')}</div>`
              : emptyState({
                iconName: 'calendar',
                title: 'Сегодня приёмов нет',
                text: canSchedule()
                  ? 'Создайте запись — система проверит свободные слоты врача.'
                  : 'Расписание на сегодня свободно.',
                action: canSchedule()
                  ? `<button type="button" class="btn btn-primary btn-sm" data-action="booking">${icon('plus')}<span>Создать запись</span></button>`
                  : '',
              })}
          </section>

          <section class="card">
            <div class="card-header">
              <div>
                <div class="card-title">Ближайшие приёмы</div>
                <div class="card-subtitle">Следующие подтверждённые визиты</div>
              </div>
              <a class="link-button" href="#/calendar">Календарь ${icon('arrow-right')}</a>
            </div>
            ${upcoming.length
              ? `<div class="upcoming-list">${upcoming.map(upcomingRow).join('')}</div>`
              : emptyState({
                iconName: 'clock',
                title: 'Ближайших записей нет',
                text: 'На будущие даты пока не создано ни одного приёма.',
              })}
          </section>
        </div>

        <div class="stack">
          <section class="card">
            <div class="card-header">
              <div>
                <div class="card-title">Быстрые действия</div>
                <div class="card-subtitle">Частые операции в один клик</div>
              </div>
            </div>
            <div class="quick-actions">
              ${actions.join('')}
            </div>
          </section>

          <section class="card card-pad">
            <div class="flex items-center gap-12">
              <span class="avatar avatar-lg avatar-blue">${esc(initials(user.fullName))}</span>
              <div>
                <div class="cell-main">${esc(user.fullName)}</div>
                <div class="cell-sub">${esc(user.roleLabel || 'Сотрудник клиники')}</div>
              </div>
            </div>
            <div class="info-note mt-16">
              ${icon('shield')}
              <span>Данные отображаются с учётом вашей роли. Сервер дополнительно проверяет права на каждую операцию.</span>
            </div>
          </section>
        </div>
      </div>
    </div>
  `;
}

export async function render(root, ctx) {
  root.innerHTML = `
    <div class="page-loading"><span class="spinner"></span><span>Загружаем сводку клиники…</span></div>
  `;

  let data;
  try {
    data = await api.dashboard();
  } catch (error) {
    root.innerHTML = errorState(getErrorMessage(error));
    root.querySelector('[data-retry]')?.addEventListener('click', () => render(root, ctx));
    return;
  }

  root.innerHTML = buildMarkup(data, ctx.user);
  const refresh = () => render(root, ctx);

  root.querySelectorAll('[data-appointment-id]').forEach((element) => {
    const open = () => openDetails(Number(element.dataset.appointmentId), { onChanged: refresh });
    element.addEventListener('click', open);
    element.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        open();
      }
    });
  });

  root.querySelectorAll('[data-action="booking"]').forEach((button) => {
    button.addEventListener('click', () => openBooking({ onDone: refresh }));
  });

  root.querySelectorAll('[data-route]').forEach((button) => {
    button.addEventListener('click', () => navigate(button.dataset.route));
  });
}

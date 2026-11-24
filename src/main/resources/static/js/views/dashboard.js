import { api, getErrorMessage } from '../api.js';
import { t } from '../i18n.js';
import {
  icon,
  esc,
  formatTime,
  formatDateLong,
  formatDateShort,
  formatMoney,
  formatNumber,
  statusBadge,
  emptyState,
  errorState,
  pluralRu,
  weekdaysShort,
  weekdayIndex,
} from '../ui.js';
import { canSchedule, hasRole, navigate } from '../store.js';
import { openBooking, openDetails, openComplete, markNoShow } from './appointments.js';
import { openPatientForm } from './patients.js';

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
        ${statusBadge(appointment.status)}
      </div>
    </div>
  `;
}

function upcomingRow(appointment) {
  const day = formatDateShort(appointment.startTime);
  const weekday = weekdaysShort()[weekdayIndex(appointment.startTime)];
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
  const unmarked = data.unmarkedAppointments || [];
  const canMark = hasRole('ADMIN', 'DOCTOR');

  const kpis = [
    {
      label: t('Приёмов сегодня'),
      value: formatNumber(data.todayTotal),
      iconName: 'calendar',
      tone: '',
      foot: t('{0} в расписании', [pluralRu(data.todayTotal, ['запись', 'записи', 'записей'])]),
    },
    {
      label: t('Ближайшие приёмы'),
      value: formatNumber(data.upcomingTotal),
      iconName: 'clock',
      tone: 'blue',
      foot: t('запланировано на будущее'),
    },
    {
      label: t('Завершено за месяц'),
      value: formatNumber(data.completedThisMonth),
      iconName: 'clipboard-check',
      tone: 'amber',
      foot: `<strong>${formatNumber(data.uniquePatientsThisMonth)}</strong> ${t('уникальных пациентов')}`,
    },
    {
      label: t('Выручка за месяц'),
      value: formatMoney(data.revenueThisMonth),
      iconName: 'wallet',
      tone: 'rose',
      foot: t('по завершённым приёмам'),
      compact: true,
    },
  ];

  const actions = [
    ...(canSchedule() ? [
      quickAction({
        action: 'booking',
        iconName: 'plus',
        title: t('Новая запись'),
        subtitle: t('Подобрать свободный слот врача'),
      }),
      quickAction({
        route: 'patients?new=1',
        iconName: 'user-plus',
        title: t('Добавить пациента'),
        subtitle: t('Создать карточку в картотеке'),
      }),
    ] : []),
    quickAction({
      route: 'calendar',
      iconName: 'calendar',
      title: t('Календарь приёмов'),
      subtitle: t('Загрузка клиники по дням'),
    }),
    ...(isDoctor ? [
      quickAction({
        route: 'referral-inbox',
        iconName: 'inbox',
        title: t('Направления'),
        subtitle: t('Пациенты, направленные к вам'),
      }),
      quickAction({
        action: 'patient',
        iconName: 'user-plus',
        title: t('Карта пациента'),
        subtitle: t('Завести новую карту по ИИН'),
      }),
      quickAction({
        route: 'schedules',
        iconName: 'schedules',
        title: t('Моё расписание'),
        subtitle: t('Смены и интервалы приёма'),
      }),
    ] : []),
    ...(hasRole('ADMIN') ? [
      quickAction({
        route: 'reports',
        iconName: 'reports',
        title: t('Отчёты клиники'),
        subtitle: t('Выручка и нагрузка врачей'),
      }),
    ] : []),
    ...(hasRole('RECEPTIONIST') ? [
      quickAction({
        route: 'reports',
        iconName: 'reports',
        title: t('Отчёты'),
        subtitle: t('Статистика приёмов за период'),
      }),
    ] : []),
  ];

  return `
    <div class="stack">
      ${unmarked.length ? `
      <section class="attention">
        <div class="attention-head"><strong>${t('Отметьте, состоялся ли приём')}: ${unmarked.length}</strong>
          <p>${t('Время этих визитов прошло, а статус не указан. Без отметки визит не попадёт в историю пациента.')}</p></div>
        ${unmarked.slice(0, 5).map((visit) => `
          <div class="attention-row">
            <div><strong class="cell-main">${esc(visit.patientName)}</strong>
              <div class="muted">${formatDateLong(visit.startTime)}, ${formatTime(visit.startTime)}${isDoctor ? '' : `, ${esc(visit.doctorName)}`}</div></div>
            ${canMark ? `<div class="flex gap-8"><button type="button" class="btn btn-primary btn-sm" data-attended="${visit.id}">${t('Прошел')}</button>
              <button type="button" class="btn btn-danger-soft btn-sm" data-absent="${visit.id}">${t('Не явился')}</button></div>` : ''}
          </div>`).join('')}
      </section>` : ''}

      <section class="kpi-grid">
        ${kpis.map((kpi) => `
          <article class="kpi-card">
            <div class="kpi-top">
              <span class="kpi-label">${esc(kpi.label)}</span>
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
                <div class="card-title">${t('Приёмы сегодня')}</div>
                <div class="card-subtitle">${appointmentsToday.length
                  ? t('{0} {1} в расписании', [appointmentsToday.length, pluralRu(appointmentsToday.length, ['запись', 'записи', 'записей'])])
                  : t('На сегодня записей нет')}</div>
              </div>
              <a class="link-button" href="#/appointments">${t('Все записи')} ${icon('arrow-right')}</a>
            </div>
            ${appointmentsToday.length
              ? `<div class="appointment-list">${appointmentsToday.map(appointmentRow).join('')}</div>`
              : emptyState({
                iconName: 'calendar',
                title: t('Сегодня приёмов нет'),
                text: canSchedule()
                  ? t('Создайте запись — система проверит свободные слоты врача.')
                  : t('Расписание на сегодня свободно.'),
                action: canSchedule()
                  ? `<button type="button" class="btn btn-primary btn-sm" data-action="booking">${icon('plus')}<span>${t('Создать запись')}</span></button>`
                  : '',
              })}
          </section>

          <section class="card">
            <div class="card-header">
              <div>
                <div class="card-title">${t('Ближайшие приёмы')}</div>
                <div class="card-subtitle">${t('Следующие подтверждённые визиты')}</div>
              </div>
              <a class="link-button" href="#/calendar">${t('Календарь')} ${icon('arrow-right')}</a>
            </div>
            ${upcoming.length
              ? `<div class="upcoming-list">${upcoming.map(upcomingRow).join('')}</div>`
              : emptyState({
                iconName: 'clock',
                title: t('Ближайших записей нет'),
                text: t('На будущие даты пока не создано ни одного приёма.'),
              })}
          </section>
        </div>

        <div class="stack">
          <section class="card">
            <div class="card-header">
              <div>
                <div class="card-title">${t('Быстрые действия')}</div>
                <div class="card-subtitle">${t('Частые операции в один клик')}</div>
              </div>
            </div>
            <div class="quick-actions">
              ${actions.join('')}
            </div>
          </section>
        </div>
      </div>
    </div>
  `;
}

export async function render(root, ctx) {
  root.innerHTML = `
    <div class="page-loading"><span class="spinner"></span><span>${t('Загружаем сводку клиники…')}</span></div>
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

  root.querySelectorAll('[data-attended]').forEach((button) => {
    button.addEventListener('click', () => openComplete(Number(button.dataset.attended), { onDone: refresh }));
  });
  root.querySelectorAll('[data-absent]').forEach((button) => {
    button.addEventListener('click', () => markNoShow(Number(button.dataset.absent), refresh));
  });
  root.querySelectorAll('[data-action="patient"]').forEach((button) => {
    button.addEventListener('click', () => openPatientForm());
  });
  root.querySelectorAll('[data-action="booking"]').forEach((button) => {
    button.addEventListener('click', () => openBooking({ onDone: refresh }));
  });

  root.querySelectorAll('[data-route]').forEach((button) => {
    button.addEventListener('click', () => navigate(button.dataset.route));
  });
}

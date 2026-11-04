import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  statusBadge,
  statusChip,
  formatDateLong,
  formatTime,
  formatMoney,
  formatNumber,
  todayISO,
  addDays,
  toISODate,
  parseISODate,
  monthTitle,
  weekdayName,
  weekdayIndex,
  WEEKDAYS_SHORT,
  openModal,
  toast,
  emptyState,
  errorState,
  pluralRu,
} from '../ui.js';
import { canSchedule, hasRole } from '../store.js';
import { openBooking, openDetails } from './appointments.js';

function firstOfMonth(date = new Date()) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function gridStartFor(monthDate) {
  const first = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
  const offset = (first.getDay() + 6) % 7;
  first.setDate(first.getDate() - offset);
  return first;
}

const state = {
  month: firstOfMonth(),
  selected: todayISO(),
  doctorId: '',
  status: '',
};

let doctorsCache = [];

function monthCells(monthDate) {
  const start = gridStartFor(monthDate);
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return date;
  });
}

function appointmentsForDate(appointments, isoDate, status) {
  return appointments
    .filter((appointment) => appointment.startTime.slice(0, 10) === isoDate)
    .filter((appointment) => !status || appointment.status === status)
    .sort((left, right) => left.startTime.localeCompare(right.startTime));
}

function renderGrid(container, appointments, ctx) {
  const today = todayISO();
  const monthIndex = state.month.getMonth();
  const cells = monthCells(state.month);

  container.innerHTML = cells.map((date) => {
    const iso = toISODate(date);
    const dayAppointments = appointmentsForDate(appointments, iso, state.status);
    const outside = date.getMonth() !== monthIndex;
    return `
      <button type="button"
              class="calendar-day ${outside ? 'is-outside' : ''} ${iso === state.selected ? 'is-selected' : ''} ${iso === today ? 'is-today' : ''}"
              data-date="${iso}"
              aria-label="${esc(formatDateLong(iso))}, ${dayAppointments.length} приёмов">
        <span class="day-number">${date.getDate()}</span>
        <span class="calendar-chips">
          ${dayAppointments.slice(0, 2).map((appointment) => `
            <span class="calendar-chip ${appointment.status === 'SCHEDULED' ? 'status-scheduled' : appointment.status === 'COMPLETED' ? 'status-completed' : 'status-cancelled'}">
              ${formatTime(appointment.startTime)} ${esc(appointment.patientName.split(' ')[0])}
            </span>
          `).join('')}
          ${dayAppointments.length > 2
            ? `<span class="calendar-more">+${dayAppointments.length - 2} ещё</span>`
            : ''}
        </span>
      </button>
    `;
  }).join('');

  container.querySelectorAll('[data-date]').forEach((button) => {
    button.addEventListener('click', () => {
      state.selected = button.dataset.date;
      container.querySelectorAll('.calendar-day').forEach((item) => {
        item.classList.toggle('is-selected', item === button);
      });
      renderDayPanel(appointments, ctx);
    });
  });
}

function renderDayPanel(appointments, ctx) {
  const title = document.querySelector('[data-day-title]');
  const subtitle = document.querySelector('[data-day-subtitle]');
  const list = document.querySelector('[data-day-list]');
  const foot = document.querySelector('[data-day-foot]');
  if (!title || !subtitle || !list || !foot) {
    return;
  }
  const dayAppointments = appointmentsForDate(appointments, state.selected, state.status);

  title.textContent = formatDateLong(state.selected);
  subtitle.textContent = `${weekdayName(state.selected)} · ${dayAppointments.length} ${pluralRu(dayAppointments.length, ['приём', 'приёма', 'приёмов'])}`;

  if (dayAppointments.length === 0) {
    list.innerHTML = emptyState({
      iconName: 'calendar',
      title: 'На этот день записей нет',
      text: 'Выберите другой день или создайте новую запись.',
    });
  } else {
    list.innerHTML = dayAppointments.map((appointment) => `
      <div class="day-panel-item" role="button" tabindex="0" data-appointment-id="${appointment.id}">
        <div class="day-panel-time">${formatTime(appointment.startTime)}</div>
        <div class="day-panel-body">
          <strong>${esc(appointment.patientName)}</strong>
          <small>${esc(appointment.doctorName)} · ${esc(appointment.serviceName)}</small>
          <span>${statusBadge(appointment.status, appointment.statusLabel)}</span>
        </div>
        <span class="appointment-price">${formatMoney(appointment.price)}</span>
      </div>
    `).join('');

    list.querySelectorAll('[data-appointment-id]').forEach((element) => {
      const open = () => openDetails(Number(element.dataset.appointmentId), {
        onChanged: () => window.dispatchEvent(new Event('calendar:refresh')),
      });
      element.addEventListener('click', open);
      element.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          open();
        }
      });
    });
  }

  const bookingDate = state.selected < todayISO() ? todayISO() : state.selected;
  foot.innerHTML = canSchedule()
    ? `<button type="button" class="btn btn-primary btn-block btn-sm" data-book-day>${icon('plus')}<span>Записаться на ${formatDateLong(bookingDate)}</span></button>`
    : `<span class="muted" style="font-size:11px">Показаны приёмы вашего расписания</span>`;
  foot.querySelector('[data-book-day]')?.addEventListener('click', () => {
    openBooking({
      date: bookingDate,
      doctorId: state.doctorId || undefined,
      onDone: () => window.dispatchEvent(new Event('calendar:refresh')),
    });
  });
}

export async function render(root, ctx) {
  if (!hasRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')) {
    return;
  }

  root.innerHTML = `
    <div class="calendar-layout">
      <section class="card calendar-card">
        <div class="calendar-toolbar">
          <div class="flex items-center gap-4">
            <button type="button" class="icon-btn" data-prev title="Предыдущий месяц" aria-label="Предыдущий месяц">${icon('chevron-left')}</button>
            <button type="button" class="icon-btn" data-next title="Следующий месяц" aria-label="Следующий месяц">${icon('chevron-right')}</button>
          </div>
          <div class="calendar-month" data-month></div>
          <button type="button" class="btn btn-secondary btn-sm" data-today>Сегодня</button>
          <div class="calendar-nav">
            <button type="button" class="btn btn-secondary btn-sm" data-list>${icon('appointments')}<span>Журнал</span></button>
            ${canSchedule() ? `<button type="button" class="btn btn-primary btn-sm" data-new>${icon('plus')}<span>Запись</span></button>` : ''}
          </div>
        </div>
        <div class="calendar-filters">
          ${hasRole('ADMIN', 'RECEPTIONIST') ? `<select class="select" data-doctor-filter></select>` : ''}
          <select class="select" data-status-filter>
            <option value="">Все статусы</option>
            <option value="SCHEDULED">Запланированные</option>
            <option value="COMPLETED">Завершённые</option>
            <option value="CANCELLED">Отменённые</option>
          </select>
          <span class="spacer"></span>
          <span class="metric-inline">${icon('info')} <span>Нажмите на день, чтобы увидеть детали</span></span>
        </div>
        <div class="calendar-weekdays">
          ${WEEKDAYS_SHORT.map((day) => `<span>${day}</span>`).join('')}
        </div>
        <div class="calendar-grid" data-grid>
          <div style="grid-column:1/-1">${emptyState({ iconName: 'calendar', title: 'Загружаем календарь…' })}</div>
        </div>
      </section>

      <aside class="card day-panel">
        <div class="day-panel-head">
          <div>
            <h3 data-day-title>${formatDateLong(state.selected)}</h3>
            <p data-day-subtitle>Загрузка…</p>
          </div>
          ${icon('calendar', 'icon-sm')}
        </div>
        <div class="day-panel-list" data-day-list></div>
        <div class="day-panel-foot" data-day-foot></div>
      </aside>
    </div>
  `;

  const grid = root.querySelector('[data-grid]');
  const monthLabel = root.querySelector('[data-month]');
  const doctorFilter = root.querySelector('[data-doctor-filter]');
  const statusFilter = root.querySelector('[data-status-filter]');
  statusFilter.value = state.status;

  if (doctorFilter) {
    try {
      doctorsCache = await api.doctors.list();
      doctorFilter.innerHTML = `
        <option value="">Все врачи</option>
        ${doctorsCache.map((doctor) => `
          <option value="${doctor.id}" ${String(doctor.id) === String(state.doctorId) ? 'selected' : ''}>${esc(doctor.fullName)}</option>
        `).join('')}
      `;
    } catch {
      doctorsCache = [];
    }
  }

  let appointments = [];
  let sequence = 0;

  const load = async () => {
    const current = ++sequence;
    monthLabel.textContent = monthTitle(state.month.getFullYear(), state.month.getMonth());
    const cells = monthCells(state.month);
    const from = toISODate(cells[0]);
    const to = toISODate(cells[cells.length - 1]);

    grid.innerHTML = `<div style="grid-column:1/-1">${emptyState({ iconName: 'calendar', title: 'Загружаем приёмы…', text: `${formatDateLong(from)} — ${formatDateLong(to)}` })}</div>`;
    try {
      const result = await api.appointments.list({
        from,
        to,
        doctorId: state.doctorId || undefined,
        status: state.status || undefined,
      });
      if (current !== sequence) {
        return;
      }
      appointments = result;
      renderGrid(grid, appointments, ctx);
      renderDayPanel(appointments, ctx);
    } catch (error) {
      if (current !== sequence) {
        return;
      }
      grid.innerHTML = `<div style="grid-column:1/-1">${errorState(getErrorMessage(error))}</div>`;
      grid.querySelector('[data-retry]')?.addEventListener('click', load);
      const list = root.querySelector('[data-day-list]');
      if (list) list.innerHTML = '';
    }
  };

  const shiftMonth = (offset) => {
    state.month = new Date(state.month.getFullYear(), state.month.getMonth() + offset, 1);
    state.selected = toISODate(state.month);
    load();
  };

  root.querySelector('[data-prev]').addEventListener('click', () => shiftMonth(-1));
  root.querySelector('[data-next]').addEventListener('click', () => shiftMonth(1));
  root.querySelector('[data-today]').addEventListener('click', () => {
    state.month = firstOfMonth();
    state.selected = todayISO();
    load();
  });
  root.querySelector('[data-list]').addEventListener('click', () => {
    window.location.hash = '#/appointments';
  });
  root.querySelector('[data-new]')?.addEventListener('click', () => openBooking({
    date: state.selected < todayISO() ? todayISO() : state.selected,
    doctorId: state.doctorId || undefined,
    onDone: load,
  }));

  doctorFilter?.addEventListener('change', () => {
    state.doctorId = doctorFilter.value;
    load();
  });
  statusFilter.addEventListener('change', () => {
    state.status = statusFilter.value;
    load();
  });

  const refreshHandler = () => load();
  window.addEventListener('calendar:refresh', refreshHandler);
  const cleanup = () => window.removeEventListener('calendar:refresh', refreshHandler);

  await load();
  return cleanup;
}

import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  formatTime,
  openModal,
  confirmDialog,
  toast,
  emptyState,
  errorState,
  loadingState,
  setButtonLoading,
  clearFieldErrors,
  showFormAlert,
  applyFieldErrors,
  dayOfWeekLabel,
  WEEKDAYS_API,
} from '../ui.js';
import { isAdmin } from '../store.js';

const state = {
  doctorId: '',
};

let doctorsCache = [];

function sortSchedules(schedules) {
  return [...schedules].sort((left, right) => {
    const dayDiff = WEEKDAYS_API.indexOf(left.dayOfWeek) - WEEKDAYS_API.indexOf(right.dayOfWeek);
    if (dayDiff !== 0) return dayDiff;
    return String(left.startTime).localeCompare(String(right.startTime));
  });
}

function shiftCard(schedule, onEdit, onDelete) {
  return `
    <div class="shift-card">
      <strong>${formatTime(schedule.startTime)}–${formatTime(schedule.endTime)}</strong>
      <small>${schedule.slotMinutes} мин · ${schedule.active ? 'активна' : 'отключена'}</small>
      ${isAdmin() ? `
        <div class="shift-card-actions">
          <button type="button" class="icon-action" data-edit="${schedule.id}" title="Изменить смену" aria-label="Изменить смену">${icon('edit')}</button>
          <button type="button" class="icon-action danger" data-delete="${schedule.id}" title="Удалить смену" aria-label="Удалить смену">${icon('trash')}</button>
        </div>
      ` : ''}
    </div>
  `;
}

function weekGrid(schedules, handers) {
  const grouped = new Map();
  WEEKDAYS_API.forEach((day) => grouped.set(day, []));
  sortSchedules(schedules).forEach((schedule) => {
    if (!grouped.has(schedule.dayOfWeek)) grouped.set(schedule.dayOfWeek, []);
    grouped.get(schedule.dayOfWeek).push(schedule);
  });

  return `
    <div class="week-grid">
      ${WEEKDAYS_API.map((day) => {
        const shifts = grouped.get(day) || [];
        return `
          <article class="card week-day-card ${day === currentDayName() ? 'is-today' : ''}">
            <div class="week-day-head">
              <strong>${esc(dayOfWeekLabel(day))}</strong>
              <span>${shifts.length ? `${shifts.length} смен${shifts.length > 1 ? 'ы' : 'а'}` : 'выходной'}</span>
            </div>
            ${shifts.length
              ? shifts.map((schedule) => shiftCard(schedule, handers.onEdit, handers.onDelete)).join('')
              : `<div class="shift-empty">${icon('clock')}<span>Смены не заданы</span></div>`}
            ${isAdmin()
              ? `<button type="button" class="btn btn-ghost btn-sm shift-add" data-add="${day}">${icon('plus')}<span>Добавить</span></button>`
              : ''}
          </article>
        `;
      }).join('')}
    </div>
  `;
}

function currentDayName() {
  const jsDay = new Date().getDay();
  const index = (jsDay + 6) % 7;
  return WEEKDAYS_API[index];
}

export async function render(root, ctx) {
  const doctorMode = ctx.user.role === 'DOCTOR';

  root.innerHTML = `
    <div class="stack">
      <div class="page-toolbar">
        ${doctorMode
          ? `<div class="flex items-center gap-8"><span class="badge badge-soft">${icon('user')} ${esc(ctx.user.fullName)}</span><span class="muted" style="font-size:12px">Ваши недельные смены</span></div>`
          : `<select class="select" id="schedule-doctor" style="min-width:260px"></select>`}
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" id="schedule-refresh" title="Обновить">${icon('refresh')}</button>
        ${isAdmin()
          ? `<button type="button" class="btn btn-primary" id="schedule-new">${icon('plus')}<span>Добавить смену</span></button>`
          : ''}
      </div>
      <section class="card card-pad" id="schedule-summary">
        <div class="section-head" style="margin-bottom:0">
          <div>
            <h3 data-schedule-title>Недельное расписание</h3>
            <p data-schedule-subtitle>Загружаем смены врача…</p>
          </div>
        </div>
      </section>
      <div data-schedule-grid>${loadingState('Загружаем расписание…')}</div>
    </div>
  `;

  const grid = root.querySelector('[data-schedule-grid]');
  const title = root.querySelector('[data-schedule-title]');
  const subtitle = root.querySelector('[data-schedule-subtitle]');
  const doctorSelect = root.querySelector('#schedule-doctor');

  if (doctorMode) {
    state.doctorId = ctx.user.doctorId ? String(ctx.user.doctorId) : '';
  } else {
    try {
      doctorsCache = await api.doctors.list();
    } catch (error) {
      grid.innerHTML = errorState(getErrorMessage(error));
      return;
    }
    if (doctorsCache.length === 0) {
      grid.innerHTML = emptyState({
        iconName: 'doctors',
        title: 'Врачей пока нет',
        text: 'Сначала создайте профиль врача, затем настройте его смены.',
      });
      return;
    }
    if (!state.doctorId || !doctorsCache.some((doctor) => String(doctor.id) === String(state.doctorId))) {
      state.doctorId = String(doctorsCache[0].id);
    }
    doctorSelect.innerHTML = doctorsCache.map((doctor) => `
      <option value="${doctor.id}" ${String(doctor.id) === String(state.doctorId) ? 'selected' : ''}>
        ${esc(doctor.fullName)} — ${esc(doctor.specializationName)}${doctor.active ? '' : ' (неактивен)'}
      </option>
    `).join('');
    doctorSelect.addEventListener('change', () => {
      state.doctorId = doctorSelect.value;
      load();
    });
  }

  let currentSchedules = [];

  const load = async () => {
    if (!state.doctorId) {
      grid.innerHTML = errorState('Для вашей учётной записи не найден профиль врача', { retry: false });
      return;
    }
    grid.innerHTML = loadingState('Загружаем расписание…');
    try {
      currentSchedules = await api.schedules.byDoctor(state.doctorId);
      const doctor = doctorsCache.find((item) => String(item.id) === String(state.doctorId));
      const shifts = currentSchedules.filter((item) => item.active).length;
      title.textContent = doctor ? doctor.fullName : (ctx.user.fullName || 'Расписание врача');
      subtitle.textContent = currentSchedules.length
        ? `${shifts} активных смен · ${currentSchedules.length} всего`
        : 'Смены ещё не настроены';
      grid.innerHTML = weekGrid(currentSchedules, { onEdit: openEditForm, onDelete: removeSchedule });
      bindGridActions();
    } catch (error) {
      grid.innerHTML = errorState(getErrorMessage(error));
      grid.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  };

  const bindGridActions = () => {
    grid.querySelectorAll('[data-add]').forEach((button) => {
      button.addEventListener('click', () => openScheduleForm({
        doctorId: Number(state.doctorId),
        dayOfWeek: button.dataset.add,
        onDone: load,
      }));
    });
    grid.querySelectorAll('[data-edit]').forEach((button) => {
      button.addEventListener('click', () => {
        const schedule = currentSchedules.find((item) => Number(item.id) === Number(button.dataset.edit));
        if (schedule) {
          openScheduleForm({
            doctorId: Number(state.doctorId),
            schedule,
            onDone: load,
          });
        }
      });
    });
    grid.querySelectorAll('[data-delete]').forEach((button) => {
      button.addEventListener('click', () => {
        const schedule = currentSchedules.find((item) => Number(item.id) === Number(button.dataset.delete));
        if (schedule) removeSchedule(schedule, load);
      });
    });
  };

  const openEditForm = (schedule) => openScheduleForm({
    doctorId: Number(state.doctorId),
    schedule,
    onDone: load,
  });

  const removeSchedule = async (schedule, onDone) => {
    const confirmed = await confirmDialog({
      title: 'Удалить смену?',
      message: `${dayOfWeekLabel(schedule.dayOfWeek)}, ${formatTime(schedule.startTime)}–${formatTime(schedule.endTime)}. Записи на приём при этом не удаляются.`,
      confirmText: 'Удалить смену',
      cancelText: 'Отмена',
    });
    if (!confirmed) {
      return;
    }
    try {
      await api.schedules.remove(schedule.id);
      toast('Смена удалена из расписания', 'success');
      onDone?.();
    } catch (error) {
      toast(getErrorMessage(error), 'error', 'Не удалось удалить смену');
    }
  };

  root.querySelector('#schedule-refresh').addEventListener('click', load);
  root.querySelector('#schedule-new')?.addEventListener('click', () => {
    if (!state.doctorId) {
      toast('Сначала выберите врача', 'error');
      return;
    }
    openScheduleForm({
      doctorId: Number(state.doctorId),
      dayOfWeek: currentDayName(),
      onDone: load,
    });
  });

  await load();
}

function openScheduleForm({ doctorId, dayOfWeek = 'MONDAY', schedule = null, onDone = null }) {
  const isEdit = Boolean(schedule);
  const formId = `schedule-form-${Date.now()}`;
  const doctor = doctorsCache.find((item) => Number(item.id) === Number(doctorId));
  const modal = openModal({
    title: isEdit ? 'Изменить смену' : 'Новая смена',
    subtitle: doctor ? `${doctor.fullName} · ${doctor.specializationName}` : 'Настройка недельного расписания',
    size: 'sm',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="field">
          <label for="${formId}-day">День недели *</label>
          <select class="select" id="${formId}-day" name="dayOfWeek" required>
            ${WEEKDAYS_API.map((day) => `
              <option value="${day}" ${(schedule?.dayOfWeek || dayOfWeek) === day ? 'selected' : ''}>${esc(dayOfWeekLabel(day))}</option>
            `).join('')}
          </select>
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="${formId}-start">Начало смены *</label>
            <input class="input" type="time" id="${formId}-start" name="startTime" required value="${esc(formatTime(schedule?.startTime || '09:00'))}">
          </div>
          <div class="field">
            <label for="${formId}-end">Конец смены *</label>
            <input class="input" type="time" id="${formId}-end" name="endTime" required value="${esc(formatTime(schedule?.endTime || '17:00'))}">
          </div>
        </div>
        <div class="field">
          <label for="${formId}-slot">Интервал приёма *</label>
          <select class="select" id="${formId}-slot" name="slotMinutes" required>
            ${[15, 20, 30, 45, 60].map((minutes) => `
              <option value="${minutes}" ${Number(schedule?.slotMinutes || 30) === minutes ? 'selected' : ''}>${minutes} минут</option>
            `).join('')}
          </select>
        </div>
        <div class="check-row">
          <input class="checkbox" type="checkbox" id="${formId}-active" name="active" ${schedule?.active === false ? '' : 'checked'}>
          <label class="check-label" for="${formId}-active">Смена активна</label>
        </div>
        <div class="info-note">
          ${icon('info')}
          <span>Запись возможна только внутри активной смены и с шагом, кратным интервалу приёма.</span>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>Отмена</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? 'Сохранить' : 'Добавить смену'}</span>
      </button>
    `,
  });

  const form = modal.body.querySelector('form');
  const submitButton = modal.footer.querySelector('[data-submit]');
  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => modal.close());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    const values = {
      dayOfWeek: form.elements.namedItem('dayOfWeek').value,
      startTime: form.elements.namedItem('startTime').value,
      endTime: form.elements.namedItem('endTime').value,
      slotMinutes: Number(form.elements.namedItem('slotMinutes').value),
      active: form.elements.namedItem('active').checked,
    };
    if (!values.startTime || !values.endTime || !values.dayOfWeek) {
      showFormAlert(form, 'Заполните день и время смены');
      return;
    }
    if (values.startTime >= values.endTime) {
      showFormAlert(form, 'Время окончания должно быть позже времени начала');
      form.elements.namedItem('endTime').classList.add('is-invalid');
      return;
    }

    setButtonLoading(submitButton, true, isEdit ? 'Сохраняем…' : 'Добавляем…');
    try {
      if (isEdit) {
        await api.schedules.update(schedule.id, values);
      } else {
        await api.schedules.create(doctorId, values);
      }
      modal.close();
      toast(isEdit ? 'Смена обновлена' : 'Смена добавлена в расписание', 'success');
      onDone?.();
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
    } finally {
      setButtonLoading(submitButton, false);
    }
  });
}

import { api, getErrorMessage } from '../api.js';
import { t } from '../i18n.js';
import {
  icon,
  esc,
  avatar,
  statusBadge,
  paymentBadge,
  referralStatusBadge,
  formatDate,
  formatDateLong,
  formatTime,
  formatDateTime,
  formatMoney,
  todayISO,
  addDays,
  openModal,
  confirmDialog,
  toast,
  emptyState,
  errorState,
  loadingState,
  debounce,
  setButtonLoading,
  clearFieldErrors,
  showFormAlert,
  applyFieldErrors,
  formValue,
  segmentedControl,
  pluralRu,
} from '../ui.js';
import { canSchedule, hasRole, store } from '../store.js';
import { openPatientForm } from './patients.js';

const state = {
  query: '',
  status: '',
  doctorId: '',
  from: addDays(todayISO(), -30),
  to: addDays(todayISO(), 62),
  patientId: '',
  patientName: '',
};

let lastParamsKey = '';

function statusTab(status) {
  return {
    value: status,
    label: {
      '': t('Все записи'),
      SCHEDULED: t('Ожидают'),
      COMPLETED: t('Приёмы проведены'),
      CANCELLED: t('Отменённые'),
      NO_SHOW: t('Не явились'),
    }[status],
  };
}

function slotReason(reason) {
  if (reason === t('Занято')) return t('занято');
  if (reason === t('Время уже прошло')) return t('прошло');
  return t('конец смены');
}

function renderSlots(container, availability, { selectedStart = null, onSelect } = {}) {
  if (!availability.workingDay) {
    container.innerHTML = `
      <div class="slots-message">
        ${esc(availability.message || t('В этот день врач не принимает'))}
      </div>
    `;
    return;
  }
  if (!availability.slots || availability.slots.length === 0) {
    container.innerHTML = `<div class="slots-message">${t('На эту дату нет доступных интервалов')}</div>`;
    return;
  }

  const available = availability.slots.filter((slot) => slot.available).length;
  container.innerHTML = `
    <div class="slots-header">
      <span class="slots-title">${t('Свободное время')}</span>
      <span class="slots-hint">${available
        ? t('Доступно {0} {1}', [available, pluralRu(available, ['слот', 'слота', 'слотов'])])
        : t('Свободных слотов нет')}</span>
    </div>
    <div class="slots-grid">
      ${availability.slots.map((slot) => `
        <button type="button"
                class="slot-btn ${selectedStart === slot.startTime ? 'is-selected' : ''}"
                data-slot="${esc(slot.startTime)}"
                ${slot.available ? '' : 'disabled'}
                title="${esc(slot.reason || t('Свободно'))}">
          ${formatTime(slot.startTime)}
          ${slot.available ? `<small>${t('свободно')}</small>` : `<small>${esc(slotReason(slot.reason))}</small>`}
        </button>
      `).join('')}
    </div>
  `;

  container.querySelectorAll('.slot-btn[data-slot]:not([disabled])').forEach((button) => {
    button.addEventListener('click', () => {
      const slot = availability.slots.find((item) => item.startTime === button.dataset.slot);
      container.querySelectorAll('.slot-btn').forEach((item) => item.classList.remove('is-selected'));
      button.classList.add('is-selected');
      onSelect?.(slot, availability);
    });
  });
}

function doctorOptions(doctors, selectedId) {
  return doctors
    .filter((doctor) => doctor.active)
    .map((doctor) => `
      <option value="${doctor.id}" ${String(doctor.id) === String(selectedId) ? 'selected' : ''}>
        ${esc(doctor.fullName)} — ${esc(doctor.specializationName)}
      </option>
    `).join('');
}

function allDoctorOptions(doctors, selectedId) {
  return doctors.map((doctor) => `
    <option value="${doctor.id}" ${String(doctor.id) === String(selectedId) ? 'selected' : ''}>
      ${esc(doctor.fullName)}${doctor.active ? '' : ` ${t('(неактивен)')}`}
    </option>
  `).join('');
}

function patientOptions(patients, selectedId) {
  return patients.map((patient) => `
    <option value="${patient.id}" ${String(patient.id) === String(selectedId) ? 'selected' : ''}>
      ${esc(patient.fullName)} · ${esc(patient.iin)}
    </option>
  `).join('');
}

function serviceOptions(services, selectedId) {
  return services
    .filter((service) => service.active)
    .map((service) => `
      <option value="${service.id}" ${String(service.id) === String(selectedId) ? 'selected' : ''}>
        ${esc(service.name)} · ${service.durationMinutes} ${t('мин')} · ${formatMoney(service.price)}
      </option>
    `).join('');
}

function appointmentRow(appointment, user, callbacks) {
  const isOwnDoctor = user.role === 'DOCTOR' && Number(user.doctorId) === Number(appointment.doctorId);
  const scheduler = canSchedule();
  const canComplete = appointment.status === 'SCHEDULED'
    && new Date(appointment.startTime) <= new Date()
    && (user.role === 'ADMIN' || (user.role === 'DOCTOR' && isOwnDoctor));
  const canCancelNow = appointment.status === 'SCHEDULED'
    && (scheduler || (user.role === 'DOCTOR' && isOwnDoctor));
  const canReschedule = appointment.status === 'SCHEDULED' && scheduler;
  const canNoShow = appointment.status === 'SCHEDULED'
    && new Date(appointment.startTime) <= new Date()
    && (user.role === 'ADMIN' || isOwnDoctor);
  const canReferral = ['SCHEDULED', 'COMPLETED'].includes(appointment.status) && isOwnDoctor;

  return `
    <tr>
      <td>
        <div class="patient-cell">
          ${avatar(appointment.patientName)}
          <div>
            <strong>${esc(appointment.patientName)}</strong>
            <small>${t('ИИН {0}', [esc(appointment.patientIin)])}</small>
            <small>${esc(appointment.patientPhone || '')}</small>
          </div>
        </div>
      </td>
      <td>
        <span class="cell-main">${esc(appointment.doctorName)}</span>
        <span class="cell-sub">${esc(appointment.doctorSpecialization)}${appointment.doctorOffice ? ` · ${t('каб. {0}', [esc(appointment.doctorOffice)])}` : ''}</span>
      </td>
      <td>
        <span class="cell-main">${esc(appointment.serviceName)}</span>
        <span class="cell-sub">${appointment.serviceDurationMinutes} ${t('мин')}</span>
      </td>
      <td class="nowrap">
        <span class="cell-main">${formatDate(appointment.startTime)}</span>
        <span class="cell-sub">${formatTime(appointment.startTime)}–${formatTime(appointment.endTime)}</span>
      </td>
      <td class="num">${formatMoney(appointment.price)}<div class="mt-8">${paymentBadge(appointment.paymentStatus)}</div></td>
      <td>${statusBadge(appointment.status)}</td>
      <td>
        <div class="actions">
          <button type="button" class="icon-action" data-view="${appointment.id}" title="${t('Открыть карточку')}" aria-label="${t('Открыть запись')}">${icon('eye')}</button>
          ${canReschedule ? `<button type="button" class="icon-action" data-reschedule="${appointment.id}" title="${t('Перенести')}" aria-label="${t('Перенести запись')}">${icon('calendar')}</button>` : ''}
          ${canComplete ? `<button type="button" class="icon-action" data-complete="${appointment.id}" title="${t('Завершить приём')}" aria-label="${t('Завершить приём')}">${icon('check-circle')}</button>` : ''}
          ${canNoShow ? `<button type="button" class="icon-action danger" data-no-show="${appointment.id}" title="${t('Не явился')}" aria-label="${t('Отметить неявку')}">${icon('user')}</button>` : ''}
          ${canReferral ? `<button type="button" class="icon-action" data-referral="${appointment.id}" title="${t('Выписать направление')}" aria-label="${t('Выписать направление')}">${icon('referrals')}</button>` : ''}
          ${canCancelNow ? `<button type="button" class="icon-action danger" data-cancel="${appointment.id}" title="${t('Отменить')}" aria-label="${t('Отменить запись')}">${icon('ban')}</button>` : ''}
        </div>
      </td>
    </tr>
  `;
}

function renderTable(appointments, user, callbacks) {
  if (appointments.length === 0) {
    return `
      <div class="table-empty">
        ${emptyState({
          iconName: 'search',
          title: t('Записи не найдены'),
          text: t('Измените период или фильтры поиска. При необходимости создайте новую запись.'),
          action: canSchedule()
            ? `<button type="button" class="btn btn-primary btn-sm" data-new-booking>${icon('plus')}<span>${t('Новая запись')}</span></button>`
            : '',
        })}
      </div>
    `;
  }

  return `
    <table class="table appointments-table">
      <thead>
        <tr>
          <th>${t('Пациент')}</th>
          <th>${t('Врач')}</th>
          <th>${t('Услуга')}</th>
          <th>${t('Дата и время')}</th>
          <th class="num">${t('Стоимость')}</th>
          <th>${t('Статус')}</th>
          <th><span class="sr-only">${t('Действия')}</span></th>
        </tr>
      </thead>
      <tbody>
        ${appointments.map((appointment) => appointmentRow(appointment, user, callbacks)).join('')}
      </tbody>
    </table>
  `;
}

export async function render(root, ctx) {
  const paramKey = ctx.params.toString();
  if (paramKey !== lastParamsKey) {
    lastParamsKey = paramKey;
    if (ctx.params.has('patientId')) {
      state.patientId = ctx.params.get('patientId');
      state.patientName = ctx.params.get('patientName') || '';
    }
    if (ctx.params.has('status')) {
      state.status = ctx.params.get('status') || '';
    }
    if (ctx.params.has('patientId')) {
      state.query = '';
    }
  }

  let doctors = [];
  try {
    doctors = await api.doctors.list();
  } catch {
    doctors = [];
  }

  const canFilterDoctor = hasRole('ADMIN', 'RECEPTIONIST');

  root.innerHTML = `
    <div class="stack">
      <div class="page-toolbar">
        <div class="search-box">
          ${icon('search')}
          <input class="input" type="search" id="appointments-search" placeholder="${t('Поиск по пациенту, врачу или услуге')}" value="${esc(state.query)}">
        </div>
        ${canFilterDoctor ? `
          <select class="select" id="appointments-doctor" style="min-width:200px">
            <option value="">${t('Все врачи')}</option>
            ${allDoctorOptions(doctors, state.doctorId)}
          </select>
        ` : ''}
        <label class="filter-label" for="appointments-from">${t('Период')}</label>
        <input class="input" type="date" id="appointments-from" value="${esc(state.from)}" style="width:auto">
        <span class="muted">—</span>
        <input class="input" type="date" id="appointments-to" value="${esc(state.to)}" style="width:auto">
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" id="appointments-refresh" title="${t('Обновить')}">${icon('refresh')}</button>
        ${canSchedule()
          ? `<button type="button" class="btn btn-primary" id="appointments-new">${icon('plus')}<span>${t('Новая запись')}</span></button>`
          : ''}
      </div>

      <div class="flex items-center gap-12 flex-wrap">
        ${segmentedControl(
          ['', 'SCHEDULED', 'COMPLETED', 'NO_SHOW', 'CANCELLED'].map(statusTab),
          state.status,
        )}
        ${state.patientId ? `
          <span class="badge badge-soft" id="patient-filter-chip" style="padding:7px 11px">
            ${t('Пациент:')} ${esc(state.patientName || `#${state.patientId}`)}
            <button type="button" class="link-button" data-clear-patient style="margin-left:4px">${icon('x')}</button>
          </span>
        ` : ''}
      </div>

      <section class="card">
        <div class="card-header">
          <div>
            <div class="card-title">${t('Журнал приёмов')}</div>
            <div class="card-subtitle" data-result-count>${t('Загружаем записи…')}</div>
          </div>
        </div>
        <div class="table-wrap" data-table>${loadingState(t('Загружаем записи…'))}</div>
      </section>
    </div>
  `;

  const tableContainer = root.querySelector('[data-table]');
  const countContainer = root.querySelector('[data-result-count]');
  const searchInput = root.querySelector('#appointments-search');

  const bindTableActions = () => {
    tableContainer.querySelectorAll('[data-view]').forEach((button) => {
      button.addEventListener('click', () => openDetails(Number(button.dataset.view), { onChanged: loadRows }));
    });
    tableContainer.querySelectorAll('[data-reschedule]').forEach((button) => {
      button.addEventListener('click', () => openReschedule(Number(button.dataset.reschedule), { onDone: loadRows }));
    });
    tableContainer.querySelectorAll('[data-complete]').forEach((button) => {
      button.addEventListener('click', () => openComplete(Number(button.dataset.complete), { onDone: loadRows }));
    });
    tableContainer.querySelectorAll('[data-no-show]').forEach((button) => {
      button.addEventListener('click', () => markNoShow(Number(button.dataset.noShow), loadRows));
    });
    tableContainer.querySelectorAll('[data-referral]').forEach((button) => {
      button.addEventListener('click', () => openReferralForm(Number(button.dataset.referral), { onDone: loadRows }));
    });
    tableContainer.querySelectorAll('[data-cancel]').forEach((button) => {
      button.addEventListener('click', () => openCancel(Number(button.dataset.cancel), { onDone: loadRows }));
    });
    tableContainer.querySelectorAll('[data-new-booking]').forEach((button) => {
      button.addEventListener('click', () => openBooking({
        onDone: loadRows,
        patientId: state.patientId || undefined,
      }));
    });
  };

  async function loadRows() {
    tableContainer.innerHTML = loadingState(t('Загружаем записи…'));
    countContainer.textContent = t('Загружаем записи…');
    try {
      const appointments = await api.appointments.list({
        from: state.from,
        to: state.to,
        doctorId: state.doctorId || undefined,
        patientId: state.patientId || undefined,
        status: state.status || undefined,
        query: state.query || undefined,
      });
      tableContainer.innerHTML = renderTable(appointments, ctx.user, {});
      countContainer.textContent = appointments.length
        ? t('{0} {1} за выбранный период', [appointments.length, pluralRu(appointments.length, ['запись', 'записи', 'записей'])])
        : t('Ничего не найдено');
      bindTableActions();
    } catch (error) {
      tableContainer.innerHTML = errorState(getErrorMessage(error));
      countContainer.textContent = t('Ошибка загрузки');
      tableContainer.querySelector('[data-retry]')?.addEventListener('click', loadRows);
    }
  }

  searchInput.addEventListener('input', debounce(() => {
    state.query = searchInput.value.trim();
    loadRows();
  }, 350));

  root.querySelector('#appointments-doctor')?.addEventListener('change', (event) => {
    state.doctorId = event.target.value;
    loadRows();
  });
  root.querySelector('#appointments-from')?.addEventListener('change', (event) => {
    state.from = event.target.value;
    loadRows();
  });
  root.querySelector('#appointments-to')?.addEventListener('change', (event) => {
    state.to = event.target.value;
    loadRows();
  });
  root.querySelector('#appointments-refresh')?.addEventListener('click', loadRows);
  root.querySelector('#appointments-new')?.addEventListener('click', () => openBooking({
    onDone: loadRows,
    patientId: state.patientId || undefined,
  }));

  root.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => {
      if (state.status === button.dataset.tab) {
        return;
      }
      state.status = button.dataset.tab;
      root.querySelectorAll('[data-tab]').forEach((item) => {
        item.classList.toggle('is-active', item === button);
        item.setAttribute('aria-selected', String(item === button));
      });
      loadRows();
    });
  });

  root.querySelector('[data-clear-patient]')?.addEventListener('click', () => {
    state.patientId = '';
    state.patientName = '';
    root.querySelector('#patient-filter-chip')?.remove();
    loadRows();
  });

  await loadRows();
}

export async function openBooking(options = {}) {
  const {
    onDone = null,
    doctorId: presetDoctor = '',
    patientId: presetPatient = '',
    date: presetDate = todayISO(),
  } = options;

  const formId = `booking-form-${Date.now()}`;
  let closed = false;
  const modal = openModal({
    title: t('Новая запись на приём'),
    subtitle: t('Свободные слоты рассчитываются сервером с учётом смены врача'),
    size: 'lg',
    body: loadingState(t('Загружаем врачей, пациентов и услуги…')),
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit disabled>
        ${icon('check')}<span>${t('Создать запись')}</span>
      </button>
    `,
    onClose: () => {
      closed = true;
    },
  });

  let doctors;
  let patients;
  let services;
  try {
    [doctors, patients, services] = await Promise.all([
      api.doctors.list(),
      api.patients.list(),
      api.services.list(),
    ]);
  } catch (error) {
    modal.body.innerHTML = errorState(getErrorMessage(error), { retry: false });
    return;
  }
  if (closed) {
    return;
  }

  const activeDoctors = doctors.filter((doctor) => doctor.active);
  const activeServices = services.filter((service) => service.active);
  if (activeDoctors.length === 0 || activeServices.length === 0) {
    modal.body.innerHTML = emptyState({
      iconName: 'alert-circle',
      title: t('Нет данных для записи'),
      text: t('Для создания записи нужен хотя бы один активный врач и одна активная услуга.'),
    });
    return;
  }

  const initialDoctor = String(presetDoctor || activeDoctors[0].id);
  const initialService = String(activeServices[0].id);
  const selectedPatient = String(presetPatient || '');

  modal.body.innerHTML = `
    <form id="${formId}" class="modal-enter" novalidate>
      <div class="form-alert hidden" data-form-alert role="alert"></div>
      <div class="form-grid">
        <div class="field">
          <label for="${formId}-doctor">${t('Врач')}</label>
          <select class="select" id="${formId}-doctor" name="doctorId" required>
            ${doctorOptions(doctors, initialDoctor)}
          </select>
        </div>
        <div class="field">
          <label for="${formId}-service">${t('Услуга')}</label>
          <select class="select" id="${formId}-service" name="serviceId" required>
            ${serviceOptions(services, initialService)}
          </select>
        </div>
        <div class="field">
          <label for="${formId}-patient">${t('Пациент')}</label>
          <div class="flex gap-8">
            <select class="select" id="${formId}-patient" name="patientId" required style="flex:1">
              <option value="">${t('Выберите пациента')}</option>
              ${patientOptions(patients, selectedPatient)}
            </select>
            <button type="button" class="btn btn-secondary btn-icon" data-add-patient title="${t('Добавить пациента')}" aria-label="${t('Добавить пациента')}">${icon('user-plus')}</button>
          </div>
        </div>
        <div class="field">
          <label for="${formId}-date">${t('Дата приёма')}</label>
          <input class="input" type="date" id="${formId}-date" name="date" min="${todayISO()}" value="${esc(presetDate)}" required>
        </div>
      </div>

      <div class="field">
        <div data-slots>
          <div class="slots-loading"><span class="spinner"></span><span>${t('Подбираем свободное время…')}</span></div>
        </div>
      </div>

      <div class="booking-summary" data-summary>
        <span>${icon('info')} ${t('Выберите свободный слот, чтобы завершить запись')}</span>
      </div>

      <div class="field">
        <label for="${formId}-notes">${t('Комментарий')}</label>
        <textarea class="textarea" id="${formId}-notes" name="notes" maxlength="500" placeholder="${t('Например: первичный приём, жалобы на самочувствие')}"></textarea>
      </div>

      <input type="hidden" name="startTime" value="">
    </form>
  `;

  const form = modal.body.querySelector('form');
  const doctorSelect = form.elements.namedItem('doctorId');
  const serviceSelect = form.elements.namedItem('serviceId');
  const dateInput = form.elements.namedItem('date');
  const patientSelect = form.elements.namedItem('patientId');
  const startInput = form.elements.namedItem('startTime');
  const slotsContainer = form.querySelector('[data-slots]');
  const summary = form.querySelector('[data-summary]');
  const submitButton = modal.footer.querySelector('[data-submit]');
  submitButton.disabled = false;

  let selectedSlot = null;
  let requestSequence = 0;
  let currentAvailability = null;

  const currentService = () => services.find((service) => String(service.id) === String(serviceSelect.value));
  const currentDoctor = () => doctors.find((doctor) => String(doctor.id) === String(doctorSelect.value));

  const updateSummary = () => {
    const doctor = currentDoctor();
    const service = currentService();
    if (!doctor || !service) {
      summary.innerHTML = `<span>${icon('info')} ${t('Выберите врача и услугу')}</span>`;
      return;
    }
    summary.innerHTML = `
      <span>${icon('briefcase')} ${esc(service.name)}</span>
      <span>${icon('clock')} ${service.durationMinutes} ${t('мин')}</span>
      <span>${icon('wallet')} ${formatMoney(service.price)}</span>
      ${selectedSlot
        ? `<span>${icon('calendar')} ${formatDateLong(dateInput.value)} · ${formatTime(selectedSlot.startTime)}–${formatTime(selectedSlot.endTime)}</span>`
        : `<span>${icon('calendar')} ${esc(doctor.fullName)} · ${t('выберите время')}</span>`}
    `;
  };

  const loadSlots = async () => {
    selectedSlot = null;
    startInput.value = '';
    updateSummary();
    if (!doctorSelect.value || !dateInput.value) {
      slotsContainer.innerHTML = `<div class="slots-message">${t('Выберите врача и дату приёма')}</div>`;
      return;
    }
    const sequence = ++requestSequence;
    slotsContainer.innerHTML = `<div class="slots-loading"><span class="spinner"></span><span>${t('Проверяем доступность врача…')}</span></div>`;
    try {
      const availability = await api.doctors.availability(doctorSelect.value, {
        date: dateInput.value,
        serviceId: serviceSelect.value || undefined,
      });
      if (sequence !== requestSequence || closed) {
        return;
      }
      currentAvailability = availability;
      updateSummary();
      renderSlots(slotsContainer, availability, {
        selectedStart: selectedSlot?.startTime || null,
        onSelect: (slot) => {
          selectedSlot = slot;
          startInput.value = slot.startTime;
          updateSummary();
        },
      });
    } catch (error) {
      if (sequence !== requestSequence || closed) {
        return;
      }
      slotsContainer.innerHTML = `<div class="slots-message">${esc(getErrorMessage(error))}</div>`;
    }
  };

  doctorSelect.addEventListener('change', loadSlots);
  serviceSelect.addEventListener('change', loadSlots);
  dateInput.addEventListener('change', () => {
    if (!dateInput.value) {
      slotsContainer.innerHTML = `<div class="slots-message">${t('Выберите дату приёма')}</div>`;
      return;
    }
    loadSlots();
  });

  form.querySelector('[data-add-patient]').addEventListener('click', () => {
    openPatientForm({
      onDone: (patient) => {
        if (!patient) return;
        const option = document.createElement('option');
        option.value = String(patient.id);
        option.textContent = `${patient.fullName} · ${patient.iin}`;
        patientSelect.appendChild(option);
        patientSelect.value = String(patient.id);
      },
    });
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => modal.close());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    if (!patientSelect.value) {
      showFormAlert(form, t('Выберите пациента для записи'));
      patientSelect.classList.add('is-invalid');
      patientSelect.focus();
      return;
    }
    if (!startInput.value) {
      showFormAlert(form, t('Выберите свободное время из доступных слотов'));
      slotsContainer.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    setButtonLoading(submitButton, true, t('Создаём запись…'));
    try {
      const created = await api.appointments.create({
        doctorId: Number(doctorSelect.value),
        patientId: Number(patientSelect.value),
        serviceId: Number(serviceSelect.value),
        startTime: startInput.value,
        notes: form.elements.namedItem('notes').value.trim() || null,
      });
      modal.close();
      toast(`${created.patientName}: ${formatDateLong(created.startTime)}, ${formatTime(created.startTime)}`, 'success', t('Запись создана'));
      onDone?.(created);
      document.dispatchEvent(new CustomEvent('appointments:changed', { detail: created }));
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
      if (error.status === 409) {
        await loadSlots();
      }
    } finally {
      setButtonLoading(submitButton, false);
    }
  });

  updateSummary();
  await loadSlots();
}

export async function openReschedule(id, options = {}) {
  const { onDone = null } = options;

  let appointment;
  try {
    appointment = await api.appointments.get(id);
  } catch (error) {
    toast(getErrorMessage(error), 'error');
    return;
  }
  if (appointment.status !== 'SCHEDULED') {
    toast(t('Перенести можно только запланированный приём'), 'error');
    return;
  }

  let services = [];
  try {
    services = await api.services.list();
  } catch {
    services = [];
  }
  const service = services.find((item) => Number(item.id) === Number(appointment.serviceId));

  const formId = `reschedule-form-${Date.now()}`;
  let closed = false;
  const modal = openModal({
    title: t('Перенести приём'),
    subtitle: `${appointment.patientName} · ${appointment.doctorName}`,
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">${t('Врач')}</span>
            <span class="detail-value">${esc(appointment.doctorName)}<br><span class="muted">${esc(appointment.doctorSpecialization)}${appointment.doctorOffice ? ` · ${t('каб. {0}', [esc(appointment.doctorOffice)])}` : ''}</span></span>
          </div>
          <div class="detail-item">
            <span class="detail-label">${t('Услуга')}</span>
            <span class="detail-value">${esc(appointment.serviceName)}<br><span class="muted">${appointment.serviceDurationMinutes} ${t('мин')} · ${formatMoney(appointment.price)}</span></span>
          </div>
          <div class="detail-item">
            <span class="detail-label">${t('Текущее время')}</span>
            <span class="detail-value">${formatDateLong(appointment.startTime)}<br><span class="muted">${formatTime(appointment.startTime)}–${formatTime(appointment.endTime)}</span></span>
          </div>
          <div class="field">
            <label for="${formId}-date">${t('Новая дата')}</label>
            <input class="input" type="date" id="${formId}-date" name="date" min="${todayISO()}" value="${appointment.startTime.slice(0, 10)}" required>
          </div>
        </div>

        <div data-slots>
          <div class="slots-loading"><span class="spinner"></span><span>${t('Проверяем доступность врача…')}</span></div>
        </div>

        <div class="field">
          <label for="${formId}-notes">${t('Комментарий')}</label>
          <textarea class="textarea" id="${formId}-notes" name="notes" maxlength="500">${esc(appointment.notes || '')}</textarea>
        </div>

        <input type="hidden" name="startTime" value="">
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${t('Перенести')}</span>
      </button>
    `,
    onClose: () => {
      closed = true;
    },
  });

  const form = modal.body.querySelector('form');
  const dateInput = form.elements.namedItem('date');
  const startInput = form.elements.namedItem('startTime');
  const slotsContainer = form.querySelector('[data-slots]');
  const submitButton = modal.footer.querySelector('[data-submit]');
  let selectedSlot = null;
  let sequence = 0;

  const loadSlots = async () => {
    selectedSlot = null;
    startInput.value = '';
    if (!dateInput.value) {
      slotsContainer.innerHTML = `<div class="slots-message">${t('Выберите новую дату')}</div>`;
      return;
    }
    const current = ++sequence;
    slotsContainer.innerHTML = `<div class="slots-loading"><span class="spinner"></span><span>${t('Проверяем доступность врача…')}</span></div>`;
    try {
      const availability = await api.doctors.availability(appointment.doctorId, {
        date: dateInput.value,
        serviceId: appointment.serviceId,
        excludeAppointmentId: appointment.id,
      });
      if (current !== sequence || closed) {
        return;
      }
      renderSlots(slotsContainer, availability, {
        onSelect: (slot) => {
          selectedSlot = slot;
          startInput.value = slot.startTime;
        },
      });
    } catch (error) {
      if (current !== sequence || closed) {
        return;
      }
      slotsContainer.innerHTML = `<div class="slots-message">${esc(getErrorMessage(error))}</div>`;
    }
  };

  dateInput.addEventListener('change', loadSlots);
  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => modal.close());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    if (!startInput.value) {
      showFormAlert(form, t('Выберите новое время из доступных слотов'));
      return;
    }
    setButtonLoading(submitButton, true, t('Переносим запись…'));
    try {
      const updated = await api.appointments.reschedule(id, {
        startTime: startInput.value,
        notes: form.elements.namedItem('notes').value.trim() || null,
      });
      modal.close();
      toast(`${formatDateLong(updated.startTime)}, ${formatTime(updated.startTime)}`, 'success', t('Приём перенесён'));
      onDone?.(updated);
      document.dispatchEvent(new CustomEvent('appointments:changed', { detail: updated }));
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
      if (error.status === 409) {
        await loadSlots();
      }
    } finally {
      setButtonLoading(submitButton, false);
    }
  });

  await loadSlots();
}

export async function openCancel(id, options = {}) {
  const { onDone = null, appointment: preset = null } = options;
  let appointment = preset;
  if (!appointment) {
    try {
      appointment = await api.appointments.get(id);
    } catch (error) {
      toast(getErrorMessage(error), 'error');
      return;
    }
  }

  const formId = `cancel-form-${Date.now()}`;
  const modal = openModal({
    title: t('Отменить запись'),
    subtitle: `${appointment.patientName} · ${formatDateLong(appointment.startTime)}, ${formatTime(appointment.startTime)}`,
    size: 'sm',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="info-note">
          ${icon('alert-circle')}
          <span>${t('Слот освободится и станет доступен для новой записи. Действие изменит статус приёма на «Отменён».')}</span>
        </div>
        <div class="field">
          <label for="${formId}-reason">${t('Причина отмены')} <span class="muted">${t('(необязательно)')}</span></label>
          <textarea class="textarea" id="${formId}-reason" name="reason" maxlength="500" placeholder="${t('Например: пациент перенёс визит')}"></textarea>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-close>${t('Назад')}</button>
      <button type="submit" form="${formId}" class="btn btn-danger" data-submit>
        ${icon('ban')}<span>${t('Отменить запись')}</span>
      </button>
    `,
  });

  const form = modal.body.querySelector('form');
  const submitButton = modal.footer.querySelector('[data-submit]');
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    setButtonLoading(submitButton, true, t('Отменяем…'));
    try {
      const updated = await api.appointments.cancel(id, form.elements.namedItem('reason').value.trim());
      modal.close();
      toast(`${appointment.patientName}: ${formatDateLong(appointment.startTime)}`, 'success', t('Запись отменена'));
      onDone?.(updated);
      document.dispatchEvent(new CustomEvent('appointments:changed', { detail: updated }));
    } catch (error) {
      showFormAlert(form, getErrorMessage(error));
    } finally {
      setButtonLoading(submitButton, false);
    }
  });
}

export async function markNoShow(id, onDone) {
  const confirmed = await confirmDialog({
    title: t('Отметить неявку?'),
    message: t('Приём получит статус «Не явился». Вернуть его в работу можно будет только через администратора.'),
    confirmText: t('Не явился'),
    cancelText: t('Назад'),
    danger: true,
    iconName: 'user',
  });
  if (!confirmed) {
    return false;
  }
  try {
    const updated = await api.appointments.noShow(id);
    toast(t('Приём № {0}: {1}', [id, updated.statusLabel]), 'success', t('Неявка отмечена'));
    onDone?.(updated);
    document.dispatchEvent(new CustomEvent('appointments:changed', { detail: updated }));
    return true;
  } catch (error) {
    toast(getErrorMessage(error), 'error');
    return false;
  }
}

export async function openReferralForm(appointmentId, options = {}) {
  const { onDone = null } = options;
  let doctors = [];
  let specializations = [];
  let services = [];
  let me = null;
  try {
    let appointment;
    [doctors, specializations, services, appointment] = await Promise.all([
      api.doctors.list(), api.specializations.list(), api.services.list(), api.appointments.get(appointmentId),
    ]);
    me = doctors.find((doctor) => doctor.id === appointment.doctorId);
    doctors = doctors.filter((doctor) => doctor.active && doctor.id !== appointment.doctorId);
    specializations = specializations.filter((item) => item.active);
    services = services.filter((item) => item.active);
  } catch (error) {
    toast(getErrorMessage(error), 'error', t('Не удалось загрузить справочники'));
    return;
  }
  const diagnostics = services.filter((item) => item.diagnostic);
  const consultations = services.filter((item) => !item.diagnostic);

  const formId = `referral-form-${Date.now()}`;
  const option = (value, label) => `<option value="${value}">${esc(label)}</option>`;
  const modal = openModal({
    title: t('Электронное направление'),
    subtitle: t('Приём № {0}', [appointmentId]),
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="detail-total"><span><span class="detail-label">${t('От кого')}</span>
          <b style="font-weight:600">${esc(me?.fullName || '')}</b><br>${esc(me?.specializationName || '')} · ${t('каб. {0}', [esc(me?.office || '—')])}</span></div>
        <div class="field">
          <span class="field-label">${t('Куда направить')}</span>
          <div class="flex items-center gap-16 flex-wrap">
            <label class="radio-label"><input type="radio" name="targetType" value="doctor" checked> <span>${t('К врачу')}</span></label>
            <label class="radio-label"><input type="radio" name="targetType" value="department"> <span>${t('В отделение')}</span></label>
            <label class="radio-label"><input type="radio" name="targetType" value="diagnostic"> <span>${t('На диагностику или анализы')}</span></label>
          </div>
        </div>
        <div class="form-grid">
          <div class="field" data-for="doctor"><label for="${formId}-doctor">${t('Врач *')}</label>
            <select class="select" id="${formId}-doctor" name="toDoctorId">${doctors.map((item) => option(item.id, `${item.fullName} · ${item.specializationName} · ${t('каб. {0}', [item.office])}`)).join('')}</select></div>
          <div class="field hidden" data-for="department"><label for="${formId}-specialization">${t('Отделение *')}</label>
            <select class="select" id="${formId}-specialization" name="toSpecializationId">${specializations.map((item) => option(item.id, item.name)).join('')}</select></div>
          <div class="field hidden span-2" data-for="diagnostic"><label for="${formId}-diagnostic">${t('Исследование *')}</label>
            <select class="select" id="${formId}-diagnostic" name="diagnosticId">${diagnostics.map((item) => option(item.id, `${item.name} · ${item.specializationName || ''} · ${t('каб. {0}', [item.office || '—'])} · ${formatMoney(item.price)}`)).join('')}</select></div>
          <div class="field" data-for="doctor department"><label for="${formId}-service">${t('Услуга')}</label>
            <select class="select" id="${formId}-service" name="serviceId">${consultations.map((item) => option(item.id, `${item.name} · ${formatMoney(item.price)}`)).join('')}</select></div>
          <div class="field span-2"><label for="${formId}-reason">${t('Причина и цель направления *')}</label>
            <textarea class="textarea" id="${formId}-reason" name="reason" maxlength="500"></textarea></div>
        </div>
      </form>`,
    footer: `
      <button type="button" class="btn btn-secondary" data-close>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-save><span>${t('Выписать')}</span></button>`,
  });

  const form = modal.body.querySelector('form');
  const save = modal.footer.querySelector('[data-save]');
  const targetType = () => form.querySelector('[name="targetType"]:checked').value;
  const syncTarget = () => form.querySelectorAll('[data-for]').forEach((element) => {
    element.classList.toggle('hidden', !element.dataset.for.split(' ').includes(targetType()));
  });
  form.querySelectorAll('[name="targetType"]').forEach((radio) => radio.addEventListener('change', syncTarget));
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    const type = targetType();
    const payload = { appointmentId, reason: formValue(form, 'reason') || null };
    if (type === 'diagnostic') {
      payload.serviceId = Number(formValue(form, 'diagnosticId')) || null;
    } else {
      payload.serviceId = Number(formValue(form, 'serviceId')) || null;
      if (type === 'doctor') payload.toDoctorId = Number(formValue(form, 'toDoctorId')) || null;
      else payload.toSpecializationId = Number(formValue(form, 'toSpecializationId')) || null;
    }
    setButtonLoading(save, true, t('Сохраняем…'));
    try {
      const created = await api.referrals.create(payload);
      modal.close();
      toast(t('Направление № {0} передано получателю и видно пациенту', [created.id]), 'success', t('Направление выписано'));
      onDone?.(created);
      document.dispatchEvent(new CustomEvent('referrals:changed', { detail: created }));
    } catch (error) {
      showFormAlert(form, getErrorMessage(error));
      applyFieldErrors(form, error?.fieldErrors);
    } finally {
      setButtonLoading(save, false);
    }
  });
}

/** Closes the visit: the status "attended" is stored together with the doctor's conclusion. */
export async function openComplete(id, options = {}) {
  const { onDone = null } = options;
  let appointment;
  try {
    appointment = await api.appointments.get(id);
  } catch (error) {
    toast(getErrorMessage(error), 'error');
    return;
  }
  const formId = `complete-form-${Date.now()}`;
  const modal = openModal({
    title: t('Приём прошел'),
    subtitle: `${appointment.patientName} · ${t('ИИН {0}', [appointment.patientIin])}`,
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="field"><label for="${formId}-diagnosis">${t('Диагноз *')}</label>
          <input class="input" id="${formId}-diagnosis" name="diagnosis" maxlength="500"></div>
        <div class="field"><label for="${formId}-conclusion">${t('Заключение врача *')}</label>
          <textarea class="textarea" id="${formId}-conclusion" name="conclusion" rows="4" maxlength="2000"></textarea></div>
        <div class="field"><label for="${formId}-recommendations">${t('Рекомендации')}</label>
          <textarea class="textarea" id="${formId}-recommendations" name="recommendations" maxlength="2000"></textarea></div>
        <div><button type="button" class="btn btn-secondary btn-sm" data-referral>${icon('referrals')}<span>${t('Выписать направление')}</span></button></div>
        <div class="callout">${icon('info')}<span>${t('После сохранения визит закроется, попадёт в историю пациента, и пациент сможет скачать справку.')}</span></div>
      </form>`,
    footer: `
      <button type="button" class="btn btn-danger-soft" data-no-show>${t('Не явился')}</button>
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-close>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-save><span>${t('Сохранить: прошел')}</span></button>`,
  });
  const form = modal.body.querySelector('form');
  const save = modal.footer.querySelector('[data-save]');
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());
  modal.footer.querySelector('[data-no-show]').addEventListener('click', async () => {
    if (await markNoShow(id, onDone)) modal.close();
  });
  form.querySelector('[data-referral]').addEventListener('click', () => openReferralForm(id));
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    setButtonLoading(save, true, t('Сохраняем…'));
    try {
      const updated = await api.appointments.complete(id, {
        diagnosis: formValue(form, 'diagnosis') || null,
        conclusion: formValue(form, 'conclusion') || null,
        recommendations: formValue(form, 'recommendations') || null,
      });
      modal.close();
      toast(`${updated.patientName}: ${formatDateLong(updated.startTime)}`, 'success', t('Приём завершён'));
      onDone?.(updated);
      document.dispatchEvent(new CustomEvent('appointments:changed', { detail: updated }));
    } catch (error) {
      showFormAlert(form, getErrorMessage(error));
      applyFieldErrors(form, error?.fieldErrors);
    } finally {
      setButtonLoading(save, false);
    }
  });
}

export async function openDetails(id, options = {}) {
  const { onChanged = null } = options;
  const modal = openModal({
    title: t('Карточка приёма'),
    subtitle: t('Подробности визита и доступные действия'),
    size: 'lg',
    body: loadingState(t('Загружаем карточку приёма…')),
    footer: `<span class="spacer"></span><button type="button" class="btn btn-secondary" data-close>${t('Закрыть')}</button>`,
  });
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());

  let appointment;
  let referrals = [];
  try {
    [appointment, referrals] = await Promise.all([api.appointments.get(id), api.appointments.referrals(id)]);
  } catch (error) {
    modal.body.innerHTML = errorState(getErrorMessage(error), { retry: false });
    return;
  }
  if (!modal.body.isConnected) {
    return;
  }

  const user = store.user;
  const isOwnDoctor = user?.role === 'DOCTOR' && Number(user.doctorId) === Number(appointment.doctorId);
  const scheduler = canSchedule();
  const canReschedule = appointment.status === 'SCHEDULED' && scheduler;
  const canCompleteNow = appointment.status === 'SCHEDULED'
    && new Date(appointment.startTime) <= new Date()
    && (user?.role === 'ADMIN' || isOwnDoctor);
  const canCancelNow = appointment.status === 'SCHEDULED'
    && (scheduler || isOwnDoctor);
  const canNoShowNow = appointment.status === 'SCHEDULED'
    && new Date(appointment.startTime) <= new Date()
    && (user?.role === 'ADMIN' || isOwnDoctor);
  const canReferralNow = ['SCHEDULED', 'COMPLETED'].includes(appointment.status) && isOwnDoctor;

  modal.body.innerHTML = `
    <div class="detail-status-row">
      <div class="flex items-center gap-8">
        ${statusBadge(appointment.status)}
        ${appointment.completedAt ? `<span class="muted" style="font-size:11px">${t('завершён')} ${formatDateTime(appointment.completedAt)}</span>` : ''}
        ${appointment.cancelledAt ? `<span class="muted" style="font-size:11px">${t('отменён')} ${formatDateTime(appointment.cancelledAt)}</span>` : ''}
      </div>
      <span class="appointment-id">${t('Запись №')} ${appointment.id}</span>
    </div>

    <div class="detail-grid">
      <div class="detail-item">
        <span class="detail-label">${t('Пациент')}</span>
        <span class="detail-value"><strong>${esc(appointment.patientName)}</strong><br>${t('ИИН {0}', [esc(appointment.patientIin)])}<br>${esc(appointment.patientPhone || '')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">${t('Лечащий врач')}</span>
        <span class="detail-value"><strong>${esc(appointment.doctorName)}</strong><br>${esc(appointment.doctorSpecialization)}${appointment.doctorOffice ? ` · ${t('каб. {0}', [esc(appointment.doctorOffice)])}` : ''}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">${t('Услуга')}</span>
        <span class="detail-value"><strong>${esc(appointment.serviceName)}</strong><br>${appointment.serviceDurationMinutes} ${t('мин')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">${t('Дата и время')}</span>
        <span class="detail-value"><strong>${formatDateLong(appointment.startTime)}</strong><br>${formatTime(appointment.startTime)}–${formatTime(appointment.endTime)}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">${t('Создано')}</span>
        <span class="detail-value">${appointment.createdAt ? formatDateTime(appointment.createdAt) : '—'}</span>
      </div>
    </div>

    ${appointment.notes ? `<div class="mt-16"><span class="detail-label">${t('Комментарий')}</span><div class="detail-note">${esc(appointment.notes)}</div></div>` : ''}
    ${appointment.cancellationReason ? `<div class="mt-16"><span class="detail-label">${t('Причина отмены')}</span><div class="detail-note">${esc(appointment.cancellationReason)}</div></div>` : ''}
    ${appointment.status === 'COMPLETED' ? `
      <div class="mt-16"><span class="detail-label">${t('Диагноз')}</span><div class="detail-note">${esc(appointment.diagnosis || '—')}</div></div>
      <div class="mt-16"><span class="detail-label">${t('Заключение врача')}</span><div class="detail-note">${esc(appointment.conclusion || '—')}</div></div>
      ${appointment.recommendations ? `<div class="mt-16"><span class="detail-label">${t('Рекомендации')}</span><div class="detail-note">${esc(appointment.recommendations)}</div></div>` : ''}` : ''}
    ${referrals.length ? `<div class="mt-16"><span class="detail-label">${t('Выданные направления')}</span>
      <ul class="visit-referrals">${referrals.map((referral) => `<li>${esc([referral.serviceName, referral.toDoctorName || referral.toSpecializationName].filter(Boolean).join(', '))}: ${esc(referral.reason || '—')} ${referralStatusBadge(referral.status)}</li>`).join('')}</ul></div>` : ''}
    <div class="detail-total mt-16">
      <span class="flex items-center gap-12">${t('Стоимость приёма')} ${paymentBadge(appointment.paymentStatus)}</span>
      <strong>${formatMoney(appointment.price)}</strong>
    </div>
  `;

  modal.footer.innerHTML = `
    <span class="spacer"></span>
    ${canReschedule ? `<button type="button" class="btn btn-secondary" data-reschedule>${icon('calendar')}<span>${t('Перенести')}</span></button>` : ''}
    ${canCancelNow ? `<button type="button" class="btn btn-danger-soft" data-cancel-appointment>${icon('ban')}<span>${t('Отменить')}</span></button>` : ''}
    ${canNoShowNow ? `<button type="button" class="btn btn-danger-soft" data-no-show-appointment>${icon('user')}<span>${t('Не явился')}</span></button>` : ''}
    ${canReferralNow ? `<button type="button" class="btn btn-secondary" data-referral-appointment>${icon('referrals')}<span>${t('Направление')}</span></button>` : ''}
    ${appointment.status === 'COMPLETED' ? `<button type="button" class="btn btn-secondary" data-certificate>${icon('download')}<span>${t('Справка PDF')}</span></button>` : ''}
    ${canCompleteNow ? `<button type="button" class="btn btn-primary" data-complete>${icon('check-circle')}<span>${t('Отметить: прошел')}</span></button>` : ''}
    <button type="button" class="btn btn-secondary" data-close>${t('Закрыть')}</button>
  `;
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());

  modal.footer.querySelector('[data-certificate]')?.addEventListener('click', async () => {
    try {
      await api.appointments.certificate(appointment.id);
    } catch (error) {
      toast(getErrorMessage(error), 'error');
    }
  });
  modal.footer.querySelector('[data-reschedule]')?.addEventListener('click', () => {
    modal.close();
    openReschedule(appointment.id, { onDone: onChanged });
  });
  modal.footer.querySelector('[data-cancel-appointment]')?.addEventListener('click', () => {
    modal.close();
    openCancel(appointment.id, { onDone: onChanged, appointment });
  });
  modal.footer.querySelector('[data-complete]')?.addEventListener('click', async () => {
    modal.close();
    await openComplete(appointment.id, { onDone: onChanged });
  });
  modal.footer.querySelector('[data-no-show-appointment]')?.addEventListener('click', async () => {
    modal.close();
    await markNoShow(appointment.id, onChanged);
  });
  modal.footer.querySelector('[data-referral-appointment]')?.addEventListener('click', () => {
    modal.close();
    openReferralForm(appointment.id, { onDone: onChanged });
  });
}

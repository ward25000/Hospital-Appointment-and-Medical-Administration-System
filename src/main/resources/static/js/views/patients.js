import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  avatar,
  formatDate,
  todayISO,
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
  pluralRu,
  genderLabel,
} from '../ui.js';
import { navigate } from '../store.js';

const state = {
  query: '',
};

function patientRow(patient, callbacks) {
  return `
    <tr>
      <td>
        <div class="name-cell">
          ${avatar(patient.fullName)}
          <div class="name-cell-copy">
            <strong>${esc(patient.fullName)}</strong>
            <small>ID ${patient.id} · ${esc(patient.genderLabel || genderLabel(patient.gender))}</small>
          </div>
        </div>
      </td>
      <td>
        <span class="cell-main">${formatDate(patient.birthDate)}</span>
        <span class="cell-sub">${patient.age} ${pluralRu(patient.age, ['год', 'года', 'лет'])}</span>
      </td>
      <td>${esc(patient.phone || '—')}</td>
      <td>${patient.email ? `<a class="link-button" href="mailto:${esc(patient.email)}">${esc(patient.email)}</a>` : '<span class="muted">—</span>'}</td>
      <td class="muted-cell">${esc(patient.address || '—')}</td>
      <td>
        <div class="actions">
          <button type="button" class="icon-action" data-appointments="${patient.id}" title="Записи пациента" aria-label="Записи пациента">${icon('calendar')}</button>
          <button type="button" class="icon-action" data-edit="${patient.id}" title="Редактировать" aria-label="Редактировать пациента">${icon('edit')}</button>
          <button type="button" class="icon-action danger" data-delete="${patient.id}" title="Удалить" aria-label="Удалить пациента">${icon('trash')}</button>
        </div>
      </td>
    </tr>
  `;
}

function renderTable(patients) {
  if (patients.length === 0) {
    return `
      <div class="table-empty">
        ${emptyState({
          iconName: 'patients',
          title: 'Пациенты не найдены',
          text: state.query
            ? 'Попробуйте изменить поисковый запрос или сбросить фильтр.'
            : 'В картотеке пока нет пациентов. Добавьте первую карточку.',
          action: `<button type="button" class="btn btn-primary btn-sm" data-new>${icon('user-plus')}<span>Добавить пациента</span></button>`,
        })}
      </div>
    `;
  }
  return `
    <table class="table">
      <thead>
        <tr>
          <th>Пациент</th>
          <th>Дата рождения</th>
          <th>Телефон</th>
          <th>Email</th>
          <th>Адрес</th>
          <th><span class="sr-only">Действия</span></th>
        </tr>
      </thead>
      <tbody>
        ${patients.map((patient) => patientRow(patient)).join('')}
      </tbody>
    </table>
  `;
}

export async function render(root, ctx) {
  root.innerHTML = `
    <div class="stack">
      <div class="page-toolbar">
        <div class="search-box">
          ${icon('search')}
          <input class="input" type="search" id="patient-search" placeholder="Поиск по имени, телефону или email" value="${esc(state.query)}">
        </div>
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" id="patient-refresh" title="Обновить">${icon('refresh')}</button>
        <button type="button" class="btn btn-primary" id="patient-new">${icon('user-plus')}<span>Добавить пациента</span></button>
      </div>

      <section class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Картотека пациентов</div>
            <div class="card-subtitle" data-count>Загружаем картотеку…</div>
          </div>
          <span class="metric-inline">${icon('shield')} <span>Персональные данные защищены</span></span>
        </div>
        <div class="table-wrap" data-table>${loadingState('Загружаем пациентов…')}</div>
      </section>
    </div>
  `;

  const tableContainer = root.querySelector('[data-table]');
  const countContainer = root.querySelector('[data-count]');
  const searchInput = root.querySelector('#patient-search');

  const load = async () => {
    tableContainer.innerHTML = loadingState('Загружаем пациентов…');
    try {
      const patients = await api.patients.list(state.query);
      tableContainer.innerHTML = renderTable(patients);
      countContainer.textContent = patients.length
        ? `${patients.length} ${pluralRu(patients.length, ['пациент', 'пациента', 'пациентов'])} в картотеке`
        : 'Ничего не найдено';
      bindActions(patients);
    } catch (error) {
      tableContainer.innerHTML = errorState(getErrorMessage(error));
      countContainer.textContent = 'Ошибка загрузки';
      tableContainer.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  };

  const bindActions = (patients) => {
    tableContainer.querySelectorAll('[data-appointments]').forEach((button) => {
      button.addEventListener('click', () => {
        const patient = patients.find((item) => Number(item.id) === Number(button.dataset.appointments));
        navigate(`/appointments?patientId=${button.dataset.appointments}${patient ? `&patientName=${encodeURIComponent(patient.fullName)}` : ''}`);
      });
    });
    tableContainer.querySelectorAll('[data-edit]').forEach((button) => {
      button.addEventListener('click', () => {
        const patient = patients.find((item) => Number(item.id) === Number(button.dataset.edit));
        openPatientForm({ patient, onDone: load });
      });
    });
    tableContainer.querySelectorAll('[data-delete]').forEach((button) => {
      button.addEventListener('click', () => {
        const patient = patients.find((item) => Number(item.id) === Number(button.dataset.delete));
        if (patient) {
          removePatient(patient, load);
        }
      });
    });
    tableContainer.querySelectorAll('[data-new]').forEach((button) => {
      button.addEventListener('click', () => openPatientForm({ onDone: load }));
    });
  };

  searchInput.addEventListener('input', debounce(() => {
    state.query = searchInput.value.trim();
    load();
  }, 350));

  root.querySelector('#patient-refresh').addEventListener('click', load);
  root.querySelector('#patient-new').addEventListener('click', () => openPatientForm({ onDone: load }));

  await load();

  if (ctx.params.get('new') === '1') {
    history.replaceState(null, '', '#/patients');
    openPatientForm({ onDone: load });
  }
}

async function removePatient(patient, onDone) {
  const confirmed = await confirmDialog({
    title: 'Удалить пациента?',
    message: `Карточка «${patient.fullName}» будет удалена без возможности восстановления. Удаление возможно только при отсутствии записей на приём.`,
    confirmText: 'Удалить',
    cancelText: 'Отмена',
  });
  if (!confirmed) {
    return;
  }
  try {
    await api.patients.remove(patient.id);
    toast(`Карточка «${patient.fullName}» удалена`, 'success');
    onDone?.();
  } catch (error) {
    toast(getErrorMessage(error), 'error', 'Не удалось удалить пациента');
  }
}

export function openPatientForm({ patient = null, onDone = null } = {}) {
  const isEdit = Boolean(patient);
  const formId = `patient-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? 'Редактировать пациента' : 'Новый пациент',
    subtitle: isEdit ? patient.fullName : 'Заполните контактные данные для картотеки',
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="form-grid">
          <div class="field">
            <label for="${formId}-firstName">Имя *</label>
            <input class="input" id="${formId}-firstName" name="firstName" maxlength="80" required value="${esc(patient?.firstName || '')}" placeholder="Например, Анна">
          </div>
          <div class="field">
            <label for="${formId}-lastName">Фамилия *</label>
            <input class="input" id="${formId}-lastName" name="lastName" maxlength="80" required value="${esc(patient?.lastName || '')}" placeholder="Например, Соколова">
          </div>
          <div class="field">
            <label for="${formId}-birthDate">Дата рождения *</label>
            <input class="input" type="date" id="${formId}-birthDate" name="birthDate" max="${todayISO()}" required value="${esc(patient?.birthDate || '')}">
          </div>
          <div class="field">
            <label for="${formId}-gender">Пол *</label>
            <select class="select" id="${formId}-gender" name="gender" required>
              <option value="">Выберите пол</option>
              <option value="FEMALE" ${patient?.gender === 'FEMALE' ? 'selected' : ''}>Женский</option>
              <option value="MALE" ${patient?.gender === 'MALE' ? 'selected' : ''}>Мужской</option>
            </select>
          </div>
          <div class="field">
            <label for="${formId}-phone">Телефон *</label>
            <input class="input" id="${formId}-phone" name="phone" maxlength="25" required value="${esc(patient?.phone || '')}" placeholder="+7 (916) 100-10-01">
          </div>
          <div class="field">
            <label for="${formId}-email">Email</label>
            <input class="input" type="email" id="${formId}-email" name="email" maxlength="160" value="${esc(patient?.email || '')}" placeholder="patient@example.com">
          </div>
          <div class="field span-2">
            <label for="${formId}-address">Адрес</label>
            <input class="input" id="${formId}-address" name="address" maxlength="300" value="${esc(patient?.address || '')}" placeholder="Город, улица, дом">
          </div>
        </div>
        <div class="info-note">
          ${icon('shield')}
          <span>Используются только демонстрационные данные. Персональные сведения не передаются третьим лицам.</span>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>Отмена</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? 'Сохранить' : 'Добавить пациента'}</span>
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
      firstName: form.elements.namedItem('firstName').value.trim(),
      lastName: form.elements.namedItem('lastName').value.trim(),
      birthDate: form.elements.namedItem('birthDate').value,
      gender: form.elements.namedItem('gender').value,
      phone: form.elements.namedItem('phone').value.trim(),
      email: form.elements.namedItem('email').value.trim() || null,
      address: form.elements.namedItem('address').value.trim() || null,
    };

    if (!values.firstName || !values.lastName || !values.birthDate || !values.gender || !values.phone) {
      showFormAlert(form, 'Заполните обязательные поля, отмеченные звёздочкой');
      if (!values.firstName) form.elements.namedItem('firstName').classList.add('is-invalid');
      if (!values.lastName) form.elements.namedItem('lastName').classList.add('is-invalid');
      if (!values.birthDate) form.elements.namedItem('birthDate').classList.add('is-invalid');
      if (!values.gender) form.elements.namedItem('gender').classList.add('is-invalid');
      if (!values.phone) form.elements.namedItem('phone').classList.add('is-invalid');
      return;
    }

    setButtonLoading(submitButton, true, isEdit ? 'Сохраняем…' : 'Добавляем…');
    try {
      const saved = isEdit
        ? await api.patients.update(patient.id, values)
        : await api.patients.create(values);
      modal.close();
      toast(isEdit ? 'Карточка пациента обновлена' : `Пациент «${saved.fullName}» добавлен в картотеку`, 'success');
      onDone?.(saved);
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
    } finally {
      setButtonLoading(submitButton, false);
    }
  });
}

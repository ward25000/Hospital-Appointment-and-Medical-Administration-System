import { api, getErrorMessage } from '../api.js';
import { t } from '../i18n.js';
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
  formValue,
  addressFields,
  readAddress,
  parseIin,
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
            <small>${t('ИИН {0}', [esc(patient.iin)])}</small>
            <small>${esc(genderLabel(patient.gender))}${patient.userId ? `, ${t('есть личный кабинет')}` : ''}</small>
          </div>
        </div>
      </td>
      <td>
        <span class="cell-main">${formatDate(patient.birthDate)}</span>
        <span class="cell-sub">${patient.age} ${pluralRu(patient.age, ['год', 'года', 'лет'])}</span>
      </td>
      <td class="nowrap">${esc(patient.phone || '—')}</td>
      <td>${patient.email ? `<a class="link-button" href="mailto:${esc(patient.email)}">${esc(patient.email)}</a>` : '<span class="muted">—</span>'}</td>
      <td class="muted-cell">${esc(patient.address || '—')}</td>
      <td>
        <div class="actions">
          <button type="button" class="icon-action" data-appointments="${patient.id}" title="${t('Записи пациента')}" aria-label="${t('Записи пациента')}">${icon('calendar')}</button>
          <button type="button" class="icon-action" data-edit="${patient.id}" title="${t('Редактировать')}" aria-label="${t('Редактировать пациента')}">${icon('edit')}</button>
          <button type="button" class="icon-action danger" data-delete="${patient.id}" title="${t('Удалить')}" aria-label="${t('Удалить пациента')}">${icon('trash')}</button>
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
          title: t('Пациенты не найдены'),
          text: state.query
            ? t('Попробуйте изменить поисковый запрос или сбросить фильтр.')
            : t('В картотеке пока нет пациентов. Добавьте первую карточку.'),
          action: `<button type="button" class="btn btn-primary btn-sm" data-new>${icon('user-plus')}<span>${t('Добавить пациента')}</span></button>`,
        })}
      </div>
    `;
  }
  return `
    <table class="table">
      <thead>
        <tr>
          <th>${t('Пациент')}</th>
          <th>${t('Дата рождения')}</th>
          <th>${t('Телефон')}</th>
          <th>Email</th>
          <th>${t('Адрес')}</th>
          <th><span class="sr-only">${t('Действия')}</span></th>
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
          <input class="input" type="search" id="patient-search" placeholder="${t('Поиск по ИИН, имени, телефону или email')}" value="${esc(state.query)}">
        </div>
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" id="patient-refresh" title="${t('Обновить')}">${icon('refresh')}</button>
        <button type="button" class="btn btn-primary" id="patient-new">${icon('user-plus')}<span>${t('Добавить пациента')}</span></button>
      </div>

      <section class="card">
        <div class="card-header">
          <div>
            <div class="card-title">${t('Картотека пациентов')}</div>
            <div class="card-subtitle" data-count>${t('Загружаем картотеку…')}</div>
          </div>
        </div>
        <div class="table-wrap" data-table>${loadingState(t('Загружаем пациентов…'))}</div>
      </section>
    </div>
  `;

  const tableContainer = root.querySelector('[data-table]');
  const countContainer = root.querySelector('[data-count]');
  const searchInput = root.querySelector('#patient-search');

  const load = async () => {
    tableContainer.innerHTML = loadingState(t('Загружаем пациентов…'));
    try {
      const patients = await api.patients.list(state.query);
      tableContainer.innerHTML = renderTable(patients);
      countContainer.textContent = patients.length
        ? t('{0} {1} в картотеке', [patients.length, pluralRu(patients.length, ['пациент', 'пациента', 'пациентов'])])
        : t('Ничего не найдено');
      bindActions(patients);
    } catch (error) {
      tableContainer.innerHTML = errorState(getErrorMessage(error));
      countContainer.textContent = t('Ошибка загрузки');
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
    title: t('Удалить пациента?'),
    message: t('Карточка «{0}» будет удалена без возможности восстановления. Удаление возможно только при отсутствии записей на приём.', [patient.fullName]),
    confirmText: t('Удалить'),
    cancelText: t('Отмена'),
  });
  if (!confirmed) {
    return;
  }
  try {
    await api.patients.remove(patient.id);
    toast(t('Карточка «{0}» удалена', [patient.fullName]), 'success');
    onDone?.();
  } catch (error) {
    toast(getErrorMessage(error), 'error', t('Не удалось удалить пациента'));
  }
}

export function openPatientForm({ patient = null, onDone = null } = {}) {
  const isEdit = Boolean(patient);
  const formId = `patient-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? t('Редактировать пациента') : t('Новый пациент'),
    subtitle: isEdit ? patient.fullName : t('Заполните контактные данные для картотеки'),
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="field"><label for="${formId}-iin">${t('ИИН *')}</label>
          <input class="input" id="${formId}-iin" name="iin" inputmode="numeric" maxlength="12" autocomplete="off" value="${esc(patient?.iin || '')}"></div>
        <div class="form-grid-3">
          <div class="field"><label for="${formId}-lastName">${t('Фамилия *')}</label>
            <input class="input" id="${formId}-lastName" name="lastName" maxlength="80" value="${esc(patient?.lastName || '')}"></div>
          <div class="field"><label for="${formId}-firstName">${t('Имя *')}</label>
            <input class="input" id="${formId}-firstName" name="firstName" maxlength="80" value="${esc(patient?.firstName || '')}"></div>
          <div class="field"><label for="${formId}-middleName">${t('Отчество')}</label>
            <input class="input" id="${formId}-middleName" name="middleName" maxlength="80" value="${esc(patient?.middleName || '')}"></div>
        </div>
        <div class="form-grid">
          <div class="field"><label for="${formId}-birthDate">${t('Дата рождения *')}</label>
            <input class="input" type="date" id="${formId}-birthDate" name="birthDate" max="${todayISO()}" value="${esc(patient?.birthDate || '')}"></div>
          <div class="field"><label for="${formId}-gender">${t('Пол *')}</label>
            <select class="select" id="${formId}-gender" name="gender">
              <option value="">${t('Выберите пол')}</option>
              <option value="FEMALE" ${patient?.gender === 'FEMALE' ? 'selected' : ''}>${t('Женский')}</option>
              <option value="MALE" ${patient?.gender === 'MALE' ? 'selected' : ''}>${t('Мужской')}</option>
            </select></div>
          <div class="field"><label for="${formId}-phone">${t('Телефон *')}</label>
            <input class="input" type="tel" id="${formId}-phone" name="phone" maxlength="25" value="${esc(patient?.phone || '')}"></div>
          <div class="field"><label for="${formId}-email">Email</label>
            <input class="input" type="email" id="${formId}-email" name="email" maxlength="160" value="${esc(patient?.email || '')}"></div>
        </div>
        <div class="form-section"><h4>${t('Адрес проживания')}</h4>${addressFields(formId, patient || {})}
          ${patient && !patient.city ? `<p class="muted mt-8">${t('Сейчас в карте')}: ${esc(patient.address)}</p>` : ''}</div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? t('Сохранить') : t('Добавить пациента')}</span>
      </button>
    `,
  });

  const form = modal.body.querySelector('form');
  const submitButton = modal.footer.querySelector('[data-submit]');
  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => modal.close());

  form.elements.namedItem('iin').addEventListener('input', (event) => {
    const value = event.target.value.replace(/\D/g, '').slice(0, 12);
    event.target.value = value;
    const parsed = parseIin(value);
    if (parsed) {
      const birthDate = form.elements.namedItem('birthDate');
      const gender = form.elements.namedItem('gender');
      if (!birthDate.value) birthDate.value = parsed.birthDate;
      if (!gender.value) gender.value = parsed.gender;
    }
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    const values = {
      iin: formValue(form, 'iin'),
      firstName: formValue(form, 'firstName'),
      lastName: formValue(form, 'lastName'),
      middleName: formValue(form, 'middleName') || null,
      birthDate: formValue(form, 'birthDate') || null,
      gender: formValue(form, 'gender') || null,
      phone: formValue(form, 'phone'),
      email: formValue(form, 'email') || null,
      ...readAddress(form),
    };
    if (!values.city && !values.street && !values.house && patient && !patient.city) {
      values.address = patient.address;
    }

    setButtonLoading(submitButton, true, isEdit ? t('Сохраняем…') : t('Добавляем…'));
    try {
      const saved = isEdit
        ? await api.patients.update(patient.id, values)
        : await api.patients.create(values);
      modal.close();
      toast(isEdit ? t('Карточка пациента обновлена') : t('Пациент «{0}» добавлен в картотеку', [saved.fullName]), 'success');
      onDone?.(saved);
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
    } finally {
      setButtonLoading(submitButton, false);
    }
  });
}

import { api, getErrorMessage } from '../api.js';
import { t } from '../i18n.js';
import {
  icon,
  esc,
  formatMoney,
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
  segmentedControl,
  pluralRu,
} from '../ui.js';
import { isAdmin } from '../store.js';

const state = {
  tab: 'specializations',
};

function statusBadge(active) {
  return active
    ? `<span class="badge badge-active">${t('Активна')}</span>`
    : `<span class="badge badge-inactive">${t('Отключена')}</span>`;
}

function specializationRow(item) {
  return `
    <tr>
      <td>
        <span class="cell-main">${esc(item.name)}</span>
        <span class="cell-sub">${esc(item.description || t('Описание не заполнено'))}</span>
      </td>
      <td class="num">${item.doctorsCount} ${pluralRu(item.doctorsCount, ['врач', 'врача', 'врачей'])}</td>
      <td>${statusBadge(item.active)}</td>
      <td>
        <div class="actions">
          ${isAdmin() ? `
            <button type="button" class="icon-action" data-edit-specialization="${item.id}" title="${t('Редактировать')}" aria-label="${t('Редактировать специализацию')}">${icon('edit')}</button>
            <button type="button" class="icon-action danger" data-delete-specialization="${item.id}" title="${t('Удалить')}" aria-label="${t('Удалить специализацию')}">${icon('trash')}</button>
          ` : ''}
        </div>
      </td>
    </tr>
  `;
}

function serviceRow(item) {
  return `
    <tr>
      <td>
        <span class="cell-main">${esc(item.name)}</span>
        <span class="cell-sub">${esc(item.description || t('Описание не заполнено'))}</span>
        ${item.diagnostic ? `<span class="badge badge-soft mt-8">${t('Диагностика')}: ${esc(item.specializationName || '')}, ${t('каб. {0}', [esc(item.office || '—')])}</span>` : ''}
      </td>
      <td class="nowrap">${item.durationMinutes} ${t('мин')}</td>
      <td class="num revenue-cell">${formatMoney(item.price)}</td>
      <td>${statusBadge(item.active)}</td>
      <td>
        <div class="actions">
          ${isAdmin() ? `
            <button type="button" class="icon-action" data-edit-service="${item.id}" title="${t('Редактировать')}" aria-label="${t('Редактировать услугу')}">${icon('edit')}</button>
            <button type="button" class="icon-action danger" data-delete-service="${item.id}" title="${t('Удалить')}" aria-label="${t('Удалить услугу')}">${icon('trash')}</button>
          ` : ''}
        </div>
      </td>
    </tr>
  `;
}

export async function render(root, ctx) {
  root.innerHTML = `
    <div class="stack">
      <div class="flex items-center justify-between flex-wrap gap-12">
        ${segmentedControl([
          { value: 'specializations', label: t('Специализации'), count: undefined },
          { value: 'services', label: t('Медицинские услуги'), count: undefined },
        ], state.tab)}
        <div class="flex items-center gap-8">
          <button type="button" class="btn btn-secondary btn-icon" id="catalog-refresh" title="${t('Обновить')}">${icon('refresh')}</button>
          ${isAdmin()
            ? `<button type="button" class="btn btn-primary" id="catalog-add">${icon('plus')}<span data-add-label>${t('Добавить')}</span></button>`
            : ''}
        </div>
      </div>
      <div data-catalog>${loadingState(t('Загружаем справочники…'))}</div>
    </div>
  `;

  const catalogContainer = root.querySelector('[data-catalog]');
  const addButton = root.querySelector('#catalog-add');
  const addLabel = root.querySelector('[data-add-label]');
  let specializations = [];
  let services = [];

  const updateLabels = () => {
    if (addLabel) {
      addLabel.textContent = state.tab === 'specializations' ? t('Добавить специализацию') : t('Добавить услугу');
    }
    root.querySelectorAll('[data-tab]').forEach((button) => {
      button.classList.toggle('is-active', button.dataset.tab === state.tab);
      button.setAttribute('aria-selected', String(button.dataset.tab === state.tab));
    });
  };

  const renderActive = () => {
    if (state.tab === 'specializations') {
      catalogContainer.innerHTML = `
        <section class="card">
          <div class="card-header">
            <div>
              <div class="card-title">${t('Специализации клиники')}</div>
              <div class="card-subtitle">${specializations.length} ${pluralRu(specializations.length, ['направление', 'направления', 'направлений'])} ${t('в справочнике')}</div>
            </div>
          </div>
          ${specializations.length
            ? `<div class="table-wrap"><table class="table">
                <thead><tr><th>${t('Название')}</th><th class="num">${t('Врачи')}</th><th>${t('Статус')}</th><th><span class="sr-only">${t('Действия')}</span></th></tr></thead>
                <tbody>${specializations.map(specializationRow).join('')}</tbody>
              </table></div>`
            : emptyState({
              iconName: 'catalog',
              title: t('Справочник пуст'),
              text: t('Добавьте первое направление, чтобы назначать его врачам.'),
              action: isAdmin() ? `<button type="button" class="btn btn-primary btn-sm" data-add>${icon('plus')}<span>${t('Добавить специализацию')}</span></button>` : '',
            })}
        </section>
      `;
    } else {
      catalogContainer.innerHTML = `
        <section class="card">
          <div class="card-header">
            <div>
              <div class="card-title">${t('Медицинские услуги')}</div>
              <div class="card-subtitle">${services.length} ${pluralRu(services.length, ['услуга', 'услуги', 'услуг'])} ${t('в прайсе клиники')}</div>
            </div>
          </div>
          ${services.length
            ? `<div class="table-wrap"><table class="table">
                <thead><tr><th>${t('Услуга')}</th><th>${t('Длительность')}</th><th class="num">${t('Стоимость')}</th><th>${t('Статус')}</th><th><span class="sr-only">${t('Действия')}</span></th></tr></thead>
                <tbody>${services.map(serviceRow).join('')}</tbody>
              </table></div>`
            : emptyState({
              iconName: 'catalog',
              title: t('Прайс пуст'),
              text: t('Добавьте первую медицинскую услугу, чтобы её можно было выбрать при записи.'),
              action: isAdmin() ? `<button type="button" class="btn btn-primary btn-sm" data-add>${icon('plus')}<span>${t('Добавить услугу')}</span></button>` : '',
            })}
        </section>
      `;
    }
    bindRowActions();
  };

  const load = async () => {
    catalogContainer.innerHTML = loadingState(t('Загружаем справочники…'));
    try {
      [specializations, services] = await Promise.all([
        api.specializations.list(),
        api.services.list(),
      ]);
      const tabs = root.querySelectorAll('[data-tab]');
      if (tabs[0]) tabs[0].querySelector('.tab-count')?.remove();
      renderActive();
    } catch (error) {
      catalogContainer.innerHTML = errorState(getErrorMessage(error));
      catalogContainer.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  };

  const openAdd = () => {
    if (state.tab === 'specializations') {
      openSpecializationForm({ onDone: load });
    } else {
      openServiceForm({ departments: specializations, onDone: load });
    }
  };

  const bindRowActions = () => {
    catalogContainer.querySelectorAll('[data-edit-specialization]').forEach((button) => {
      button.addEventListener('click', () => {
        const item = specializations.find((entry) => Number(entry.id) === Number(button.dataset.editSpecialization));
        if (item) openSpecializationForm({ item, onDone: load });
      });
    });
    catalogContainer.querySelectorAll('[data-delete-specialization]').forEach((button) => {
      button.addEventListener('click', () => {
        const item = specializations.find((entry) => Number(entry.id) === Number(button.dataset.deleteSpecialization));
        if (item) removeItem('specialization', item, load);
      });
    });
    catalogContainer.querySelectorAll('[data-edit-service]').forEach((button) => {
      button.addEventListener('click', () => {
        const item = services.find((entry) => Number(entry.id) === Number(button.dataset.editService));
        if (item) openServiceForm({ item, departments: specializations, onDone: load });
      });
    });
    catalogContainer.querySelectorAll('[data-delete-service]').forEach((button) => {
      button.addEventListener('click', () => {
        const item = services.find((entry) => Number(entry.id) === Number(button.dataset.deleteService));
        if (item) removeItem('service', item, load);
      });
    });
    catalogContainer.querySelectorAll('[data-add]').forEach((button) => {
      button.addEventListener('click', openAdd);
    });
  };

  root.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => {
      state.tab = button.dataset.tab;
      updateLabels();
      renderActive();
    });
  });
  root.querySelector('#catalog-refresh').addEventListener('click', load);
  addButton?.addEventListener('click', openAdd);

  updateLabels();
  await load();
}

async function removeItem(type, item, onDone) {
  const isSpecialization = type === 'specialization';
  const confirmed = await confirmDialog({
    title: isSpecialization ? t('Удалить специализацию?') : t('Удалить услугу?'),
    message: isSpecialization
      ? t('«{0}» будет удалена из справочника. Удаление невозможно, если к направлению привязаны врачи.', [item.name])
      : t('«{0}» будет удалена из прайса. Удаление невозможно, если услуга уже используется в приёмах.', [item.name]),
    confirmText: t('Удалить'),
    cancelText: t('Отмена'),
  });
  if (!confirmed) {
    return;
  }
  try {
    if (isSpecialization) {
      await api.specializations.remove(item.id);
    } else {
      await api.services.remove(item.id);
    }
    toast(isSpecialization ? t('Специализация удалена') : t('Услуга удалена'), 'success');
    onDone?.();
  } catch (error) {
    toast(getErrorMessage(error), 'error', t('Не удалось удалить запись'));
  }
}

function translatedFields(formId, item) {
  const langs = [['', 'RU', ' *'], ['Kk', 'KZ', ''], ['En', 'EN', '']];
  const value = (name, suffix) => esc(item?.[name + (suffix || 'Ru')] || '');
  return langs.map(([suffix, label, mark]) => `
    <div class="field"><label for="${formId}-name${suffix}">${t('Название')} (${label})${mark}</label>
      <input class="input" id="${formId}-name${suffix}" name="name${suffix}" maxlength="120" value="${value('name', suffix)}"></div>`).join('')
    + langs.map(([suffix, label]) => `
    <div class="field"><label for="${formId}-description${suffix}">${t('Описание')} (${label})</label>
      <textarea class="textarea" style="min-height:58px" id="${formId}-description${suffix}" name="description${suffix}" maxlength="500">${value('description', suffix)}</textarea></div>`).join('');
}

function readTranslated(form) {
  return Object.fromEntries(['name', 'nameKk', 'nameEn', 'description', 'descriptionKk', 'descriptionEn']
    .map((name) => [name, form.elements.namedItem(name).value.trim() || null]));
}

function openSpecializationForm({ item = null, onDone = null } = {}) {
  const isEdit = Boolean(item);
  const formId = `specialization-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? t('Редактировать специализацию') : t('Новая специализация'),
    subtitle: t('Направление, к которому относятся врачи клиники'),
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        ${translatedFields(formId, item)}
        <div class="check-row">
          <input class="checkbox" type="checkbox" id="${formId}-active" name="active" ${item?.active === false ? '' : 'checked'}>
          <label class="check-label" for="${formId}-active">${t('Специализация доступна для выбора')}</label>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? t('Сохранить') : t('Добавить')}</span>
      </button>
    `,
  });

  bindCatalogForm(modal, formId, isEdit, async (values) => {
    if (isEdit) {
      await api.specializations.update(item.id, values);
    } else {
      await api.specializations.create(values);
    }
    toast(isEdit ? t('Специализация обновлена') : t('Специализация добавлена'), 'success');
    onDone?.();
  }, (form) => ({
    ...readTranslated(form),
    active: form.elements.namedItem('active').checked,
  }));
}

function openServiceForm({ item = null, departments = [], onDone = null } = {}) {
  const isEdit = Boolean(item);
  const formId = `service-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? t('Редактировать услугу') : t('Новая медицинская услуга'),
    subtitle: t('Стоимость и длительность влияют на расчёт слотов и выручки'),
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        ${translatedFields(formId, item)}
        <div class="form-grid">
          <div class="field">
            <label for="${formId}-price">${t('Стоимость, ₸ *')}</label>
            <input class="input" type="number" id="${formId}-price" name="price" min="0" step="0.01" required value="${item ? Number(item.price) : ''}" placeholder="2500">
          </div>
          <div class="field">
            <label for="${formId}-duration">${t('Длительность, мин *')}</label>
            <input class="input" type="number" id="${formId}-duration" name="durationMinutes" min="5" max="240" step="5" required value="${item?.durationMinutes || 30}">
          </div>
        </div>
        <div class="form-grid">
          <div class="field"><label for="${formId}-specializationId">${t('Отделение')}</label>
            <select class="select" id="${formId}-specializationId" name="specializationId">
              <option value="">${t('Не указано')}</option>
              ${departments.map((entry) => `<option value="${entry.id}" ${entry.id === item?.specializationId ? 'selected' : ''}>${esc(entry.name)}</option>`).join('')}
            </select></div>
          <div class="field"><label for="${formId}-office">${t('Кабинет')}</label>
            <input class="input" id="${formId}-office" name="office" maxlength="30" value="${esc(item?.office || '')}"></div>
        </div>
        <div class="check-row">
          <input class="checkbox" type="checkbox" id="${formId}-diagnostic" name="diagnostic" ${item?.diagnostic ? 'checked' : ''}>
          <label class="check-label" for="${formId}-diagnostic">${t('Диагностика или анализ: врач может выписать направление')}</label>
        </div>
        <div class="check-row">
          <input class="checkbox" type="checkbox" id="${formId}-active" name="active" ${item?.active === false ? '' : 'checked'}>
          <label class="check-label" for="${formId}-active">${t('Услуга доступна для записи')}</label>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>${t('Отмена')}</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? t('Сохранить') : t('Добавить')}</span>
      </button>
    `,
  });

  bindCatalogForm(modal, formId, isEdit, async (values) => {
    if (isEdit) {
      await api.services.update(item.id, values);
    } else {
      await api.services.create(values);
    }
    toast(isEdit ? t('Услуга обновлена') : t('Услуга добавлена в прайс'), 'success');
    onDone?.();
  }, (form) => ({
    ...readTranslated(form),
    price: Number(form.elements.namedItem('price').value),
    durationMinutes: Number(form.elements.namedItem('durationMinutes').value),
    specializationId: Number(form.elements.namedItem('specializationId').value) || null,
    office: form.elements.namedItem('office').value.trim() || null,
    diagnostic: form.elements.namedItem('diagnostic').checked,
    active: form.elements.namedItem('active').checked,
  }));
}

function bindCatalogForm(modal, formId, isEdit, save, readValues) {
  const form = modal.body.querySelector('form');
  const submitButton = modal.footer.querySelector('[data-submit]');
  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => modal.close());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    const values = readValues(form);
    if (!values.name) {
      showFormAlert(form, t('Укажите название'));
      form.elements.namedItem('name').classList.add('is-invalid');
      return;
    }
    if (values.price !== undefined && (!Number.isFinite(values.price) || values.price < 0)) {
      showFormAlert(form, t('Укажите корректную стоимость услуги'));
      return;
    }
    if (values.durationMinutes !== undefined
        && (!Number.isFinite(values.durationMinutes) || values.durationMinutes < 5 || values.durationMinutes > 240)) {
      showFormAlert(form, t('Длительность должна быть от 5 до 240 минут'));
      return;
    }

    setButtonLoading(submitButton, true, isEdit ? t('Сохраняем…') : t('Добавляем…'));
    try {
      await save(values);
      modal.close();
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
    } finally {
      setButtonLoading(submitButton, false);
    }
  });
}

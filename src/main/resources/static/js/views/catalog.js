import { api, getErrorMessage } from '../api.js';
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
    ? '<span class="badge badge-active">Активна</span>'
    : '<span class="badge badge-inactive">Отключена</span>';
}

function specializationRow(item) {
  return `
    <tr>
      <td>
        <span class="cell-main">${esc(item.name)}</span>
        <span class="cell-sub">${esc(item.description || 'Описание не заполнено')}</span>
      </td>
      <td class="num">${item.doctorsCount} ${pluralRu(item.doctorsCount, ['врач', 'врача', 'врачей'])}</td>
      <td>${statusBadge(item.active)}</td>
      <td>
        <div class="actions">
          ${isAdmin() ? `
            <button type="button" class="icon-action" data-edit-specialization="${item.id}" title="Редактировать" aria-label="Редактировать специализацию">${icon('edit')}</button>
            <button type="button" class="icon-action danger" data-delete-specialization="${item.id}" title="Удалить" aria-label="Удалить специализацию">${icon('trash')}</button>
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
        <span class="cell-sub">${esc(item.description || 'Описание не заполнено')}</span>
      </td>
      <td class="nowrap">${item.durationMinutes} мин</td>
      <td class="num revenue-cell">${formatMoney(item.price)}</td>
      <td>${statusBadge(item.active)}</td>
      <td>
        <div class="actions">
          ${isAdmin() ? `
            <button type="button" class="icon-action" data-edit-service="${item.id}" title="Редактировать" aria-label="Редактировать услугу">${icon('edit')}</button>
            <button type="button" class="icon-action danger" data-delete-service="${item.id}" title="Удалить" aria-label="Удалить услугу">${icon('trash')}</button>
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
          { value: 'specializations', label: 'Специализации', count: undefined },
          { value: 'services', label: 'Медицинские услуги', count: undefined },
        ], state.tab)}
        <div class="flex items-center gap-8">
          <button type="button" class="btn btn-secondary btn-icon" id="catalog-refresh" title="Обновить">${icon('refresh')}</button>
          ${isAdmin()
            ? `<button type="button" class="btn btn-primary" id="catalog-add">${icon('plus')}<span data-add-label>Добавить</span></button>`
            : ''}
        </div>
      </div>
      <div data-catalog>${loadingState('Загружаем справочники…')}</div>
    </div>
  `;

  const catalogContainer = root.querySelector('[data-catalog]');
  const addButton = root.querySelector('#catalog-add');
  const addLabel = root.querySelector('[data-add-label]');
  let specializations = [];
  let services = [];

  const updateLabels = () => {
    if (addLabel) {
      addLabel.textContent = state.tab === 'specializations' ? 'Добавить специализацию' : 'Добавить услугу';
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
              <div class="card-title">Специализации клиники</div>
              <div class="card-subtitle">${specializations.length} ${pluralRu(specializations.length, ['направление', 'направления', 'направлений'])} в справочнике</div>
            </div>
          </div>
          ${specializations.length
            ? `<div class="table-wrap"><table class="table">
                <thead><tr><th>Название</th><th class="num">Врачи</th><th>Статус</th><th><span class="sr-only">Действия</span></th></tr></thead>
                <tbody>${specializations.map(specializationRow).join('')}</tbody>
              </table></div>`
            : emptyState({
              iconName: 'catalog',
              title: 'Справочник пуст',
              text: 'Добавьте первое направление, чтобы назначать его врачам.',
              action: isAdmin() ? `<button type="button" class="btn btn-primary btn-sm" data-add>${icon('plus')}<span>Добавить специализацию</span></button>` : '',
            })}
        </section>
      `;
    } else {
      catalogContainer.innerHTML = `
        <section class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Медицинские услуги</div>
              <div class="card-subtitle">${services.length} ${pluralRu(services.length, ['услуга', 'услуги', 'услуг'])} в прайсе клиники</div>
            </div>
          </div>
          ${services.length
            ? `<div class="table-wrap"><table class="table">
                <thead><tr><th>Услуга</th><th>Длительность</th><th class="num">Стоимость</th><th>Статус</th><th><span class="sr-only">Действия</span></th></tr></thead>
                <tbody>${services.map(serviceRow).join('')}</tbody>
              </table></div>`
            : emptyState({
              iconName: 'catalog',
              title: 'Прайс пуст',
              text: 'Добавьте первую медицинскую услугу, чтобы её можно было выбрать при записи.',
              action: isAdmin() ? `<button type="button" class="btn btn-primary btn-sm" data-add>${icon('plus')}<span>Добавить услугу</span></button>` : '',
            })}
        </section>
      `;
    }
    bindRowActions();
  };

  const load = async () => {
    catalogContainer.innerHTML = loadingState('Загружаем справочники…');
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
      openServiceForm({ onDone: load });
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
        if (item) openServiceForm({ item, onDone: load });
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
    title: isSpecialization ? 'Удалить специализацию?' : 'Удалить услугу?',
    message: isSpecialization
      ? `«${item.name}» будет удалена из справочника. Удаление невозможно, если к направлению привязаны врачи.`
      : `«${item.name}» будет удалена из прайса. Удаление невозможно, если услуга уже используется в приёмах.`,
    confirmText: 'Удалить',
    cancelText: 'Отмена',
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
    toast(isSpecialization ? 'Специализация удалена' : 'Услуга удалена', 'success');
    onDone?.();
  } catch (error) {
    toast(getErrorMessage(error), 'error', 'Не удалось удалить запись');
  }
}

function openSpecializationForm({ item = null, onDone = null } = {}) {
  const isEdit = Boolean(item);
  const formId = `specialization-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? 'Редактировать специализацию' : 'Новая специализация',
    subtitle: 'Направление, к которому относятся врачи клиники',
    size: 'sm',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="field">
          <label for="${formId}-name">Название *</label>
          <input class="input" id="${formId}-name" name="name" maxlength="120" required value="${esc(item?.name || '')}" placeholder="Например, Кардиология">
        </div>
        <div class="field">
          <label for="${formId}-description">Описание</label>
          <textarea class="textarea" id="${formId}-description" name="description" maxlength="500" placeholder="Кратко о направлении">${esc(item?.description || '')}</textarea>
        </div>
        <div class="check-row">
          <input class="checkbox" type="checkbox" id="${formId}-active" name="active" ${item?.active === false ? '' : 'checked'}>
          <label class="check-label" for="${formId}-active">Специализация доступна для выбора</label>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>Отмена</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? 'Сохранить' : 'Добавить'}</span>
      </button>
    `,
  });

  bindCatalogForm(modal, formId, isEdit, async (values) => {
    if (isEdit) {
      await api.specializations.update(item.id, values);
    } else {
      await api.specializations.create(values);
    }
    toast(isEdit ? 'Специализация обновлена' : 'Специализация добавлена', 'success');
    onDone?.();
  }, (form) => ({
    name: form.elements.namedItem('name').value.trim(),
    description: form.elements.namedItem('description').value.trim() || null,
    active: form.elements.namedItem('active').checked,
  }));
}

function openServiceForm({ item = null, onDone = null } = {}) {
  const isEdit = Boolean(item);
  const formId = `service-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? 'Редактировать услугу' : 'Новая медицинская услуга',
    subtitle: 'Стоимость и длительность влияют на расчёт слотов и выручки',
    size: 'sm',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="field">
          <label for="${formId}-name">Название *</label>
          <input class="input" id="${formId}-name" name="name" maxlength="150" required value="${esc(item?.name || '')}" placeholder="Например, Первичная консультация">
        </div>
        <div class="field">
          <label for="${formId}-description">Описание</label>
          <textarea class="textarea" id="${formId}-description" name="description" maxlength="500">${esc(item?.description || '')}</textarea>
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="${formId}-price">Стоимость, ₽ *</label>
            <input class="input" type="number" id="${formId}-price" name="price" min="0" step="0.01" required value="${item ? Number(item.price) : ''}" placeholder="2500">
          </div>
          <div class="field">
            <label for="${formId}-duration">Длительность, мин *</label>
            <input class="input" type="number" id="${formId}-duration" name="durationMinutes" min="5" max="240" step="5" required value="${item?.durationMinutes || 30}">
          </div>
        </div>
        <div class="check-row">
          <input class="checkbox" type="checkbox" id="${formId}-active" name="active" ${item?.active === false ? '' : 'checked'}>
          <label class="check-label" for="${formId}-active">Услуга доступна для записи</label>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      <button type="button" class="btn btn-secondary" data-cancel>Отмена</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? 'Сохранить' : 'Добавить'}</span>
      </button>
    `,
  });

  bindCatalogForm(modal, formId, isEdit, async (values) => {
    if (isEdit) {
      await api.services.update(item.id, values);
    } else {
      await api.services.create(values);
    }
    toast(isEdit ? 'Услуга обновлена' : 'Услуга добавлена в прайс', 'success');
    onDone?.();
  }, (form) => ({
    name: form.elements.namedItem('name').value.trim(),
    description: form.elements.namedItem('description').value.trim() || null,
    price: Number(form.elements.namedItem('price').value),
    durationMinutes: Number(form.elements.namedItem('durationMinutes').value),
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
      showFormAlert(form, 'Укажите название');
      form.elements.namedItem('name').classList.add('is-invalid');
      return;
    }
    if (values.price !== undefined && (!Number.isFinite(values.price) || values.price < 0)) {
      showFormAlert(form, 'Укажите корректную стоимость услуги');
      return;
    }
    if (values.durationMinutes !== undefined
        && (!Number.isFinite(values.durationMinutes) || values.durationMinutes < 5 || values.durationMinutes > 240)) {
      showFormAlert(form, 'Длительность должна быть от 5 до 240 минут');
      return;
    }

    setButtonLoading(submitButton, true, isEdit ? 'Сохраняем…' : 'Добавляем…');
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

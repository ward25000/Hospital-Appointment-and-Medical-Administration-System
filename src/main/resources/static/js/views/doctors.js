import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  avatar,
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
} from '../ui.js';
import { canSchedule, isAdmin } from '../store.js';
import { openBooking } from './appointments.js';

const state = {
  query: '',
  specializationId: '',
};

let specializationsCache = [];

function doctorCard(doctor, callbacks) {
  const inactive = !doctor.active;
  return `
    <article class="card doctor-card">
      <div class="doctor-card-top">
        ${avatar(doctor.fullName)}
        <div class="doctor-name">
          <strong>${esc(doctor.fullName)}</strong>
          <small>${esc(doctor.specializationName)}</small>
        </div>
        ${inactive
          ? '<span class="badge badge-inactive doctor-status">Неактивен</span>'
          : '<span class="badge badge-active doctor-status">Активен</span>'}
      </div>
      <p class="doctor-bio">${esc(doctor.bio || 'Описание специалиста пока не заполнено.')}</p>
      <div class="doctor-info">
        <div class="doctor-info-row">${icon('building')}<span>Кабинет ${esc(doctor.office || '—')}</span></div>
        <div class="doctor-info-row">${icon('phone')}<span>${esc(doctor.phone || '—')}</span></div>
        <div class="doctor-info-row">${icon('mail')}<span>${esc(doctor.email || '—')}</span></div>
      </div>
      <div class="doctor-card-foot">
        ${canSchedule() && doctor.active
          ? `<button type="button" class="btn btn-primary btn-sm" data-book="${doctor.id}">${icon('plus')}<span>Записаться</span></button>`
          : ''}
        <button type="button" class="btn btn-secondary btn-sm" data-profile="${doctor.id}">${icon('eye')}<span>Профиль</span></button>
        ${isAdmin()
          ? `<button type="button" class="icon-action" data-edit="${doctor.id}" title="Редактировать врача" aria-label="Редактировать врача">${icon('edit')}</button>`
          : ''}
      </div>
    </article>
  `;
}

export async function render(root, ctx) {
  root.innerHTML = `
    <div class="stack">
      <div class="page-toolbar">
        <div class="search-box">
          ${icon('search')}
          <input class="input" type="search" id="doctor-search" placeholder="Поиск по имени или специализации" value="${esc(state.query)}">
        </div>
        <select class="select" id="doctor-specialization" style="min-width:200px">
          <option value="">Все специализации</option>
        </select>
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" id="doctor-refresh" title="Обновить">${icon('refresh')}</button>
        ${isAdmin()
          ? `<button type="button" class="btn btn-primary" id="doctor-new">${icon('user-plus')}<span>Добавить врача</span></button>`
          : ''}
      </div>
      <div class="section-head">
        <div>
          <h3>Специалисты клиники</h3>
          <p data-count>Загружаем список врачей…</p>
        </div>
      </div>
      <div data-grid>${loadingState('Загружаем врачей…')}</div>
    </div>
  `;

  const grid = root.querySelector('[data-grid]');
  const countContainer = root.querySelector('[data-count]');
  const searchInput = root.querySelector('#doctor-search');
  const specializationSelect = root.querySelector('#doctor-specialization');

  try {
    specializationsCache = await api.specializations.list();
    specializationSelect.innerHTML = `
      <option value="">Все специализации</option>
      ${specializationsCache.map((item) => `
        <option value="${item.id}" ${String(item.id) === String(state.specializationId) ? 'selected' : ''}>${esc(item.name)}</option>
      `).join('')}
    `;
  } catch {
    specializationsCache = [];
  }

  const load = async () => {
    grid.innerHTML = loadingState('Загружаем врачей…');
    try {
      const doctors = await api.doctors.list({
        query: state.query || undefined,
        specializationId: state.specializationId || undefined,
      });
      countContainer.textContent = doctors.length
        ? `${doctors.length} ${pluralRu(doctors.length, ['специалист', 'специалиста', 'специалистов'])} найдено`
        : 'Ничего не найдено';
      if (doctors.length === 0) {
        grid.innerHTML = `
          <div class="card">
            ${emptyState({
              iconName: 'doctors',
              title: 'Врачи не найдены',
              text: 'Измените поисковый запрос или выберите другую специализацию.',
              action: isAdmin()
                ? `<button type="button" class="btn btn-primary btn-sm" data-new>${icon('user-plus')}<span>Добавить врача</span></button>`
                : '',
            })}
          </div>
        `;
      } else {
        grid.innerHTML = `<div class="doctor-grid">${doctors.map((doctor) => doctorCard(doctor)).join('')}</div>`;
      }
      bindActions(doctors);
    } catch (error) {
      grid.innerHTML = errorState(getErrorMessage(error));
      countContainer.textContent = 'Ошибка загрузки';
      grid.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  };

  const bindActions = (doctors) => {
    grid.querySelectorAll('[data-book]').forEach((button) => {
      button.addEventListener('click', () => openBooking({
        doctorId: Number(button.dataset.book),
        onDone: load,
      }));
    });
    grid.querySelectorAll('[data-profile]').forEach((button) => {
      button.addEventListener('click', () => {
        const doctor = doctors.find((item) => Number(item.id) === Number(button.dataset.profile));
        if (doctor) openDoctorProfile(doctor, load);
      });
    });
    grid.querySelectorAll('[data-edit]').forEach((button) => {
      button.addEventListener('click', () => {
        const doctor = doctors.find((item) => Number(item.id) === Number(button.dataset.edit));
        if (doctor) openDoctorForm({ doctor, onDone: load });
      });
    });
    grid.querySelectorAll('[data-new]').forEach((button) => {
      button.addEventListener('click', () => openDoctorForm({ onDone: load }));
    });
  };

  searchInput.addEventListener('input', debounce(() => {
    state.query = searchInput.value.trim();
    load();
  }, 350));
  specializationSelect.addEventListener('change', () => {
    state.specializationId = specializationSelect.value;
    load();
  });
  root.querySelector('#doctor-refresh').addEventListener('click', load);
  root.querySelector('#doctor-new')?.addEventListener('click', () => openDoctorForm({ onDone: load }));

  await load();
}

function openDoctorProfile(doctor, onDone) {
  const modal = openModal({
    title: doctor.fullName,
    subtitle: `${doctor.specializationName} · кабинет ${doctor.office || '—'}`,
    size: 'lg',
    body: `
      <div class="flex items-center gap-12 mb-16">
        ${avatar(doctor.fullName, 'avatar-lg')}
        <div>
          <div class="cell-main">${esc(doctor.fullName)}</div>
          <div class="cell-sub">${esc(doctor.specializationName)} · @${esc(doctor.username)}</div>
        </div>
        ${doctor.active
          ? '<span class="badge badge-active" style="margin-left:auto">Активен</span>'
          : '<span class="badge badge-inactive" style="margin-left:auto">Неактивен</span>'}
      </div>
      <div class="detail-grid">
        <div class="detail-item">
          <span class="detail-label">Специализация</span>
          <span class="detail-value">${esc(doctor.specializationName)}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Кабинет</span>
          <span class="detail-value">${esc(doctor.office || '—')}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Телефон</span>
          <span class="detail-value">${doctor.phone ? `<a class="link-button" href="tel:${esc(doctor.phone.replace(/[^+0-9]/g, ''))}">${esc(doctor.phone)}</a>` : '—'}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Email</span>
          <span class="detail-value">${doctor.email ? `<a class="link-button" href="mailto:${esc(doctor.email)}">${esc(doctor.email)}</a>` : '—'}</span>
        </div>
      </div>
      <div class="mt-16">
        <span class="detail-label">О специалисте</span>
        <div class="detail-note">${esc(doctor.bio || 'Описание пока не заполнено.')}</div>
      </div>
    `,
    footer: `
      <span class="spacer"></span>
      ${isAdmin() ? `<button type="button" class="btn btn-secondary" data-edit>${icon('edit')}<span>Редактировать</span></button>` : ''}
      ${canSchedule() && doctor.active ? `<button type="button" class="btn btn-primary" data-book>${icon('plus')}<span>Записаться</span></button>` : ''}
      <button type="button" class="btn btn-secondary" data-close>Закрыть</button>
    `,
  });

  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());
  modal.footer.querySelector('[data-book]')?.addEventListener('click', () => {
    modal.close();
    openBooking({ doctorId: doctor.id, onDone });
  });
  modal.footer.querySelector('[data-edit]')?.addEventListener('click', () => {
    modal.close();
    openDoctorForm({ doctor, onDone });
  });
}

function openDoctorForm({ doctor = null, onDone = null } = {}) {
  const isEdit = Boolean(doctor);
  const formId = `doctor-form-${Date.now()}`;
  const modal = openModal({
    title: isEdit ? 'Редактировать врача' : 'Новый врач',
    subtitle: isEdit ? doctor.fullName : 'Создайте профиль специалиста и учётную запись',
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="form-grid">
          <div class="field">
            <label for="${formId}-firstName">Имя *</label>
            <input class="input" id="${formId}-firstName" name="firstName" maxlength="80" required value="${esc(doctor?.firstName || '')}">
          </div>
          <div class="field">
            <label for="${formId}-lastName">Фамилия *</label>
            <input class="input" id="${formId}-lastName" name="lastName" maxlength="80" required value="${esc(doctor?.lastName || '')}">
          </div>
          <div class="field">
            <label for="${formId}-specializationId">Специализация *</label>
            <select class="select" id="${formId}-specializationId" name="specializationId" required>
              <option value="">Выберите специализацию</option>
              ${specializationsCache.map((item) => `
                <option value="${item.id}" ${Number(item.id) === Number(doctor?.specializationId) ? 'selected' : ''}>
                  ${esc(item.name)}${item.active ? '' : ' (неактивна)'}
                </option>
              `).join('')}
            </select>
          </div>
          <div class="field">
            <label for="${formId}-office">Кабинет *</label>
            <input class="input" id="${formId}-office" name="office" maxlength="30" required value="${esc(doctor?.office || '')}" placeholder="Например, 201">
          </div>
          <div class="field">
            <label for="${formId}-email">Email *</label>
            <input class="input" type="email" id="${formId}-email" name="email" maxlength="160" required value="${esc(doctor?.email || '')}">
          </div>
          <div class="field">
            <label for="${formId}-phone">Телефон *</label>
            <input class="input" id="${formId}-phone" name="phone" maxlength="25" required value="${esc(doctor?.phone || '')}" placeholder="+7 (495) 120-20-01">
          </div>
          ${isEdit ? `
            <div class="field span-2">
              <div class="check-row">
                <input class="checkbox" type="checkbox" id="${formId}-active" name="active" ${doctor.active ? 'checked' : ''}>
                <label class="check-label" for="${formId}-active">Врач ведёт приём (учётная запись активна)</label>
              </div>
            </div>
          ` : `
            <div class="field">
              <label for="${formId}-username">Логин *</label>
              <input class="input" id="${formId}-username" name="username" maxlength="60" required placeholder="doctor.ivanov" autocomplete="off">
              <span class="hint">4–60 символов: латиница, цифры, точка, дефис, подчёркивание</span>
            </div>
            <div class="field">
              <label for="${formId}-password">Пароль *</label>
              <input class="input" type="password" id="${formId}-password" name="password" minlength="8" maxlength="100" required autocomplete="new-password">
              <span class="hint">Не менее 8 символов</span>
            </div>
          `}
          <div class="field span-2">
            <label for="${formId}-bio">О специалисте</label>
            <textarea class="textarea" id="${formId}-bio" name="bio" maxlength="1000" placeholder="Опыт, направления работы, категория">${esc(doctor?.bio || '')}</textarea>
          </div>
        </div>
      </form>
    `,
    footer: `
      <span class="spacer"></span>
      ${isEdit ? `<button type="button" class="btn btn-danger-soft" data-delete>${icon('trash')}<span>Удалить</span></button>` : ''}
      <button type="button" class="btn btn-secondary" data-cancel>Отмена</button>
      <button type="submit" form="${formId}" class="btn btn-primary" data-submit>
        ${icon('check')}<span>${isEdit ? 'Сохранить' : 'Создать врача'}</span>
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
      specializationId: Number(form.elements.namedItem('specializationId').value),
      email: form.elements.namedItem('email').value.trim(),
      phone: form.elements.namedItem('phone').value.trim(),
      office: form.elements.namedItem('office').value.trim(),
      bio: form.elements.namedItem('bio').value.trim() || null,
    };
    if (!values.firstName || !values.lastName || !values.specializationId || !values.email || !values.phone || !values.office) {
      showFormAlert(form, 'Заполните обязательные поля формы');
      return;
    }

    let payload = values;
    if (isEdit) {
      payload = { ...values, active: form.elements.namedItem('active').checked };
    } else {
      const username = form.elements.namedItem('username').value.trim();
      const password = form.elements.namedItem('password').value;
      if (!username || password.length < 8) {
        showFormAlert(form, 'Укажите логин и пароль не короче 8 символов');
        return;
      }
      payload = { ...values, username, password };
    }

    setButtonLoading(submitButton, true, isEdit ? 'Сохраняем…' : 'Создаём…');
    try {
      const saved = isEdit
        ? await api.doctors.update(doctor.id, payload)
        : await api.doctors.create(payload);
      modal.close();
      toast(isEdit ? 'Профиль врача обновлён' : `Врач «${saved.fullName}» добавлен`, 'success');
      onDone?.(saved);
    } catch (error) {
      applyFieldErrors(form, error.fieldErrors);
      showFormAlert(form, getErrorMessage(error));
    } finally {
      setButtonLoading(submitButton, false);
    }
  });

  modal.footer.querySelector('[data-delete]')?.addEventListener('click', async () => {
    const confirmed = await confirmDialog({
      title: 'Удалить врача?',
      message: `Профиль «${doctor.fullName}» и его учётная запись будут удалены. Если у врача есть приёмы, удаление невозможно — отключите его вместо этого.`,
      confirmText: 'Удалить',
      cancelText: 'Отмена',
    });
    if (!confirmed) {
      return;
    }
    try {
      await api.doctors.remove(doctor.id);
      modal.close();
      toast(`Профиль «${doctor.fullName}» удалён`, 'success');
      onDone?.();
    } catch (error) {
      showFormAlert(form, getErrorMessage(error));
    }
  });
}

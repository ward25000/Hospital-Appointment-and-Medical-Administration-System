import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  avatar,
  toast,
  formatDate,
  genderLabel,
  openModal,
  loadingState,
  errorState,
  setButtonLoading,
  clearFieldErrors,
  showFormAlert,
  applyFieldErrors,
  formValue,
  setFormValues,
  todayISO,
} from '../ui.js';

export function openProfileForm(profile, onDone) {
  const formId = `profile-form-${Date.now()}`;
  const modal = openModal({
    title: 'Редактировать профиль',
    subtitle: 'ИИН и адрес обязательны для пациента',
    size: 'lg',
    body: `
      <form id="${formId}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="form-grid">
          <div class="field">
            <label for="${formId}-firstName">Имя *</label>
            <input class="input" id="${formId}-firstName" name="firstName" maxlength="80" required value="${esc(profile.firstName)}">
          </div>
          <div class="field">
            <label for="${formId}-lastName">Фамилия *</label>
            <input class="input" id="${formId}-lastName" name="lastName" maxlength="80" required value="${esc(profile.lastName)}">
          </div>
          <div class="field">
            <label for="${formId}-birthDate">Дата рождения *</label>
            <input class="input" type="date" id="${formId}-birthDate" name="birthDate" max="${todayISO()}" required value="${esc(profile.birthDate)}">
          </div>
          <div class="field">
            <label for="${formId}-gender">Пол *</label>
            <select class="select" id="${formId}-gender" name="gender" required>
              <option value="MALE" ${profile.gender === 'MALE' ? 'selected' : ''}>Мужской</option>
              <option value="FEMALE" ${profile.gender === 'FEMALE' ? 'selected' : ''}>Женский</option>
            </select>
          </div>
          <div class="field">
            <label for="${formId}-phone">Телефон *</label>
            <input class="input" id="${formId}-phone" name="phone" maxlength="30" required value="${esc(profile.phone)}" placeholder="+7 (916) 100-10-01">
          </div>
          <div class="field">
            <label for="${formId}-email">Email</label>
            <input class="input" type="email" id="${formId}-email" name="email" maxlength="160" value="${esc(profile.email || '')}" placeholder="patient@example.com">
          </div>
          <div class="field span-2">
            <label for="${formId}-address">Адрес проживания *</label>
            <input class="input" id="${formId}-address" name="address" maxlength="300" required value="${esc(profile.address)}" placeholder="Город, улица, дом, квартира">
          </div>
        </div>
      </form>
    `,
    footer: `<span class="spacer"></span><button type="button" class="btn btn-secondary" data-close>Отмена</button>
             <button type="button" class="btn btn-primary" data-save>${icon('check')}<span>Сохранить</span></button>`,
  });

  const form = modal.body.querySelector('form');
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());
  modal.footer.querySelector('[data-save]').addEventListener('click', async (event) => {
    clearFieldErrors(form);
    const button = event.currentTarget;
    const payload = {
      firstName: formValue(form, 'firstName'),
      lastName: formValue(form, 'lastName'),
      birthDate: formValue(form, 'birthDate'),
      gender: formValue(form, 'gender'),
      phone: formValue(form, 'phone'),
      email: formValue(form, 'email') || null,
      address: formValue(form, 'address'),
    };
    setButtonLoading(button, true, 'Сохраняем…');
    try {
      const updated = await api.me.updateProfile(payload);
      modal.close();
      toast('Профиль обновлён', 'success');
      onDone?.(updated);
    } catch (error) {
      showFormAlert(form, getErrorMessage(error));
      applyFieldErrors(form, error?.fieldErrors);
    } finally {
      setButtonLoading(button, false);
    }
  });
}

function detailItem(label, value, iconName) {
  return `
    <div class="detail-item">
      <span class="detail-label">${iconName ? `${icon(iconName)} ` : ''}${label}</span>
      <span class="detail-value">${value}</span>
    </div>
  `;
}

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      <section class="card" data-profile>${loadingState('Загружаем профиль…')}</section>
    </div>
  `;

  const container = root.querySelector('[data-profile]');

  async function load() {
    container.innerHTML = loadingState('Загружаем профиль…');
    try {
      const profile = await api.me.profile();
      container.innerHTML = `
        <div class="card-header">
          <div class="flex items-center gap-12">
            ${avatar(profile.fullName, 'avatar-lg')}
            <div>
              <div class="card-title">${esc(profile.fullName)}</div>
              <div class="card-subtitle">Личный кабинет пациента · ИИН ${esc(profile.iin)}</div>
            </div>
          </div>
          <button type="button" class="btn btn-secondary btn-icon" data-edit title="Редактировать профиль" aria-label="Редактировать профиль">${icon('edit')}</button>
        </div>
        <div class="card-body">
          <div class="detail-grid">
            ${detailItem('ИИН', `<strong>${esc(profile.iin)}</strong>`, 'shield')}
            ${detailItem('Дата рождения', `${formatDate(profile.birthDate)} · ${profile.age} ${profile.age % 10 === 1 && profile.age % 100 !== 11 ? 'год' : [2, 3, 4].includes(profile.age % 10) ? 'года' : 'лет'}`, 'calendar')}
            ${detailItem('Пол', esc(profile.genderLabel || genderLabel(profile.gender)), 'user')}
            ${detailItem('Телефон', `<a class="link-button" href="tel:${esc(profile.phone)}">${esc(profile.phone)}</a>`, 'phone')}
            ${detailItem('Email', profile.email ? `<a class="link-button" href="mailto:${esc(profile.email)}">${esc(profile.email)}</a>` : '<span class="muted">не указан</span>', 'mail')}
            ${detailItem('Адрес проживания', `<strong>${esc(profile.address)}</strong>`, 'map-pin')}
          </div>
          <div class="callout mt-16">
            ${icon('info')}
            <span>ИИН — уникальный идентификатор пациента в системе. Если данные нужно изменить, обратитесь в регистратуру.</span>
          </div>
        </div>
      `;
      container.querySelector('[data-edit]').addEventListener('click', () => {
        openProfileForm(profile, load);
      });
    } catch (error) {
      container.innerHTML = errorState(getErrorMessage(error));
      container.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  await load();
}

import { api, getErrorMessage } from '../api.js';
import { t, plural } from '../i18n.js';
import {
  icon, esc, toast, formatDate, genderLabel, openModal, loadingState, errorState, setButtonLoading,
  clearFieldErrors, showFormAlert, applyFieldErrors, formValue, todayISO, addressFields, readAddress,
} from '../ui.js';

export function openProfileForm(profile, onDone) {
  const id = `profile-form-${Date.now()}`;
  const modal = openModal({
    title: t('Редактировать профиль'),
    subtitle: t('Адрес и email обязательны: по ним оформляются справки и приходят квитанции'),
    size: 'lg',
    body: `
      <form id="${id}" class="modal-enter" novalidate>
        <div class="form-alert hidden" data-form-alert role="alert"></div>
        <div class="form-grid-3">
          <div class="field"><label for="${id}-lastName">${t('Фамилия *')}</label>
            <input class="input" id="${id}-lastName" name="lastName" maxlength="80" value="${esc(profile.lastName)}"></div>
          <div class="field"><label for="${id}-firstName">${t('Имя *')}</label>
            <input class="input" id="${id}-firstName" name="firstName" maxlength="80" value="${esc(profile.firstName)}"></div>
          <div class="field"><label for="${id}-middleName">${t('Отчество')}</label>
            <input class="input" id="${id}-middleName" name="middleName" maxlength="80" value="${esc(profile.middleName || '')}"></div>
        </div>
        <div class="form-grid">
          <div class="field"><label for="${id}-birthDate">${t('Дата рождения *')}</label>
            <input class="input" type="date" id="${id}-birthDate" name="birthDate" max="${todayISO()}" value="${esc(profile.birthDate)}"></div>
          <div class="field"><label for="${id}-gender">${t('Пол *')}</label>
            <select class="select" id="${id}-gender" name="gender">
              <option value="FEMALE" ${profile.gender === 'FEMALE' ? 'selected' : ''}>${t('Женский')}</option>
              <option value="MALE" ${profile.gender === 'MALE' ? 'selected' : ''}>${t('Мужской')}</option>
            </select></div>
          <div class="field"><label for="${id}-phone">${t('Телефон *')}</label>
            <input class="input" type="tel" id="${id}-phone" name="phone" maxlength="25" value="${esc(profile.phone)}"></div>
          <div class="field"><label for="${id}-email">Email *</label>
            <input class="input" type="email" id="${id}-email" name="email" maxlength="160" value="${esc(profile.email || '')}"></div>
        </div>
        <div class="form-section"><h4>${t('Адрес проживания')}</h4>${addressFields(id, profile)}</div>
      </form>`,
    footer: `<button type="button" class="btn btn-secondary" data-close>${t('Отмена')}</button>
             <button type="submit" form="${id}" class="btn btn-primary" data-save><span>${t('Сохранить')}</span></button>`,
  });

  const form = modal.body.querySelector('form');
  const save = modal.footer.querySelector('[data-save]');
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    setButtonLoading(save, true, t('Сохраняем…'));
    try {
      const updated = await api.me.updateProfile({
        firstName: formValue(form, 'firstName'),
        lastName: formValue(form, 'lastName'),
        middleName: formValue(form, 'middleName') || null,
        birthDate: formValue(form, 'birthDate') || null,
        gender: formValue(form, 'gender'),
        phone: formValue(form, 'phone'),
        email: formValue(form, 'email') || null,
        ...readAddress(form),
      });
      modal.close();
      toast(t('Профиль обновлён'), 'success');
      onDone?.(updated);
    } catch (error) {
      showFormAlert(form, getErrorMessage(error));
      applyFieldErrors(form, error?.fieldErrors);
    } finally {
      setButtonLoading(save, false);
    }
  });
}

function item(label, value) {
  return `<div class="detail-item"><span class="detail-label">${label}</span><span class="detail-value">${value}</span></div>`;
}

export async function render(root) {
  root.innerHTML = `<section class="card" data-profile>${loadingState(t('Загружаем профиль…'))}</section>`;
  const container = root.querySelector('[data-profile]');

  async function load() {
    try {
      const profile = await api.me.profile();
      const addressMissing = !profile.city || !profile.street || !profile.house;
      container.innerHTML = `
        <div class="card-header">
          <div>
            <div class="card-title">${esc(profile.fullName)}</div>
            <div class="card-subtitle">${t('ИИН {0}', [esc(profile.iin)])}</div>
          </div>
          <button type="button" class="btn btn-secondary" data-edit>${icon('edit')}<span>${t('Редактировать')}</span></button>
        </div>
        <div class="card-body stack">
          ${addressMissing ? `<div class="form-alert form-alert-info">${t('Заполните адрес: город, улицу и дом. Он нужен для медицинских справок.')}</div>` : ''}
          <div class="detail-grid">
            ${item(t('Дата рождения'), `${formatDate(profile.birthDate)}, ${profile.age} ${plural(profile.age, ['год', 'года', 'лет'])}`)}
            ${item(t('Пол'), esc(genderLabel(profile.gender)))}
            ${item(t('Телефон'), esc(profile.phone))}
            ${item('Email', profile.email ? esc(profile.email) : `<span class="muted">${t('не указан')}</span>`)}
          </div>
          <div class="form-section"><h4>${t('Адрес проживания')}</h4>
            <div class="detail-grid">
              ${addressMissing ? item(t('Адрес'), esc(profile.address)) : `
                ${item(t('Город, населённый пункт'), esc(profile.city))}
                ${item(t('Улица'), esc(profile.street))}
                ${item(t('Дом'), esc(profile.house))}
                ${item(t('Квартира'), esc(profile.apartment || '—'))}`}
            </div>
          </div>
          <div class="callout">${icon('info')}<span>${t('ИИН изменить нельзя. Если в нём ошибка, обратитесь в регистратуру.')}</span></div>
        </div>`;
      container.querySelector('[data-edit]').addEventListener('click', () => openProfileForm(profile, load));
    } catch (error) {
      container.innerHTML = errorState(getErrorMessage(error));
      container.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  await load();
}

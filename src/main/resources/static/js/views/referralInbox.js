import { api, getErrorMessage } from '../api.js';
import { t, plural } from '../i18n.js';
import {
  icon, esc, referralStatusBadge, paymentBadge, formatDate, formatDateTime, emptyState, errorState, loadingState,
  segmentedControl, toast, confirmDialog, openModal,
} from '../ui.js';

const state = { tab: 'PENDING' };

const ACTIONS = {
  ACCEPTED: () => [t('Принять направление?'), t('Направление перейдёт в работу, пациент увидит новый статус.'), 'check-circle'],
  COMPLETED: () => [t('Отметить выполненным?'), t('Направление будет закрыто как выполненное.'), 'check-circle'],
  CANCELLED: () => [t('Отменить направление?'), t('Пациент увидит, что направление отменено.'), 'ban'],
};

function target(referral) {
  return [referral.serviceName, referral.toDoctorName || referral.toSpecializationName].filter(Boolean).join(', ');
}

function row(referral, outgoing) {
  return `
    <tr>
      <td><span class="cell-main">${esc(referral.patientName)}</span><span class="cell-sub">${t('ИИН {0}', [esc(referral.patientIin)])}</span></td>
      <td>${outgoing
        ? `<span class="cell-main">${esc(target(referral))}</span><span class="cell-sub">${esc(referral.department || '')}${referral.office ? `, ${t('каб. {0}', [esc(referral.office)])}` : ''}</span>`
        : `<span class="cell-main">${esc(referral.fromDoctorName)}</span><span class="cell-sub">${esc(referral.fromSpecialization)}, ${t('каб. {0}', [esc(referral.fromOffice)])}</span>`}</td>
      <td class="muted-cell">${!outgoing && referral.serviceName ? `<span class="cell-main">${esc(referral.serviceName)}</span><br>` : ''}${esc(referral.reason || '—')}</td>
      <td class="nowrap">${formatDateTime(referral.createdAt)}</td>
      <td>${referralStatusBadge(referral.status)}</td>
      <td><div class="actions">
        <button type="button" class="icon-action" data-open="${referral.id}" title="${t('Открыть направление')}" aria-label="${t('Открыть направление')}">${icon('eye')}</button>
        ${!outgoing && referral.status === 'PENDING' ? `<button type="button" class="icon-action" data-status="ACCEPTED" data-id="${referral.id}" title="${t('Принять')}" aria-label="${t('Принять')}">${icon('check')}</button>` : ''}
        ${!outgoing && referral.status === 'ACCEPTED' ? `<button type="button" class="icon-action" data-status="COMPLETED" data-id="${referral.id}" title="${t('Выполнено')}" aria-label="${t('Выполнено')}">${icon('check-circle')}</button>` : ''}
        ${!outgoing && ['PENDING', 'ACCEPTED'].includes(referral.status) ? `<button type="button" class="icon-action danger" data-status="CANCELLED" data-id="${referral.id}" title="${t('Отменить')}" aria-label="${t('Отменить')}">${icon('ban')}</button>` : ''}
      </div></td>
    </tr>`;
}

function openReferral(referral) {
  const item = (label, value) => `<div class="detail-item"><span class="detail-label">${label}</span><span class="detail-value">${value}</span></div>`;
  const modal = openModal({
    title: t('Направление № {0}', [referral.id]),
    subtitle: formatDateTime(referral.createdAt),
    size: 'lg',
    body: `
      <div class="stack">
        <div class="flex gap-8 flex-wrap">${referralStatusBadge(referral.status)}${referral.price ? paymentBadge(referral.paymentStatus) : ''}</div>
        <div class="detail-grid">
          ${item(t('Пациент'), `<strong>${esc(referral.patientName)}</strong><br>${t('ИИН {0}', [esc(referral.patientIin)])}<br>${formatDate(referral.patientBirthDate)}, ${esc(referral.patientPhone)}`)}
          ${item(t('От кого'), `<strong>${esc(referral.fromDoctorName)}</strong><br>${esc(referral.fromSpecialization)}, ${t('каб. {0}', [esc(referral.fromOffice)])}`)}
          ${item(t('Куда и кому'), `<strong>${esc(target(referral))}</strong><br>${esc(referral.department || '')}${referral.office ? `, ${t('каб. {0}', [esc(referral.office)])}` : ''}`)}
          ${item(t('Выписано на приёме'), referral.visitStartTime ? formatDateTime(referral.visitStartTime) : '—')}
        </div>
        <div><span class="detail-label">${t('Причина и цель')}</span><div class="detail-note">${esc(referral.reason || '—')}</div></div>
        ${referral.visitDiagnosis ? `<div><span class="detail-label">${t('Диагноз направившего врача')}</span><div class="detail-note">${esc(referral.visitDiagnosis)}</div></div>` : ''}
        ${referral.visitConclusion ? `<div><span class="detail-label">${t('Заключение направившего врача')}</span><div class="detail-note">${esc(referral.visitConclusion)}</div></div>` : ''}
      </div>`,
    footer: `<button type="button" class="btn btn-secondary" data-close>${t('Закрыть')}</button>`,
  });
  modal.footer.querySelector('[data-close]').addEventListener('click', () => modal.close());
}

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      ${segmentedControl([
        { value: 'PENDING', label: t('Новые') },
        { value: 'ACCEPTED', label: t('В работе') },
        { value: 'COMPLETED', label: t('Выполненные') },
        { value: 'CANCELLED', label: t('Отменённые') },
        { value: 'OUTGOING', label: t('Выписанные мной') },
      ], state.tab)}
      <section class="card">
        <div class="card-header"><div class="card-title" data-count></div>
          <button type="button" class="btn btn-secondary btn-sm" data-refresh>${icon('refresh')}<span>${t('Обновить')}</span></button></div>
        <div class="table-wrap" data-table>${loadingState(t('Загружаем направления…'))}</div>
      </section>
    </div>`;
  const table = root.querySelector('[data-table]');
  const count = root.querySelector('[data-count]');

  async function changeStatus(id, status) {
    const [title, message, iconName] = ACTIONS[status]();
    if (!await confirmDialog({ title, message, confirmText: t('Подтвердить'), cancelText: t('Назад'), danger: status === 'CANCELLED', iconName })) {
      return;
    }
    try {
      await api.referrals.updateStatus(id, status);
      toast(t('Статус направления обновлён'), 'success');
      document.dispatchEvent(new CustomEvent('referrals:changed'));
      await load();
    } catch (error) {
      toast(getErrorMessage(error), 'error');
    }
  }

  async function load() {
    const outgoing = state.tab === 'OUTGOING';
    try {
      const referrals = outgoing ? await api.referrals.outgoing() : await api.referrals.incoming(state.tab);
      count.textContent = `${referrals.length} ${plural(referrals.length, ['направление', 'направления', 'направлений'])}`;
      table.innerHTML = referrals.length ? `
        <table class="table">
          <thead><tr><th>${t('Пациент')}</th><th>${outgoing ? t('Куда и кому') : t('От кого')}</th><th>${t('Причина и цель')}</th><th>${t('Создано')}</th><th>${t('Статус')}</th><th></th></tr></thead>
          <tbody>${referrals.map((referral) => row(referral, outgoing)).join('')}</tbody>
        </table>`
        : emptyState({ iconName: 'inbox', title: t('Направлений нет'), text: state.tab === 'PENDING' ? t('Новые направления от коллег появятся здесь сразу после создания.') : '' });
      table.querySelectorAll('[data-open]').forEach((button) => button.addEventListener('click', () => {
        openReferral(referrals.find((referral) => String(referral.id) === button.dataset.open));
      }));
      table.querySelectorAll('[data-status]').forEach((button) => {
        button.addEventListener('click', () => changeStatus(Number(button.dataset.id), button.dataset.status));
      });
    } catch (error) {
      table.innerHTML = errorState(getErrorMessage(error));
      table.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  root.querySelectorAll('[data-tab]').forEach((button) => button.addEventListener('click', () => {
    state.tab = button.dataset.tab;
    root.querySelectorAll('[data-tab]').forEach((other) => {
      other.classList.toggle('is-active', other === button);
      other.setAttribute('aria-selected', String(other === button));
    });
    load();
  }));
  root.querySelector('[data-refresh]').addEventListener('click', load);
  document.addEventListener('referrals:arrived', load);
  await load();
  return () => document.removeEventListener('referrals:arrived', load);
}

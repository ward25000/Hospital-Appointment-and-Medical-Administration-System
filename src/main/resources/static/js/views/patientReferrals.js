import { api, getErrorMessage } from '../api.js';
import { t, plural } from '../i18n.js';
import {
  esc, referralStatusBadge, paymentBadge, formatDateTime, formatMoney, emptyState, errorState, loadingState,
} from '../ui.js';

function row(referral) {
  const payable = referral.price && referral.paymentStatus === 'UNPAID' && referral.status !== 'CANCELLED';
  return `
    <tr>
      <td class="nowrap">${formatDateTime(referral.createdAt)}</td>
      <td><span class="cell-main">${esc(referral.fromDoctorName)}</span>
        <span class="cell-sub">${esc(referral.fromSpecialization || '')}, ${t('каб. {0}', [esc(referral.fromOffice)])}</span></td>
      <td><span class="cell-main">${esc(referral.serviceName || referral.toDoctorName || referral.toSpecializationName || '—')}</span>
        <span class="cell-sub">${esc([referral.serviceName ? referral.toDoctorName : '', referral.toSpecializationName].filter(Boolean).join(', '))}${referral.office ? `, ${t('каб. {0}', [esc(referral.office)])}` : ''}</span></td>
      <td class="muted-cell">${esc(referral.reason || '—')}</td>
      <td>${referralStatusBadge(referral.status)}</td>
      <td>${referral.price ? `${formatMoney(referral.price)}<div class="mt-8">${paymentBadge(referral.paymentStatus)}</div>` : '—'}
        ${payable ? `<div class="mt-8"><a class="link-button" href="#/payments?referral=${referral.id}">${t('Оплатить')}</a></div>` : ''}</td>
    </tr>`;
}

export async function render(root) {
  root.innerHTML = `
    <section class="card">
      <div class="card-header"><div class="card-title" data-count>${t('Мои направления')}</div></div>
      <div class="table-wrap" data-table>${loadingState(t('Загружаем направления…'))}</div>
    </section>`;
  const table = root.querySelector('[data-table]');

  async function load() {
    try {
      const referrals = await api.referrals.mine();
      root.querySelector('[data-count]').textContent = `${referrals.length} ${plural(referrals.length, ['направление', 'направления', 'направлений'])}`;
      table.innerHTML = referrals.length ? `
        <table class="table">
          <thead><tr><th>${t('Дата')}</th><th>${t('От кого')}</th><th>${t('Куда и кому')}</th><th>${t('Причина')}</th><th>${t('Статус')}</th><th>${t('Оплата')}</th></tr></thead>
          <tbody>${referrals.map(row).join('')}</tbody>
        </table>`
        : emptyState({ iconName: 'referrals', title: t('Направлений пока нет'), text: t('Врач выпишет направление во время приёма, и оно появится здесь.') });
    } catch (error) {
      table.innerHTML = errorState(getErrorMessage(error));
      table.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }
  await load();
}

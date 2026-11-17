import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  referralStatusBadge,
  formatDateTime,
  emptyState,
  errorState,
  loadingState,
} from '../ui.js';

function target(referral) {
  if (referral.toDoctorName) {
    return `${referral.toDoctorName}${referral.toSpecializationName ? ` · ${referral.toSpecializationName}` : ''}`;
  }
  return referral.toSpecializationName || '—';
}

function referralRow(referral) {
  return `
    <tr>
      <td>
        <span class="cell-main">${esc(referral.fromDoctorName)}</span>
        <span class="cell-sub">${esc(referral.fromSpecialization || '')}</span>
      </td>
      <td><span class="cell-main">${esc(target(referral))}</span></td>
      <td>
        <span class="cell-main">${esc(referral.department || '—')}</span>
        ${referral.office ? `<span class="cell-sub">каб. ${esc(referral.office)}</span>` : ''}
      </td>
      <td class="nowrap">${referral.visitStartTime ? formatDateTime(referral.visitStartTime) : '—'}</td>
      <td>${referralStatusBadge(referral.status, referral.statusLabel)}</td>
      <td class="muted-cell">${esc(referral.reason || '—')}</td>
    </tr>
  `;
}

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      <section class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Мои направления</div>
            <div class="card-subtitle" data-count>Загружаем направления…</div>
          </div>
          <span class="metric-inline">${icon('database')} <span>Личный кабинет</span></span>
        </div>
        <div class="table-wrap" data-table>${loadingState('Загружаем направления…')}</div>
      </section>
    </div>
  `;

  const table = root.querySelector('[data-table]');
  const count = root.querySelector('[data-count]');

  async function load() {
    table.innerHTML = loadingState('Загружаем направления…');
    try {
      const referrals = await api.referrals.mine();
      count.textContent = `${referrals.length} ${referrals.length === 1 ? 'направление' : 'направлений'}`;
      if (referrals.length === 0) {
        table.innerHTML = `
          <div class="table-empty">
            ${emptyState({
              iconName: 'referrals',
              title: 'Направлений пока нет',
              text: 'Врач выпишет направление во время приёма, и оно появится здесь.',
            })}
          </div>
        `;
        return;
      }
      table.innerHTML = `
        <table class="table">
          <thead>
            <tr>
              <th>От кого</th>
              <th>К кому</th>
              <th>Куда</th>
              <th>Дата приёма</th>
              <th>Статус</th>
              <th>Причина</th>
            </tr>
          </thead>
          <tbody>${referrals.map(referralRow).join('')}</tbody>
        </table>
      `;
    } catch (error) {
      table.innerHTML = errorState(getErrorMessage(error));
      table.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  await load();
}

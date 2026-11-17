import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  referralStatusBadge,
  formatDateTime,
  emptyState,
  errorState,
  loadingState,
  segmentedControl,
  toast,
  confirmDialog,
} from '../ui.js';

const state = { status: 'PENDING' };

function referralRow(referral) {
  const canAccept = referral.status === 'PENDING';
  const canComplete = referral.status === 'ACCEPTED';
  const canCancel = referral.status === 'PENDING' || referral.status === 'ACCEPTED';

  return `
    <tr>
      <td>
        <span class="cell-main">${esc(referral.patientName)}</span>
        <span class="cell-sub">приём № ${referral.appointmentId || '—'}</span>
      </td>
      <td>
        <span class="cell-main">${esc(referral.fromDoctorName)}</span>
        <span class="cell-sub">${esc(referral.fromSpecialization || '')}</span>
      </td>
      <td>
        <span class="cell-main">${esc(referral.department || '—')}</span>
        ${referral.office ? `<span class="cell-sub">каб. ${esc(referral.office)}</span>` : ''}
      </td>
      <td class="nowrap">${referral.createdAt ? formatDateTime(referral.createdAt) : '—'}</td>
      <td>${referralStatusBadge(referral.status, referral.statusLabel)}</td>
      <td class="muted-cell">${esc(referral.reason || '—')}</td>
      <td>
        <div class="actions">
          ${canAccept ? `<button type="button" class="icon-action" data-accept="${referral.id}" title="Принять направление" aria-label="Принять направление">${icon('check')}</button>` : ''}
          ${canComplete ? `<button type="button" class="icon-action" data-complete="${referral.id}" title="Отметить выполненным" aria-label="Отметить выполненным">${icon('check-circle')}</button>` : ''}
          ${canCancel ? `<button type="button" class="icon-action danger" data-cancel="${referral.id}" title="Отменить направление" aria-label="Отменить направление">${icon('ban')}</button>` : ''}
        </div>
      </td>
    </tr>
  `;
}

function renderTable(referrals) {
  if (referrals.length === 0) {
    return `
      <div class="table-empty">
        ${emptyState({
          iconName: 'inbox',
          title: 'Входящих направлений нет',
          text: state.status === 'PENDING'
            ? 'Новые направления от коллег появятся здесь сразу после создания.'
            : 'Для выбранного статуса направлений нет.',
        })}
      </div>
    `;
  }
  return `
    <table class="table">
      <thead>
        <tr>
          <th>Пациент</th>
          <th>От кого</th>
          <th>Куда</th>
          <th>Создано</th>
          <th>Статус</th>
          <th>Причина</th>
          <th><span class="sr-only">Действия</span></th>
        </tr>
      </thead>
      <tbody>${referrals.map((item) => referralRow(item, callbacks)).join('')}</tbody>
    </table>
  `;
}

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      <div class="flex items-center gap-12 flex-wrap">
        ${segmentedControl([
          { value: 'PENDING', label: 'Ожидают' },
          { value: 'ACCEPTED', label: 'Принятые' },
          { value: 'COMPLETED', label: 'Выполненные' },
          { value: 'CANCELLED', label: 'Отменённые' },
          { value: '', label: 'Все' },
        ], state.status)}
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" data-refresh title="Обновить" aria-label="Обновить">${icon('refresh')}</button>
      </div>
      <section class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Входящие направления</div>
            <div class="card-subtitle" data-count>Загружаем направления…</div>
          </div>
          <span class="metric-inline">${icon('database')} <span>Общая база направлений</span></span>
        </div>
        <div class="table-wrap" data-table>${loadingState('Загружаем направления…')}</div>
      </section>
    </div>
  `;

  const table = root.querySelector('[data-table]');
  const count = root.querySelector('[data-count]');

  async function changeStatus(id, status, title, message) {
    const confirmed = await confirmDialog({
      title,
      message,
      confirmText: 'Подтвердить',
      cancelText: 'Назад',
      danger: status === 'CANCELLED',
      iconName: status === 'CANCELLED' ? 'ban' : 'check-circle',
    });
    if (!confirmed) {
      return;
    }
    try {
      await api.referrals.updateStatus(id, status);
      toast('Статус направления обновлён', 'success');
      await load();
    } catch (error) {
      toast(getErrorMessage(error), 'error');
    }
  }

  async function load() {
    table.innerHTML = loadingState('Загружаем направления…');
    try {
      const referrals = await api.referrals.incoming(state.status || undefined);
      count.textContent = `${referrals.length} ${referrals.length === 1 ? 'направление' : 'направлений'}`;
      table.innerHTML = renderTable(referrals);
      table.querySelectorAll('[data-accept]').forEach((button) => {
        button.addEventListener('click', () => changeStatus(
          Number(button.dataset.accept), 'ACCEPTED', 'Принять направление?',
          'Направление перейдёт в работу и станет видно пациенту.',
        ));
      });
      table.querySelectorAll('[data-complete]').forEach((button) => {
        button.addEventListener('click', () => changeStatus(
          Number(button.dataset.complete), 'COMPLETED', 'Выполнить направление?',
          'Направление будет отмечено как выполненное.',
        ));
      });
      table.querySelectorAll('[data-cancel]').forEach((button) => {
        button.addEventListener('click', () => changeStatus(
          Number(button.dataset.cancel), 'CANCELLED', 'Отменить направление?',
          'Пациент увидит, что направление отменено.',
        ));
      });
    } catch (error) {
      table.innerHTML = errorState(getErrorMessage(error));
      table.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  root.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => {
      state.status = button.dataset.tab;
      root.querySelectorAll('[data-tab]').forEach((item) => {
        item.classList.toggle('is-active', item.dataset.tab === state.status);
        item.setAttribute('aria-selected', String(item.dataset.tab === state.status));
      });
      load();
    });
  });
  root.querySelector('[data-refresh]').addEventListener('click', load);

  await load();
}

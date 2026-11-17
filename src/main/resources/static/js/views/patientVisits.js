import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  statusBadge,
  formatDate,
  formatDateLong,
  formatTime,
  formatMoney,
  emptyState,
  errorState,
  loadingState,
  segmentedControl,
} from '../ui.js';

const state = { period: 'all' };

function visitRow(visit) {
  return `
    <tr>
      <td>
        <span class="cell-main">${formatDateLong(visit.startTime)}</span>
        <span class="cell-sub">${formatTime(visit.startTime)}–${formatTime(visit.endTime)}</span>
      </td>
      <td>
        <span class="cell-main">${esc(visit.doctorName)}</span>
        <span class="cell-sub">${visit.doctorOffice ? `каб. ${esc(visit.doctorOffice)}` : 'кабинет не указан'}</span>
      </td>
      <td>
        <span class="cell-main">${esc(visit.doctorSpecialization)}</span>
        <span class="cell-sub">${esc(visit.serviceName)}</span>
      </td>
      <td>${statusBadge(visit.status, visit.statusLabel)}</td>
      <td class="num">${formatMoney(visit.price)}</td>
    </tr>
  `;
}

function renderTable(visits) {
  if (visits.length === 0) {
    return `
      <div class="table-empty">
        ${emptyState({
          iconName: 'visits',
          title: 'Визитов пока нет',
          text: 'Здесь появятся ваши предстоящие и прошедшие приёмы.',
        })}
      </div>
    `;
  }
  return `
    <table class="table">
      <thead>
        <tr>
          <th>Дата и время</th>
          <th>Врач</th>
          <th>Специальность / услуга</th>
          <th>Статус</th>
          <th class="num">Стоимость</th>
        </tr>
      </thead>
      <tbody>${visits.map(visitRow).join('')}</tbody>
    </table>
  `;
}

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      <div class="flex items-center gap-12 flex-wrap">
        ${segmentedControl([
          { value: 'all', label: 'Все визиты' },
          { value: 'upcoming', label: 'Предстоящие' },
          { value: 'past', label: 'Прошедшие' },
        ], state.period)}
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary btn-icon" data-refresh title="Обновить" aria-label="Обновить">${icon('refresh')}</button>
      </div>
      <section class="card">
        <div class="card-header">
          <div>
            <div class="card-title">История посещений</div>
            <div class="card-subtitle" data-count>Загружаем визиты…</div>
          </div>
          <span class="metric-inline">${icon('database')} <span>Личный кабинет</span></span>
        </div>
        <div class="table-wrap" data-table>${loadingState('Загружаем визиты…')}</div>
      </section>
    </div>
  `;

  const table = root.querySelector('[data-table]');
  const count = root.querySelector('[data-count]');

  async function load() {
    table.innerHTML = loadingState('Загружаем визиты…');
    try {
      const visits = await api.me.visits();
      const now = Date.now();
      const filtered = visits.filter((visit) => {
        if (state.period === 'upcoming') {
          return new Date(visit.startTime).getTime() >= now;
        }
        if (state.period === 'past') {
          return new Date(visit.startTime).getTime() < now;
        }
        return true;
      });
      count.textContent = `${filtered.length} ${filtered.length === 1 ? 'визит' : 'визитов'} в выбранном периоде`;
      table.innerHTML = renderTable(filtered);
    } catch (error) {
      table.innerHTML = errorState(getErrorMessage(error));
      table.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  root.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => {
      state.period = button.dataset.tab;
      root.querySelectorAll('[data-tab]').forEach((item) => {
        item.classList.toggle('is-active', item.dataset.tab === state.period);
        item.setAttribute('aria-selected', String(item.dataset.tab === state.period));
      });
      load();
    });
  });
  root.querySelector('[data-refresh]').addEventListener('click', load);

  await load();
}

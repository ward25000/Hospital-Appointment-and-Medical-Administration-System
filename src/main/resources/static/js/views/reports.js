import { api, getErrorMessage } from '../api.js';
import {
  icon,
  esc,
  formatDateLong,
  formatDate,
  formatMoney,
  formatNumber,
  todayISO,
  toISODate,
  openModal,
  toast,
  emptyState,
  errorState,
  loadingState,
  csvDownload,
  pluralRu,
} from '../ui.js';

function firstOfMonthISO() {
  const now = new Date();
  return toISODate(new Date(now.getFullYear(), now.getMonth(), 1));
}

function previousMonthRange() {
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const last = new Date(now.getFullYear(), now.getMonth(), 0);
  return { from: toISODate(first), to: toISODate(last) };
}

const state = {
  from: firstOfMonthISO(),
  to: todayISO(),
};

let lastReport = null;

function kpi(label, value, foot, tone = '') {
  return `
    <article class="card report-kpi">
      <span>${esc(label)}</span>
      <strong ${tone ? `style="color:${tone}"` : ''}>${esc(value)}</strong>
      <small>${foot}</small>
    </article>
  `;
}

function renderReport(report) {
  lastReport = report;
  const summary = report.summary;
  const daily = report.daily || [];
  const maxTotal = Math.max(...daily.map((day) => day.total), 0);
  const hasData = daily.some((day) => day.total > 0);

  return `
    <div class="stack">
      <section class="report-kpis">
        ${kpi('Всего приёмов', formatNumber(summary.totalAppointments), `${formatNumber(summary.scheduled)} запланировано`)}
        ${kpi('Завершено', formatNumber(summary.completed), `${formatNumber(summary.cancelled)} отменено`, 'var(--success)')}
        ${kpi('Уникальных пациентов', formatNumber(summary.uniquePatients), 'за выбранный период')}
        ${kpi('Выручка', formatMoney(summary.revenue), 'по завершённым приёмам', 'var(--primary-strong)')}
        ${kpi('Средний чек', formatMoney(summary.averageCheck), 'на один завершённый приём')}
      </section>

      <section class="card chart-card">
        <div class="chart-head">
          <div>
            <div class="card-title">Динамика приёмов по дням</div>
            <div class="card-subtitle">${formatDateLong(report.from)} — ${formatDateLong(report.to)}</div>
          </div>
          <div class="chart-legend">
            <span class="legend-item"><span class="legend-dot"></span> Все приёмы</span>
            <span class="legend-item"><span class="legend-dot completed"></span> Завершённые</span>
            <span class="legend-item"><span class="legend-dot cancelled"></span> Есть отмены</span>
          </div>
        </div>
        ${hasData
          ? `<div class="chart-scroll">
              <div class="chart-bars">
                ${daily.map((day) => {
                  const height = day.total === 0 ? 2 : Math.max(5, Math.round((day.total / maxTotal) * 150));
                  const completedShare = day.total === 0 ? 0 : Math.round((day.completed / day.total) * 100);
                  return `
                    <div class="chart-col ${day.cancelled > 0 ? 'has-cancelled' : ''}"
                         title="${esc(formatDate(day.date))}: ${day.total} приёмов, завершено ${day.completed}, отменено ${day.cancelled}, выручка ${formatMoney(day.revenue)}">
                      <div class="chart-bar" style="height:${height}px">
                        ${day.completed > 0 ? `<div class="chart-bar-fill" style="height:${completedShare}%"></div>` : ''}
                      </div>
                      <span class="chart-label">${day.date.slice(8, 10)}</span>
                    </div>
                  `;
                }).join('')}
              </div>
            </div>`
          : '<div class="chart-empty">За выбранный период приёмов нет</div>'}
      </section>

      <div class="report-tables">
        <section class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Нагрузка врачей</div>
              <div class="card-subtitle">Количество приёмов и выручка по специалистам</div>
            </div>
          </div>
          ${report.byDoctor.length
            ? `<div class="table-wrap"><table class="table">
                <thead><tr><th>Врач</th><th class="num">Всего</th><th class="num">Завершено</th><th class="num">Отменено</th><th class="num">Выручка</th></tr></thead>
                <tbody>
                  ${report.byDoctor.map((doctor) => `
                    <tr>
                      <td>
                        <span class="cell-main">${esc(doctor.doctorName)}</span>
                        <span class="cell-sub">${esc(doctor.specialization)}</span>
                      </td>
                      <td class="num">${formatNumber(doctor.total)}</td>
                      <td class="num">${formatNumber(doctor.completed)}</td>
                      <td class="num">${formatNumber(doctor.cancelled)}</td>
                      <td class="num revenue-cell">${formatMoney(doctor.revenue)}</td>
                    </tr>
                  `).join('')}
                </tbody>
              </table></div>`
            : emptyState({ iconName: 'doctors', title: 'Нет данных по врачам', text: 'За выбранный период приёмов не было.' })}
        </section>

        <section class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Выручка по услугам</div>
              <div class="card-subtitle">Завершённые приёмы и стоимость услуг</div>
            </div>
          </div>
          ${report.byService.length
            ? `<div class="table-wrap"><table class="table">
                <thead><tr><th>Услуга</th><th class="num">Всего</th><th class="num">Завершено</th><th class="num">Выручка</th></tr></thead>
                <tbody>
                  ${report.byService.map((service) => `
                    <tr>
                      <td><span class="cell-main">${esc(service.serviceName)}</span></td>
                      <td class="num">${formatNumber(service.total)}</td>
                      <td class="num">${formatNumber(service.completed)}</td>
                      <td class="num revenue-cell">${formatMoney(service.revenue)}</td>
                    </tr>
                  `).join('')}
                </tbody>
              </table></div>`
            : emptyState({ iconName: 'catalog', title: 'Нет данных по услугам', text: 'За выбранный период завершённых приёмов не было.' })}
        </section>
      </div>
    </div>
  `;
}

export async function render(root, ctx) {
  root.innerHTML = `
    <div class="stack">
      <section class="card report-filter-card">
        <div class="field">
          <label for="report-from">Начало периода</label>
          <input class="input" type="date" id="report-from" value="${esc(state.from)}" max="${todayISO()}">
        </div>
        <div class="field">
          <label for="report-to">Конец периода</label>
          <input class="input" type="date" id="report-to" value="${esc(state.to)}" max="${todayISO()}">
        </div>
        <div class="flex gap-8 items-center">
          <button type="button" class="btn btn-secondary btn-sm" data-range="month">Текущий месяц</button>
          <button type="button" class="btn btn-secondary btn-sm" data-range="previous">Прошлый месяц</button>
          <button type="button" class="btn btn-secondary btn-sm" data-range="30">30 дней</button>
        </div>
        <div class="spacer"></div>
        <button type="button" class="btn btn-secondary" id="report-export" disabled>${icon('download')}<span>CSV</span></button>
        <button type="button" class="btn btn-primary" id="report-build">${icon('filter')}<span>Сформировать</span></button>
      </section>

      <div data-report>${loadingState('Формируем отчёт…')}</div>
    </div>
  `;

  const container = root.querySelector('[data-report]');
  const fromInput = root.querySelector('#report-from');
  const toInput = root.querySelector('#report-to');
  const exportButton = root.querySelector('#report-export');

  const load = async () => {
    if (!fromInput.value || !toInput.value) {
      toast('Укажите начало и конец периода', 'error');
      return;
    }
    if (toInput.value < fromInput.value) {
      toast('Дата окончания не может быть раньше даты начала', 'error');
      return;
    }
    state.from = fromInput.value;
    state.to = toInput.value;
    container.innerHTML = loadingState('Формируем отчёт…');
    try {
      const report = await api.reports.get({ from: state.from, to: state.to });
      container.innerHTML = renderReport(report);
      exportButton.disabled = false;
    } catch (error) {
      container.innerHTML = errorState(getErrorMessage(error));
      container.querySelector('[data-retry]')?.addEventListener('click', load);
      exportButton.disabled = true;
    }
  };

  root.querySelector('#report-build').addEventListener('click', load);
  fromInput.addEventListener('change', () => {
    state.from = fromInput.value;
  });
  toInput.addEventListener('change', () => {
    state.to = toInput.value;
  });

  root.querySelectorAll('[data-range]').forEach((button) => {
    button.addEventListener('click', () => {
      if (button.dataset.range === 'month') {
        state.from = firstOfMonthISO();
        state.to = todayISO();
      } else if (button.dataset.range === 'previous') {
        const range = previousMonthRange();
        state.from = range.from;
        state.to = range.to;
      } else {
        state.from = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10);
        state.to = todayISO();
      }
      fromInput.value = state.from;
      toInput.value = state.to;
      load();
    });
  });

  exportButton.addEventListener('click', () => {
    if (!lastReport) {
      return;
    }
    const report = lastReport;
    const rows = [
      ['MedFlow — отчёт по приёмам', `${formatDate(report.from)} — ${formatDate(report.to)}`],
      [],
      ['Показатель', 'Значение'],
      ['Всего приёмов', report.summary.totalAppointments],
      ['Запланировано', report.summary.scheduled],
      ['Завершено', report.summary.completed],
      ['Отменено', report.summary.cancelled],
      ['Уникальных пациентов', report.summary.uniquePatients],
      ['Выручка, ₽', report.summary.revenue],
      ['Средний чек, ₽', report.summary.averageCheck],
      [],
      ['Врач', 'Специализация', 'Всего', 'Завершено', 'Отменено', 'Выручка, ₽'],
      ...report.byDoctor.map((doctor) => [
        doctor.doctorName,
        doctor.specialization,
        doctor.total,
        doctor.completed,
        doctor.cancelled,
        doctor.revenue,
      ]),
      [],
      ['Услуга', 'Всего', 'Завершено', 'Выручка, ₽'],
      ...report.byService.map((service) => [
        service.serviceName,
        service.total,
        service.completed,
        service.revenue,
      ]),
    ];
    csvDownload(`medflow-report-${report.from}-${report.to}.csv`, rows);
    toast('Отчёт выгружен в CSV', 'success');
  });

  await load();
}

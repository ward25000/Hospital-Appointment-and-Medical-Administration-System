import { api, getErrorMessage } from '../api.js';
import { t, plural } from '../i18n.js';
import {
  icon, esc, statusBadge, paymentBadge, formatDateLong, formatTime, formatMoney, emptyState, errorState,
  loadingState, segmentedControl, setButtonLoading, toast,
} from '../ui.js';

const state = { period: 'all' };

function visitCard(visit, referrals) {
  const done = visit.status === 'COMPLETED';
  const payable = visit.paymentStatus === 'UNPAID' && ['SCHEDULED', 'COMPLETED'].includes(visit.status);
  return `
    <article class="visit">
      <div class="visit-date">
        <strong>${formatDateLong(visit.startTime)}</strong>
        <span>${formatTime(visit.startTime)}–${formatTime(visit.endTime)}</span>
      </div>
      <div class="visit-body">
        <strong>${esc(visit.doctorName)}</strong>
        <p>${esc(visit.doctorSpecialization)}, ${t('каб. {0}', [esc(visit.doctorOffice)])}</p>
        <p>${esc(visit.serviceName)}, ${formatMoney(visit.price)}</p>
        ${done ? `
          <div class="visit-result">
            <div><b>${t('Диагноз')}</b><p>${esc(visit.diagnosis || '—')}</p></div>
            <div><b>${t('Заключение врача')}</b><p>${esc(visit.conclusion || '—')}</p></div>
            ${visit.recommendations ? `<div><b>${t('Рекомендации')}</b><p>${esc(visit.recommendations)}</p></div>` : ''}
          </div>` : ''}
        ${referrals.length ? `
          <div class="visit-result">
            <div><b>${t('Выданные направления')}</b>
              <ul class="visit-referrals">${referrals.map((referral) => `
                <li>${esc([referral.serviceName, referral.toDoctorName || referral.toSpecializationName].filter(Boolean).join(', '))}${referral.office ? `, ${t('каб. {0}', [esc(referral.office)])}` : ''}: ${esc(referral.reason || '—')}</li>`).join('')}
              </ul>
            </div>
          </div>` : ''}
        ${visit.cancellationReason ? `<p>${t('Причина отмены')}: ${esc(visit.cancellationReason)}</p>` : ''}
      </div>
      <div class="visit-side">
        ${statusBadge(visit.status)}
        ${['SCHEDULED', 'COMPLETED'].includes(visit.status) || visit.paymentStatus === 'PAID' ? paymentBadge(visit.paymentStatus) : ''}
        ${done ? `<button type="button" class="btn btn-secondary btn-sm" data-certificate="${visit.id}">${icon('download')}<span>${t('Скачать справку')}</span></button>` : ''}
        ${payable ? `<a class="btn btn-primary btn-sm" href="#/payments?visit=${visit.id}">${t('Оплатить')}</a>` : ''}
      </div>
    </article>`;
}

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      ${segmentedControl([
        { value: 'all', label: t('Все визиты') },
        { value: 'upcoming', label: t('Предстоящие') },
        { value: 'past', label: t('Прошедшие') },
      ], state.period)}
      <p class="muted" data-count></p>
      <section class="card" data-list>${loadingState(t('Загружаем визиты…'))}</section>
    </div>`;
  const list = root.querySelector('[data-list]');
  const count = root.querySelector('[data-count]');
  let visits = [];
  let referrals = [];

  function draw() {
    const now = Date.now();
    const shown = visits.filter((visit) => {
      const start = new Date(visit.startTime).getTime();
      if (state.period === 'upcoming') return start >= now;
      return state.period === 'past' ? start < now : true;
    });
    count.textContent = `${shown.length} ${plural(shown.length, ['визит', 'визита', 'визитов'])}`;
    list.innerHTML = shown.length
      ? shown.map((visit) => visitCard(visit, referrals.filter((item) => item.appointmentId === visit.id))).join('')
      : emptyState({ iconName: 'visits', title: t('Визитов пока нет'), text: t('Здесь появятся ваши предстоящие и прошедшие приёмы.') });
    list.querySelectorAll('[data-certificate]').forEach((button) => {
      button.addEventListener('click', async () => {
        setButtonLoading(button, true, t('Готовим…'));
        try {
          await api.appointments.certificate(button.dataset.certificate);
        } catch (error) {
          toast(getErrorMessage(error), 'error');
        } finally {
          setButtonLoading(button, false);
        }
      });
    });
  }

  async function load() {
    try {
      [visits, referrals] = await Promise.all([api.me.visits(), api.referrals.mine()]);
      visits.sort((left, right) => right.startTime.localeCompare(left.startTime));
      draw();
    } catch (error) {
      list.innerHTML = errorState(getErrorMessage(error));
      list.querySelector('[data-retry]')?.addEventListener('click', load);
    }
  }

  root.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => {
      state.period = button.dataset.tab;
      root.querySelectorAll('[data-tab]').forEach((other) => {
        other.classList.toggle('is-active', other === button);
        other.setAttribute('aria-selected', String(other === button));
      });
      draw();
    });
  });
  await load();
}

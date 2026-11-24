import { api, getErrorMessage } from '../api.js';
import { t } from '../i18n.js';
import {
  esc, formatMoney, emptyState, errorState, loadingState, setButtonLoading, toast, debounce, todayISO, addDays,
} from '../ui.js';
import { payableRow, selectedItems, paymentsTable, bindPaymentActions } from './patientPayments.js';

const state = { query: '', patient: null };

export async function render(root) {
  root.innerHTML = `
    <div class="stack">
      <div class="pay-layout">
        <section class="card">
          <div class="card-header"><div><div class="card-title">${t('Приём оплаты')}</div>
            <div class="card-subtitle">${t('Найдите пациента по ИИН, фамилии или телефону')}</div></div></div>
          <div class="card-pad">
            <label class="sr-only" for="cash-search">${t('Поиск пациента')}</label>
            <input class="input" type="search" id="cash-search" placeholder="${t('ИИН, фамилия или телефон')}" value="${esc(state.query)}" autocomplete="off">
          </div>
          <div data-result></div>
        </section>
        <section class="card card-pad" data-side>${emptyState({ iconName: 'wallet', title: t('Выберите пациента') })}</section>
      </div>
      <section class="card">
        <div class="card-header"><div><div class="card-title">${t('Последние платежи')}</div>
          <div class="card-subtitle">${t('За 30 дней, онлайн и через кассу')}</div></div></div>
        <div class="table-wrap" data-payments>${loadingState(t('Загружаем платежи…'))}</div>
      </section>
    </div>`;

  const result = root.querySelector('[data-result]');
  const side = root.querySelector('[data-side]');
  const list = root.querySelector('[data-payments]');
  const search = root.querySelector('#cash-search');
  let sequence = 0;

  async function loadPayments(attempt = 0) {
    try {
      const payments = await api.payments.list({ from: addDays(todayISO(), -30), to: todayISO() });
      if (!list.isConnected) return;
      list.innerHTML = payments.length ? paymentsTable(payments, true) : emptyState({ iconName: 'wallet', title: t('Платежей пока нет') });
      bindPaymentActions(list, api.payments, () => loadPayments());
      if (attempt < 15 && payments.some((payment) => payment.emailStatus === 'PENDING')) {
        window.setTimeout(() => loadPayments(attempt + 1), 2000);
      }
    } catch (error) {
      list.innerHTML = errorState(getErrorMessage(error));
      list.querySelector('[data-retry]')?.addEventListener('click', () => loadPayments());
    }
  }

  function clearSide() {
    side.innerHTML = emptyState({ iconName: 'wallet', title: t('Выберите пациента') });
  }

  async function find() {
    const current = ++sequence;
    clearSide();
    if (state.query.length < 2) {
      result.innerHTML = '';
      return;
    }
    try {
      const patients = await api.patients.list(state.query);
      if (current !== sequence) return;
      result.innerHTML = patients.length
        ? patients.slice(0, 8).map((patient) => `
          <button type="button" class="quick-action" style="width:100%" data-patient="${patient.id}">
            <span></span><span><strong>${esc(patient.fullName)}</strong><small>${t('ИИН {0}', [esc(patient.iin)])}, ${esc(patient.phone)}</small></span>
          </button>`).join('')
        : emptyState({ iconName: 'search', title: t('Пациенты не найдены') });
      result.querySelectorAll('[data-patient]').forEach((button) => button.addEventListener('click', () => {
        state.patient = patients.find((patient) => String(patient.id) === button.dataset.patient);
        openPatient();
      }));
    } catch (error) {
      result.innerHTML = errorState(getErrorMessage(error), { retry: false });
    }
  }

  async function openPatient() {
    const current = ++sequence;
    const patient = state.patient;
    result.innerHTML = loadingState(t('Загружаем счета…'));
    let payables;
    try {
      payables = await api.payments.payables(patient.id);
    } catch (error) {
      result.innerHTML = errorState(getErrorMessage(error), { retry: false });
      return;
    }
    if (current !== sequence) return;
    result.innerHTML = `
      <div class="card-header" style="border-top:1px solid var(--border)"><div><div class="card-title">${esc(patient.fullName)}</div>
        <div class="card-subtitle">${t('ИИН {0}', [esc(patient.iin)])}</div></div></div>
      ${payables.length ? payables.map((item) => payableRow(item, true)).join('') : emptyState({ iconName: 'check-circle', title: t('Неоплаченных услуг нет') })}`;
    if (!payables.length) {
      clearSide();
      return;
    }
    side.innerHTML = `
      <div class="pay-total"><span>${t('Итого')}</span><strong data-total></strong></div>
      <div class="pay-form">
        <div class="field"><span class="field-label">${t('Способ оплаты')}</span>
          <label class="radio-label"><input type="radio" name="method" value="CASH" checked> ${t('Наличные')}</label>
          <label class="radio-label"><input type="radio" name="method" value="POS_CARD"> ${t('Карта через терминал')}</label></div>
        <button type="button" class="btn btn-primary btn-block btn-lg" data-pay><span>${t('Принять оплату')}</span></button>
        <p class="muted" style="font-size:14px">${patient.email ? t('Квитанция придёт на {0}', [esc(patient.email)]) : t('У пациента не указан email, квитанцию можно скачать')}</p>
      </div>`;
    const button = side.querySelector('[data-pay]');
    const refreshTotal = () => {
      const sum = selectedItems(result, payables).reduce((value, item) => value + Number(item.amount), 0);
      side.querySelector('[data-total]').textContent = formatMoney(sum);
      button.disabled = sum <= 0;
    };
    result.querySelectorAll('.pay-row input').forEach((input) => input.addEventListener('change', refreshTotal));
    refreshTotal();
    button.addEventListener('click', async () => {
      setButtonLoading(button, true, t('Проводим платёж…'));
      try {
        const payment = await api.payments.cashDesk({
          patientId: patient.id,
          method: side.querySelector('[name="method"]:checked').value,
          items: selectedItems(result, payables).map(({ type, id }) => ({ type, id })),
        });
        toast(t('Платёж № {0} на сумму {1}', [payment.number, formatMoney(payment.amount)]), 'success', t('Оплачено'));
        openPatient();
        loadPayments();
      } catch (error) {
        toast(getErrorMessage(error), 'error');
        setButtonLoading(button, false);
      }
    });
  }

  search.addEventListener('input', debounce(() => {
    state.query = search.value.trim();
    state.patient = null;
    find();
  }, 300));

  loadPayments();
  if (state.patient) openPatient();
  else if (state.query) find();
}

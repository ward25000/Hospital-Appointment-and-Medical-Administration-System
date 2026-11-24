import { api, getErrorMessage } from '../api.js';
import { t, plural } from '../i18n.js';
import {
  icon, esc, formatDateTime, formatMoney, emptyState, errorState, loadingState, setButtonLoading, toast,
  clearFieldErrors, showFormAlert, applyFieldErrors, formValue, emailStatusText,
} from '../ui.js';

const keyOf = (item) => `${item.type}-${item.id}`;

export function payableRow(item, checked) {
  return `
    <label class="pay-row">
      <input class="checkbox" type="checkbox" value="${keyOf(item)}" ${checked ? 'checked' : ''}>
      <span><strong>${esc(item.serviceName)}</strong>
        <small>${item.type === 'REFERRAL' ? t('Направление') : t('Приём')}: ${esc(item.doctorName)}, ${formatDateTime(item.date)}</small></span>
      <span class="amount">${formatMoney(item.amount)}</span>
    </label>`;
}

export function selectedItems(root, payables) {
  const keys = new Set([...root.querySelectorAll('.pay-row input:checked')].map((input) => input.value));
  return payables.filter((item) => keys.has(keyOf(item)));
}

export function paymentRow(payment, withPatient = false) {
  return `
    <tr>
      <td class="nowrap"><span class="cell-main">${esc(payment.number)}</span><span class="cell-sub">${formatDateTime(payment.paidAt)}</span></td>
      ${withPatient ? `<td><span class="cell-main">${esc(payment.patientName)}</span><span class="cell-sub">${esc(payment.patientIin)}</span></td>` : ''}
      <td>${payment.items.map((item) => esc(item.serviceName)).join('<br>')}</td>
      <td class="num revenue-cell">${formatMoney(payment.amount)}</td>
      <td>${esc(payment.methodLabel)}${payment.cardLast4 ? ` <span class="nowrap">•••• ${esc(payment.cardLast4)}</span>` : ''}</td>
      <td><span class="cell-sub">${esc(payment.email || '')}</span>${emailStatusText(payment.emailStatus)}</td>
      <td><div class="actions">
        <button type="button" class="icon-action" data-receipt="${payment.id}" title="${t('Скачать квитанцию')}" aria-label="${t('Скачать квитанцию')}">${icon('download')}</button>
        <button type="button" class="icon-action" data-resend="${payment.id}" title="${t('Отправить квитанцию ещё раз')}" aria-label="${t('Отправить квитанцию ещё раз')}">${icon('mail')}</button>
      </div></td>
    </tr>`;
}

export function paymentsTable(payments, withPatient = false) {
  return `
    <table class="table">
      <thead><tr><th>${t('Платёж')}</th>${withPatient ? `<th>${t('Пациент')}</th>` : ''}<th>${t('Услуги')}</th><th class="num">${t('Сумма')}</th><th>${t('Способ оплаты')}</th><th>${t('Квитанция')}</th><th></th></tr></thead>
      <tbody>${payments.map((payment) => paymentRow(payment, withPatient)).join('')}</tbody>
    </table>`;
}

export function bindPaymentActions(root, source, reload) {
  root.querySelectorAll('[data-receipt]').forEach((button) => button.addEventListener('click', async () => {
    try {
      await source.receipt(button.dataset.receipt);
    } catch (error) {
      toast(getErrorMessage(error), 'error');
    }
  }));
  root.querySelectorAll('[data-resend]').forEach((button) => button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      const payment = await source.resend(button.dataset.resend);
      toast(payment.email, 'success', t('Отправляем квитанцию'));
      reload();
    } catch (error) {
      toast(getErrorMessage(error), 'error');
      button.disabled = false;
    }
  }));
}

/** Refreshes only the payment table until the receipt is delivered, so the card form stays intact. */
async function followDelivery(container, attempt = 0) {
  if (attempt > 15 || !container.isConnected) return;
  try {
    const payments = await api.me.payments();
    if (!container.isConnected) return;
    container.innerHTML = paymentsTable(payments);
    bindPaymentActions(container, api.me, () => followDelivery(container));
    if (payments.some((payment) => payment.emailStatus === 'PENDING')) {
      window.setTimeout(() => followDelivery(container, attempt + 1), 2000);
    }
  } catch {
    // the status will be shown on the next visit to the page
  }
}

export async function render(root, ctx) {
  root.innerHTML = loadingState(t('Загружаем счета…'));
  let payables;
  let payments;
  let profile;
  try {
    [payables, payments, profile] = await Promise.all([api.me.payables(), api.me.payments(), api.me.profile()]);
  } catch (error) {
    root.innerHTML = errorState(getErrorMessage(error));
    root.querySelector('[data-retry]')?.addEventListener('click', () => render(root, ctx));
    return;
  }

  const visit = Number(ctx.params.get('visit'));
  const referral = Number(ctx.params.get('referral'));
  const preselected = (item) => (!visit && !referral)
    || (item.type === 'APPOINTMENT' && item.id === visit) || (item.type === 'REFERRAL' && item.id === referral);

  root.innerHTML = `
    <div class="stack">
      <div class="pay-layout">
        <section class="card">
          <div class="card-header"><div><div class="card-title">${t('К оплате')}</div>
            <div class="card-subtitle">${t('Отметьте услуги, за которые хотите заплатить')}</div></div></div>
          <div data-payables>${payables.length
            ? payables.map((item) => payableRow(item, preselected(item))).join('')
            : emptyState({ iconName: 'check-circle', title: t('Неоплаченных услуг нет') })}</div>
        </section>
        ${payables.length ? `
        <section class="card card-pad">
          <div class="pay-total"><span>${t('Итого')}</span><strong data-total></strong></div>
          <form class="pay-form" data-card-form novalidate autocomplete="off">
            <div class="form-alert hidden" data-form-alert role="alert"></div>
            <div class="field"><label for="card-number">${t('Номер карты')}</label>
              <input class="input" id="card-number" name="card.number" inputmode="numeric" maxlength="19" placeholder="0000 0000 0000 0000"></div>
            <div class="form-grid" style="grid-template-columns:1fr 1fr">
              <div class="field"><label for="card-expiry">${t('Срок, ММ/ГГ')}</label>
                <input class="input" id="card-expiry" name="card.expiry" inputmode="numeric" maxlength="5"></div>
              <div class="field"><label for="card-cvc">CVC</label>
                <input class="input" id="card-cvc" name="card.cvc" type="password" inputmode="numeric" maxlength="3"></div>
            </div>
            <button type="submit" class="btn btn-primary btn-block btn-lg" data-pay><span>${t('Оплатить')}</span></button>
          </form>
          <p class="muted mt-12" style="font-size:14px">${profile.email
            ? t('Квитанция придёт на {0}', [esc(profile.email)])
            : t('Укажите email в профиле, чтобы получить квитанцию')}</p>
          <div class="callout mt-12">${icon('info')}<span>${t('Шлюз работает в тестовом режиме: деньги не списываются, настоящие карты не принимаются. Тестовая карта: 4242 4242 4242 4242, любой будущий срок и любой CVC.')}</span></div>
        </section>` : ''}
      </div>
      <section class="card">
        <div class="card-header"><div><div class="card-title">${t('История платежей')}</div>
          <div class="card-subtitle">${payments.length} ${plural(payments.length, ['платёж', 'платежа', 'платежей'])}</div></div></div>
        <div class="table-wrap" data-payments>${payments.length
          ? paymentsTable(payments)
          : emptyState({ iconName: 'wallet', title: t('Платежей пока нет') })}</div>
      </section>
    </div>`;

  const reload = () => {
    if (root.querySelector('[data-payments]')?.isConnected) render(root, { ...ctx, params: new URLSearchParams() });
  };
  const history = root.querySelector('[data-payments]');
  bindPaymentActions(history, api.me, () => followDelivery(history));
  if (payments.some((payment) => payment.emailStatus === 'PENDING')) {
    followDelivery(history);
  }
  if (!payables.length) return;

  const form = root.querySelector('[data-card-form]');
  const button = form.querySelector('[data-pay]');
  const total = root.querySelector('[data-total]');
  const refreshTotal = () => {
    const sum = selectedItems(root, payables).reduce((result, item) => result + Number(item.amount), 0);
    total.textContent = formatMoney(sum);
    button.disabled = sum <= 0;
  };
  root.querySelectorAll('.pay-row input').forEach((input) => input.addEventListener('change', refreshTotal));
  refreshTotal();

  form.elements.namedItem('card.number').addEventListener('input', (event) => {
    event.target.value = event.target.value.replace(/\D/g, '').slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ');
  });
  form.elements.namedItem('card.expiry').addEventListener('input', (event) => {
    const digits = event.target.value.replace(/\D/g, '').slice(0, 4);
    event.target.value = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
  });
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    setButtonLoading(button, true, t('Проводим платёж…'));
    try {
      const payment = await api.me.pay({
        items: selectedItems(root, payables).map(({ type, id }) => ({ type, id })),
        card: {
          number: formValue(form, 'card.number'),
          expiry: formValue(form, 'card.expiry'),
          cvc: formValue(form, 'card.cvc'),
          holder: null,
        },
      });
      toast(t('Платёж № {0} на сумму {1}', [payment.number, formatMoney(payment.amount)]), 'success', t('Оплачено'));
      window.history.replaceState(null, '', '#/payments');
      reload();
    } catch (error) {
      setButtonLoading(button, false);
      showFormAlert(form, getErrorMessage(error));
      applyFieldErrors(form, error?.fieldErrors);
    }
  });
}

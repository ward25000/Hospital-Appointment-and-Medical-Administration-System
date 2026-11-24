package com.ward.hospital.payment;

import com.ward.hospital.dto.payment.CardRequest;
import com.ward.hospital.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

/**
 * Sandbox gateway. It never moves money and accepts published test cards only,
 * so a real card entered by mistake is rejected.
 */
@Component
public class DemoPaymentGateway implements PaymentGateway {

    private static final Set<String> APPROVED = Set.of("4242424242424242", "5555555555554444");
    private static final String DECLINED = "4000000000000002";

    @Override
    public Charge charge(String paymentNumber, BigDecimal amount, CardRequest card) {
        String number = card.number().replace(" ", "");
        String[] expiry = card.expiry().split("/");
        YearMonth validThru = YearMonth.of(2000 + Integer.parseInt(expiry[1]), Integer.parseInt(expiry[0]));
        if (validThru.isBefore(YearMonth.now())) {
            throw new BusinessRuleException("Срок действия карты истёк");
        }
        if (DECLINED.equals(number)) {
            throw new BusinessRuleException("Банк отклонил платёж. Попробуйте другую карту");
        }
        if (!APPROVED.contains(number)) {
            throw new BusinessRuleException(
                    "Платёжный шлюз работает в тестовом режиме: используйте тестовую карту 4242 4242 4242 4242");
        }
        return new Charge("DEMO-" + UUID.randomUUID(), number.substring(number.length() - 4));
    }
}

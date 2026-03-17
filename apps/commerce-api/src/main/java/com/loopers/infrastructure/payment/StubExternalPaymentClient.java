package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.ExternalPaymentClient;
import com.loopers.domain.payment.ExternalPaymentResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubExternalPaymentClient implements ExternalPaymentClient {

    @Override
    public ExternalPaymentResponse pay(Long orderId, CardType cardType, String cardNo, long amount) {
        // 외부 결제 시스템 연동 전 Stub 구현: 항상 성공 응답을 반환한다.
        return new ExternalPaymentResponse(UUID.randomUUID().toString(), true);
    }
}
package com.loopers.domain.payment;

import java.util.Optional;

public interface ExternalPaymentClient {

    ExternalPaymentResponse pay(Long orderId, CardType cardType, String cardNo, long amount);

    Optional<ExternalPaymentResponse> getPaymentByOrderId(Long orderId);
}
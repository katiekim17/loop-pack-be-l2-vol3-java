package com.loopers.domain.payment;

public interface ExternalPaymentClient {

    ExternalPaymentResponse pay(Long orderId, CardType cardType, String cardNo, long amount);
}
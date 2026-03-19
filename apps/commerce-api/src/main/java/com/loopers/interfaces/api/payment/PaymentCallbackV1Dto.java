package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentInfo;
import java.time.ZonedDateTime;

public class PaymentCallbackV1Dto {

    public record CallbackRequest(String transactionId, Long orderId, boolean success) {}

    public record CallbackResponse(Long paymentId, Long orderId, String status, long amount, ZonedDateTime createdAt) {
        public static CallbackResponse from(PaymentInfo info) {
            return new CallbackResponse(
                    info.paymentId(),
                    info.orderId(),
                    info.status().name(),
                    info.amount(),
                    info.createdAt()
            );
        }
    }
}

package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ExternalPaymentClient externalPaymentClient;

    @Transactional
    public Payment pay(Long orderId, Long memberId, CardType cardType, String cardNo, long amount) {
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new CoreException(ErrorType.ORDER_ALREADY_PAID);
        }

        // ⚠️ Option 1: 트랜잭션 내부에서 외부 결제 시스템 호출
        // 외부 호출 지연 시 DB 커넥션을 점유하고, 외부 성공 후 내부 실패 시 정합성이 깨짐
        ExternalPaymentResponse response = externalPaymentClient.pay(orderId, cardType, cardNo, amount);

        PaymentStatus status = response.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        Payment payment = new Payment(orderId, memberId, cardType, cardNo, amount, status, response.transactionId());

        return paymentRepository.save(payment);
    }
}
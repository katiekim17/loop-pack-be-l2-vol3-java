package com.loopers.domain.payment;

import com.loopers.domain.alert.AlertNotifier;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@RequiredArgsConstructor
@Component
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AlertNotifier alertNotifier;

    @Transactional
    public Payment savePending(Long orderId, Long memberId, CardType cardType, String cardNo, long amount) {
        paymentRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PENDING) {
                throw new CoreException(ErrorType.PAYMENT_IN_PROGRESS);
            }
            throw new CoreException(ErrorType.ORDER_ALREADY_PAID);
        });
        Payment payment = new Payment(orderId, memberId, cardType, cardNo, amount, PaymentStatus.PENDING, null);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment saveResult(ExternalPaymentResponse response, Long orderId, Long memberId, CardType cardType, String cardNo, long amount) {
        paymentRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PENDING) {
                throw new CoreException(ErrorType.PAYMENT_IN_PROGRESS);
            }
            throw new CoreException(ErrorType.ORDER_ALREADY_PAID);
        });
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    alertNotifier.notify("결제 DB 저장 실패", "PG 결제 성공 후 DB 저장 실패 - orderId: " + orderId);
                }
            }
        });
        PaymentStatus status = response.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        Payment payment = new Payment(orderId, memberId, cardType, cardNo, amount, status, response.transactionId());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment updateByCallback(String transactionId, Long orderId, boolean success) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다."));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }
        if (success) {
            payment.complete(transactionId);
        } else {
            payment.fail();
        }
        return paymentRepository.save(payment);
    }
}

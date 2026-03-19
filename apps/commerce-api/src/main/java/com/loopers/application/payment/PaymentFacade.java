package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.ExternalPaymentClient;
import com.loopers.domain.payment.ExternalPaymentResponse;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final UserService userService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ExternalPaymentClient externalPaymentClient;

    public PaymentInfo pay(String loginId, String password, Long orderId, CardType cardType, String cardNo) {
        Users user = userService.authenticate(loginId, password);
        Order order = orderService.getOrder(user.getId(), orderId);

        ExternalPaymentResponse resp;
        try {
            resp = externalPaymentClient.pay(order.getId(), cardType, cardNo, order.getFinalPrice());
        } catch (CoreException e) {
            throw e; // circuit open → propagate to controller
        } catch (Exception e) {
            // timeout / connection failure → PENDING
            Payment pending = paymentService.savePending(order.getId(), user.getId(), cardType, cardNo, order.getFinalPrice());
            return PaymentInfo.from(pending);
        }

        Payment payment = paymentService.saveResult(resp, order.getId(), user.getId(), cardType, cardNo, order.getFinalPrice());
        if (payment.isCompleted()) {
            orderService.confirmOrder(order.getId());
        }
        return PaymentInfo.from(payment);
    }

    public PaymentInfo handleCallback(String transactionId, Long orderId, boolean success) {
        Payment payment = paymentService.updateByCallback(transactionId, orderId, success);
        if (payment.isCompleted()) {
            orderService.confirmOrder(orderId);
        }
        return PaymentInfo.from(payment);
    }

    public PaymentInfo syncPayment(String loginId, String password, Long orderId) {
        Users user = userService.authenticate(loginId, password);
        Order order = orderService.getOrder(user.getId(), orderId);

        ExternalPaymentResponse pgResponse = externalPaymentClient.getPaymentByOrderId(order.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "PG에서 결제 정보를 찾을 수 없습니다."));

        Payment payment = paymentService.updateByCallback(pgResponse.transactionId(), order.getId(), pgResponse.success());
        if (payment.isCompleted()) {
            orderService.confirmOrder(order.getId());
        }
        return PaymentInfo.from(payment);
    }
}

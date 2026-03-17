package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final UserService userService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    // ⚠️ Option 1: @Transactional 범위 안에 외부 결제 호출이 포함됨
    // 외부 결제 성공 후 confirmOrder() 실패 시 트랜잭션이 롤백되지만 외부 결제는 취소되지 않음
    @Transactional
    public PaymentInfo pay(String loginId, String password, Long orderId, CardType cardType, String cardNo) {
        Users user = userService.authenticate(loginId, password);
        Order order = orderService.getOrder(user.getId(), orderId);

        Payment payment = paymentService.pay(order.getId(), user.getId(), cardType, cardNo, order.getFinalPrice());
        orderService.confirmOrder(order.getId());

        return PaymentInfo.from(payment);
    }
}
package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.alert.AlertNotifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class PaymentServiceAlertTest {

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private AlertNotifier alertNotifier;

    @DisplayName("saveResult() 에서 트랜잭션이 롤백될 때,")
    @Nested
    class SaveResultRollback {

        @DisplayName("DB 저장 실패로 롤백되면, 알림을 발송한다.")
        @Test
        void notifiesAlert_whenSaveRollsBack() {
            // arrange
            ExternalPaymentResponse response = new ExternalPaymentResponse("txn-001", true);
            when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());
            doThrow(new RuntimeException("DB error")).when(paymentRepository).save(any());

            // act
            assertThatThrownBy(() ->
                paymentService.saveResult(response, 1L, 1L, CardType.SAMSUNG, "1234-5678", 50000L)
            ).isInstanceOf(RuntimeException.class);

            // assert
            verify(alertNotifier).notify(anyString(), anyString());
        }

        @DisplayName("DB 저장이 성공하면, 알림을 발송하지 않는다.")
        @Test
        void doesNotNotifyAlert_whenSaveSucceeds() {
            // arrange
            ExternalPaymentResponse response = new ExternalPaymentResponse("txn-001", true);
            Payment payment = new Payment(1L, 1L, CardType.SAMSUNG, "1234-5678", 50000L, PaymentStatus.COMPLETED, "txn-001");
            when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());
            when(paymentRepository.save(any())).thenReturn(payment);

            // act
            paymentService.saveResult(response, 1L, 1L, CardType.SAMSUNG, "1234-5678", 50000L);

            // assert
            verify(alertNotifier, never()).notify(anyString(), anyString());
        }
    }
}
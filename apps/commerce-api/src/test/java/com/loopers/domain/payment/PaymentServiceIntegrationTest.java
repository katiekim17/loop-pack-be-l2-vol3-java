package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("savePending() 를 호출할 때,")
    @Nested
    class SavePending {

        @DisplayName("정상 요청이면, PENDING 상태의 결제를 저장한다.")
        @Test
        void savesPendingPayment_whenValidRequest() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;

            // act
            Payment result = paymentService.savePending(orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING),
                () -> assertThat(result.getExternalTransactionId()).isNull()
            );
        }

        @DisplayName("이미 PENDING 인 주문이면, PAYMENT_IN_PROGRESS 예외가 발생한다.")
        @Test
        void throwsException_whenPaymentAlreadyPending() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            paymentService.savePending(orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // act & assert
            assertThatThrownBy(() ->
                paymentService.savePending(orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L)
            ).isInstanceOf(CoreException.class)
             .extracting(e -> ((CoreException) e).getErrorType())
             .isEqualTo(ErrorType.PAYMENT_IN_PROGRESS);
        }

        @DisplayName("이미 COMPLETED/FAILED 인 주문이면, ORDER_ALREADY_PAID 예외가 발생한다.")
        @Test
        void throwsException_whenOrderAlreadyPaid() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            ExternalPaymentResponse response = new ExternalPaymentResponse("txn-001", true);
            paymentService.saveResult(response, orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // act & assert
            assertThatThrownBy(() ->
                paymentService.savePending(orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L)
            ).isInstanceOf(CoreException.class)
             .extracting(e -> ((CoreException) e).getErrorType())
             .isEqualTo(ErrorType.ORDER_ALREADY_PAID);
        }
    }

    @DisplayName("saveResult() 를 호출할 때,")
    @Nested
    class SaveResult {

        @DisplayName("PG 응답이 success=true 이면, COMPLETED 상태의 결제를 저장한다.")
        @Test
        void savesCompletedPayment_whenPgSucceeds() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            ExternalPaymentResponse response = new ExternalPaymentResponse("txn-001", true);

            // act
            Payment result = paymentService.saveResult(response, orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // assert
            assertAll(
                () -> assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED),
                () -> assertThat(result.getExternalTransactionId()).isEqualTo("txn-001")
            );
        }

        @DisplayName("PG 응답이 success=false 이면, FAILED 상태의 결제를 저장한다.")
        @Test
        void savesFailedPayment_whenPgFails() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            ExternalPaymentResponse response = new ExternalPaymentResponse(null, false);

            // act
            Payment result = paymentService.saveResult(response, orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // assert
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @DisplayName("이미 결제된 주문이면, ORDER_ALREADY_PAID 예외가 발생한다.")
        @Test
        void throwsException_whenOrderAlreadyPaid() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            ExternalPaymentResponse response = new ExternalPaymentResponse("txn-001", true);
            paymentService.saveResult(response, orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // act & assert
            assertThatThrownBy(() ->
                paymentService.saveResult(response, orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L)
            ).isInstanceOf(CoreException.class)
             .extracting(e -> ((CoreException) e).getErrorType())
             .isEqualTo(ErrorType.ORDER_ALREADY_PAID);
        }
    }

    @DisplayName("updateByCallback() 를 호출할 때,")
    @Nested
    class UpdateByCallback {

        @DisplayName("success=true 이면, PENDING 결제를 COMPLETED 로 업데이트한다.")
        @Test
        void updatesToCompleted_whenSuccessIsTrue() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            paymentService.savePending(orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // act
            Payment result = paymentService.updateByCallback("txn-001", orderId, true);

            // assert
            assertAll(
                () -> assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED),
                () -> assertThat(result.getExternalTransactionId()).isEqualTo("txn-001")
            );
        }

        @DisplayName("success=false 이면, PENDING 결제를 FAILED 로 업데이트한다.")
        @Test
        void updatesToFailed_whenSuccessIsFalse() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            paymentService.savePending(orderId, memberId, CardType.SAMSUNG, "1234-5678", 50000L);

            // act
            Payment result = paymentService.updateByCallback(null, orderId, false);

            // assert
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @DisplayName("존재하지 않는 orderId 이면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenOrderNotFound() {
            // act & assert
            assertThatThrownBy(() ->
                paymentService.updateByCallback("txn-001", 999L, true)
            ).isInstanceOf(CoreException.class)
             .extracting(e -> ((CoreException) e).getErrorType())
             .isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}

package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.ExternalPaymentClient;
import com.loopers.domain.payment.ExternalPaymentResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class HttpExternalPaymentClient implements ExternalPaymentClient {

    private final RestTemplate pgRestTemplate;
    private final PgSimulatorProperties properties;

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackPay")
    @Retry(name = "pgRetry")
    @Override
    public ExternalPaymentResponse pay(Long orderId, CardType cardType, String cardNo, long amount) {
        String url = properties.url() + "/api/v1/payments";
        PgPayRequest request = new PgPayRequest(orderId, cardType.name(), cardNo, amount);
        try {
            PgPayResponse response = pgRestTemplate.postForObject(url, request, PgPayResponse.class);
            return new ExternalPaymentResponse(response.transactionId(), response.success());
        } catch (HttpStatusCodeException e) {
            return new ExternalPaymentResponse(null, false);
        }
    }

    private ExternalPaymentResponse fallbackPay(Long orderId, CardType cardType, String cardNo, long amount, Throwable t) {
        throw new CoreException(ErrorType.PG_CIRCUIT_OPEN);
    }

    @Override
    public Optional<ExternalPaymentResponse> getPaymentByOrderId(Long orderId) {
        String url = properties.url() + "/api/v1/payments?orderId=" + orderId;
        try {
            PgPayResponse response = pgRestTemplate.getForObject(url, PgPayResponse.class);
            return Optional.ofNullable(response)
                    .map(r -> new ExternalPaymentResponse(r.transactionId(), r.success()));
        } catch (HttpStatusCodeException e) {
            return Optional.empty();
        }
    }

    record PgPayRequest(Long orderId, String cardType, String cardNo, long amount) {}

    record PgPayResponse(String transactionId, boolean success) {}
}

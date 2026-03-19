package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCallbackV1Controller {

    private final PaymentFacade paymentFacade;

    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PaymentCallbackV1Dto.CallbackResponse> callback(
            @RequestBody PaymentCallbackV1Dto.CallbackRequest request
    ) {
        PaymentInfo info = paymentFacade.handleCallback(
                request.transactionId(),
                request.orderId(),
                request.success()
        );
        return ApiResponse.success(PaymentCallbackV1Dto.CallbackResponse.from(info));
    }
}

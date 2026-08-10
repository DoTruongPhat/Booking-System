package com.booking.payment.application.port.in;

import java.util.Map;

public interface VerifyCallbackUseCase {

    void execute(String gatewayName, Map<String, String> params);
}
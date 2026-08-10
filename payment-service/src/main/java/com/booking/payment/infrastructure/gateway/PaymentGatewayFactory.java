package com.booking.payment.infrastructure.gateway;

import com.booking.payment.application.port.out.PaymentGatewayPort;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import com.booking.payment.domain.model.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayFactory {

    private final Map<String, PaymentGatewayPort> gateways;

    public PaymentGatewayFactory(List<PaymentGatewayPort> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(
                        g -> g.getName().toUpperCase(),
                        Function.identity()
                ));
    }

    public PaymentGatewayPort getGateway(PaymentMethod method) {
        PaymentGatewayPort gateway = gateways.get(method.name());
        if (gateway == null) {
            throw new PaymentException(PaymentErrorCode.GATEWAY_NOT_SUPPORTED,
                    "No gateway for method: " + method);
        }
        return gateway;
    }

    public PaymentGatewayPort getGateway(String gatewayName) {
        PaymentGatewayPort gateway = gateways.get(gatewayName.toUpperCase());
        if (gateway == null) {
            throw new PaymentException(PaymentErrorCode.GATEWAY_NOT_SUPPORTED,
                    "Unknown gateway: " + gatewayName);
        }
        return gateway;
    }
}
package com.booking.payment.presentation.response;

import com.booking.payment.domain.model.RefundHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class RefundResponse {

    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String gatewayRefundTxnId;
    private UUID requestedBy;
    private Instant requestedAt;
    private Instant completedAt;

    public static RefundResponse from(RefundHistory r) {
        return new RefundResponse(
                r.getId(),
                r.getPaymentId(),
                r.getAmount(),
                r.getReason(),
                r.getStatus().name(),
                r.getGatewayRefundTxnId(),
                r.getRequestedBy(),
                r.getRequestedAt(),
                r.getCompletedAt()
        );
    }
}
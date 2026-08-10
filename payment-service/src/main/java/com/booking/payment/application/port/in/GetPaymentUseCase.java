package com.booking.payment.application.port.in;

import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.RefundHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface GetPaymentUseCase {

    Payment getById(UUID paymentId);

    Payment getByBookingId(UUID bookingId, UUID userId);

    Page<Payment> getByUserId(UUID userId, Pageable pageable);

    Page<Payment> getAll(String status, Pageable pageable);

    List<RefundHistory> getRefundHistory(UUID paymentId);
}
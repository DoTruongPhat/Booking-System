package com.booking.payment.application.service;

import com.booking.payment.application.port.in.GetPaymentUseCase;
import com.booking.payment.application.port.out.PaymentRepositoryPort;
import com.booking.payment.application.port.out.RefundRepositoryPort;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.RefundHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final RefundRepositoryPort refundRepository;

    @Override
    public Payment getById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    @Override
    public Payment getByBookingId(UUID bookingId, UUID userId) {
        // Prefer a final successful payment so callback polling does not get stuck
        // on an older pending attempt for the same booking.
        Payment payment = paymentRepository.findSuccessfulByBookingId(bookingId)
                .or(() -> paymentRepository.findActiveByBookingId(bookingId))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "No payment for booking: " + bookingId));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_OWNED);
        }
        return payment;
    }

    @Override
    public Page<Payment> getByUserId(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<Payment> getAll(String status, Pageable pageable) {
        return paymentRepository.findAll(status, pageable);
    }

    @Override
    public List<RefundHistory> getRefundHistory(UUID paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }
}

package com.booking.payment.infrastructure.external;

import com.booking.payment.application.port.out.BookingPaymentValidationPort;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class BookingPaymentValidationAdapter implements BookingPaymentValidationPort {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public BookingPaymentValidationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${app.booking-service.url:http://localhost:8082}") String bookingServiceUrl,
            @Value("${app.internal.api-key}") String internalApiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(bookingServiceUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public BookingPaymentSnapshot getPaymentSnapshot(UUID bookingId) {
        try {
            BookingPaymentSnapshot snapshot = restClient.get()
                    .uri("/internal/bookings/{bookingId}/payment-snapshot", bookingId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(BookingPaymentSnapshot.class);

            if (snapshot == null) {
                throw new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND,
                        "Booking snapshot response is empty: " + bookingId);
            }
            return snapshot;
        } catch (PaymentException e) {
            throw e;
        } catch (RestClientException e) {
            throw new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND,
                    "Cannot validate booking " + bookingId + ": " + e.getMessage());
        }
    }
}

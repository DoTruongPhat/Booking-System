package com.booking.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookingMetrics {

    private final Counter bookingCreated;
    private final Counter bookingConfirmed;
    private final Counter bookingCancelled;
    private final Timer bookingCreationDuration;

    public BookingMetrics(MeterRegistry registry) {
        this.bookingCreated = Counter.builder("booking.created.total")
                .description("Total bookings created")
                .tag("service", "booking-service")
                .register(registry);

        this.bookingConfirmed = Counter.builder("booking.confirmed.total")
                .description("Total bookings confirmed")
                .tag("service", "booking-service")
                .register(registry);

        this.bookingCancelled = Counter.builder("booking.cancelled.total")
                .description("Total bookings cancelled")
                .tag("service", "booking-service")
                .register(registry);

        this.bookingCreationDuration = Timer.builder("booking.creation.duration")
                .description("Time to create a booking")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("service", "booking-service")
                .register(registry);

        log.info("BookingMetrics registered: booking.created, booking.confirmed, booking.cancelled, booking.creation.duration");
    }

    public void recordCreated() {
        bookingCreated.increment();
    }

    public void recordConfirmed() {
        bookingConfirmed.increment();
    }

    public void recordCancelled(String reason) {
        bookingCancelled.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(bookingCreationDuration);
    }
}
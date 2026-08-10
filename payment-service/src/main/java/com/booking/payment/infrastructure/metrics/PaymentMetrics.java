package com.booking.payment.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentMetrics {

    private final Counter paymentInitiated;
    private final Counter paymentSuccess;
    private final Counter paymentFailed;
    private final Counter paymentExpired;
    private final Counter refundProcessed;
    private final Timer paymentDuration;

    public PaymentMetrics(MeterRegistry registry) {
        this.paymentInitiated = Counter.builder("payment.initiated.total")
                .description("Total payments initiated")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentSuccess = Counter.builder("payment.success.total")
                .description("Total payments succeeded")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentFailed = Counter.builder("payment.failed.total")
                .description("Total payments failed")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentExpired = Counter.builder("payment.expired.total")
                .description("Total payments expired")
                .tag("service", "payment-service")
                .register(registry);

        this.refundProcessed = Counter.builder("refund.processed.total")
                .description("Total refunds processed")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentDuration = Timer.builder("payment.duration")
                .description("Payment processing duration")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("service", "payment-service")
                .register(registry);

        log.info("PaymentMetrics registered");
    }

    public void recordInitiated(String method) {
        paymentInitiated.increment();
    }

    public void recordSuccess(String method) {
        paymentSuccess.increment();
    }

    public void recordFailed(String reason) {
        paymentFailed.increment();
    }

    public void recordExpired() {
        paymentExpired.increment();
    }

    public void recordRefund() {
        refundProcessed.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(paymentDuration);
    }
}
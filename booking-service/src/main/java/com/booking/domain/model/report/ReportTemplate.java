package com.booking.domain.model.report;

public enum ReportTemplate {

    BOOKING_CONFIRMATION("booking-confirmation"),
    PAYMENT_RECEIPT("payment-receipt"),
    MONTHLY_REVENUE("monthly-revenue"),
    REVENUE_REPORT("revenue-report");

    private final String templateName;

    ReportTemplate(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }
}
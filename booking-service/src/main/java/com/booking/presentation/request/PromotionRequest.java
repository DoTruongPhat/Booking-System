package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PromotionRequest {

    private UUID hotelId;

    @NotBlank(message = "Promotion title is required")
    private String title;

    private String description;

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "PERCENT|FIXED", message = "Discount type must be PERCENT or FIXED")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean active;
}

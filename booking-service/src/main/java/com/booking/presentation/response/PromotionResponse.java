package com.booking.presentation.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PromotionResponse {
    private UUID id;
    private UUID hotelId;
    private String hotelName;
    private String title;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

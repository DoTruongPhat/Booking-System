package com.booking.presentation.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
    private String email;
    private String timezone;
    private Boolean active;
}

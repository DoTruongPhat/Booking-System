package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignRoleRequest {

    @NotBlank(message = "Role code is required")
    private String roleCode;



}

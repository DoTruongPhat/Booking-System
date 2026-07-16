package com.booking.presentation.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
 private String email;
 private String timezone;
 private Boolean active;
 /** Phase 7: phone của user (optional - chỉ set khi cần update). */
 private String phone;
 private String firstName;
 private String lastName;
}
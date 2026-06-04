package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;

public class AdminResetPasswordRequest {
    @NotBlank(message = "New password is required")
    private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

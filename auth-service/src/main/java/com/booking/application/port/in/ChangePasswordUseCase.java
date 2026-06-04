package com.booking.application.port.in;

public interface ChangePasswordUseCase {
    void changePassword(String email, String currentPassword, String newPassword);
}

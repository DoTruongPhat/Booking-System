package com.booking.application.port.in;

public interface SetPasswordUseCase {
    void setPassword(String currentUsername, String newUsername, String newPassword);
}
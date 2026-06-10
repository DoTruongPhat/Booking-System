package com.booking.application.port.in;

import com.booking.presentation.response.TwoFactorResponse;

public interface Manage2faUseCase {
    TwoFactorResponse setup(String username);
    void enable(String username, String otp);
    void disable(String username);
}

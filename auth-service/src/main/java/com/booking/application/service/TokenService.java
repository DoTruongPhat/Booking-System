package com.booking.application.service;

import com.booking.domain.model.User;

public interface TokenService {
    String createToken(User user, String ipAddress, String userAgent);

    String hashToken(String token);

    boolean validateToken(String token);
}

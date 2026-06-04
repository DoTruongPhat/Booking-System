package com.booking.application.service;

public interface PasswordService {
    HashedPassword hash(String rawPassword, String username, String userId);
    boolean verify(String rawPassword,
            String storedHash,
            String userId,
            String username);
    record HashedPassword(String hash, String salt) { }
}

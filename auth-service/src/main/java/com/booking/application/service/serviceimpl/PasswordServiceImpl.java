package com.booking.application.service.serviceimpl;

import com.booking.application.service.PasswordService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class PasswordServiceImpl implements PasswordService {

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();


    @Override
    public HashedPassword hash(String rawPassword,
                               String username,
                               String userId) {
        // Bước 1: SHA-256(password + username)
        String firstHash = sha256(rawPassword + username);
        // Bước 2: SHA-256(firstHash + userId) → giới hạn độ dài
        // Tránh BCrypt giới hạn 72 bytes
        String combined = sha256(firstHash + userId);

        // Bước 3: BCrypt(combined)
        String bcryptHash = encoder.encode(combined);

        // Bước 4: Trả về hash và salt (userId)
        return new HashedPassword(bcryptHash, userId);

    }

    @Override
    public boolean verify(String rawPassword,
                          String storedHash,
                          String userId,
                          String username) {
        // Bước 1: SHA-256(password + username)
        String firstHash = sha256(rawPassword + username);
        String combined = sha256(firstHash + userId);
        // Bước 2: BCrypt verify
        return encoder.matches(combined, storedHash);

    }

    private String sha256(String input) {
        try{
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}

package com.booking.domain.event;

/**
 * Event: User yêu cầu reset password
 * → Trigger: gửi email reset link
 */
public class PasswordResetRequestedEvent extends DomainEvent{
    private final String userId;
    private final String username;
    private final String email;
    private final String resetToken;

    public PasswordResetRequestedEvent(String userId, String username, String email, String resetToken) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.resetToken = resetToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getResetToken() {
        return resetToken;
    }
}

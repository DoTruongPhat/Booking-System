package com.booking.domain.event;

/**
 * Event: User đã đăng ký thành công
 * → Trigger: gửi email chào mừng
 */
public class UserRegisteredEvent extends DomainEvent {

    private final String userId;
    private final String username;
    private final String email;

    public UserRegisteredEvent(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
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
}

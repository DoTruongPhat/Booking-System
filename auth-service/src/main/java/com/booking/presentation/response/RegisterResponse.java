package com.booking.presentation.response;


import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

/**
 * Response trả về sau khi đăng ký thành công
 * Không trả về password, passwordHash, passwordSalt
 * → Không lộ thông tin nhạy cảm
 */

@Data
@Builder
public class RegisterResponse {
    // ID của user vừa tạo
    private String id;

    // Tên đăng nhập
    private String username;

    // Email
    private String email;

    // Múi giờ
    private String timezone;

    // Thời điểm tạo tài khoản
    private ZonedDateTime createdAt;

    // Thông báo cho FE
    private String message;
}

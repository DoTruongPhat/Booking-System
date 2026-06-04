package com.booking.application.port.in;

import java.util.UUID;

/**
 * User tự đổi password
 * → Phải nhập current password để xác thực
 */
public interface AdminResetPasswordUseCase {
    void adminResetPassword(UUID userId, String newPassword);

}

package com.booking.application.port.out;

import com.booking.domain.model.Role;
import java.util.Optional;

public interface RoleRepositoryPort {
    /**
     * Tìm role theo code
     * Dùng khi register: gán role USER mặc định
     */
    Optional<Role> findByCode(String code);
}

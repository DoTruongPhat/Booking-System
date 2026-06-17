package com.booking.application.port.out;

import com.booking.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepositoryPort = Output Port
 *
 * Tại sao dùng User domain thay vì UserEntity?
 * → Application layer không biết JPA tồn tại
 * → Không phụ thuộc infrastructure
 * → Có thể swap JPA sang MongoDB
 *   mà không cần sửa application layer
 *
 * Dependency Rule:
 * application/ → chỉ biết domain/
 * KHÔNG biết infrastructure/
 */
public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    /**
     * V8: Tìm user theo Keycloak user id (dùng cho sync từ KC)
     */
    Optional<User> findByKcUserId(String kcUserId);

    User save(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByIdWithRoles(UUID id);

    Page<User> findAll(Pageable pageable);
}
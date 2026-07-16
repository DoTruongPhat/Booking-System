package com.booking.application.port.out;

import com.booking.domain.model.User;
import com.booking.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepositoryPort = Output Port
 *
 * Tại sao dùng User domain thay vì UserEntity?
 * → Application layer không biết JPA tồn tại
 * → Không phụ thuộc infrastructure
 * → Có thể swap JPA sang MongoDB
 * mà không cần sửa application layer
 *
 * Dependency Rule:
 * application/ → chỉ biết domain/
 * KHÔNG biết infrastructure/
 */
public interface UserRepositoryPort {

 Optional<User> findByUsername(String username);

 Optional<User> findByEmail(String email);

 Optional<User> findById(UUID id);


 Optional<User> findByEmailIgnoreCase(String email);

 User save(User user);

 boolean existsByUsername(String username);

 boolean existsByEmail (String email);

 Optional<User> findByIdWithRoles(UUID id);

 Page<User> findAll(Pageable pageable);

 /**
 * Đếm tổng số user trong hệ thống (cho dashboard)
 */
 long count();
 void deleteById(UUID id);
 // Repository JPA
 @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.username = :username")
 Optional<User> findByUsernameWithRoles(@Param("username") String username);
}
package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.UserRepositoryPort;
import com.booking.domain.model.User;
import com.booking.infrastructure.persistence.entity.RoleEntity;
import com.booking.infrastructure.persistence.entity.UserEntity;
import com.booking.infrastructure.persistence.mapper.UserEntityMapper;
import com.booking.infrastructure.persistence.repository.RoleJpaRepository;
import com.booking.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UserRepositoryAdapter = implement UserRepositoryPort
 *
 * Vai trò:
 * → Cầu nối giữa Domain và DB
 * → Domain gọi port/out (UserRepositoryPort)
 * → Adapter nhận → convert Domain → Entity → JPA
 * → JPA query → Entity → convert Entity → Domain → trả về
 *
 * Tại sao cần mapper ở đây?
 * → Service chỉ biết User (domain model)
 * → JPA chỉ biết UserEntity
 * → Adapter dùng mapper để convert 2 chiều
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    private final RoleJpaRepository roleJpaRepository;

    // Inject mapper để convert Entity ↔ Domain
    private final UserEntityMapper userEntityMapper;

    private final EntityManager entityManager;

    @Override
    public Optional<User> findByUsername(String username) {
        // JPA trả về UserEntity
        // → mapper convert sang User domain
        return userJpaRepository.findByUsername(username)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByKcUserId(String kcUserId) {
        return userJpaRepository.findByKcUserId(kcUserId)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(userEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity userEntity = userEntityMapper.toEntity(user);

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Set<RoleEntity> roleEntities = user.getRoles().stream()
                    .map(role -> roleJpaRepository.findByCode(role.getCode())
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            userEntity.setRoles(roleEntities);
        }

        UserEntity saved = userJpaRepository.save(userEntity);

        // Flush + clear cache → force query mới
        entityManager.flush();
        entityManager.clear();

        // Load lại với roles + permissions
        // Không fallback → nếu lỗi thì biết ngay
        return userJpaRepository.findByIdWithRoles(saved.getId())
                .map(userEntityMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found after save: " + saved.getId()));
    }

    @Override
    public boolean existsByUsername(String username) {
        // Không cần convert → trả về boolean trực tiếp
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByIdWithRoles(UUID id) {
        // Fetch user kèm roles + permissions
        // → mapper tự convert nested objects
        //   UserEntity → User
        //   Set<RoleEntity> → Set<Role>
        //   Set<PermissionEntity> → Set<Permission>
        return userJpaRepository.findByIdWithRoles(id)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        // Dùng custom query fetch roles + permissions
        List<UserEntity> allUsers = userJpaRepository.findAllWithRoles();

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allUsers.size());

        List<User> pageContent = allUsers.subList(start, end).stream()
                .map(userEntityMapper::toDomain)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, allUsers.size());
    }
}
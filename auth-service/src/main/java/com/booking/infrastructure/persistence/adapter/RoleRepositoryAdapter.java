package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.domain.model.Role;
import com.booking.infrastructure.persistence.mapper.RoleEntityMapper;
import com.booking.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository repository;

    private final RoleEntityMapper roleEntityMapper;

    @Override
    public Optional<Role> findByCode(String code) {
        return repository.findByCode(code)
                .map(roleEntityMapper::toDomain);
    }
}

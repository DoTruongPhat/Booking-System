package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.domain.model.Role;
import com.booking.infrastructure.persistence.mapper.RoleEntityMapper;
import com.booking.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return repository.findAll().stream()
                .map(roleEntityMapper::toDomain)
                .toList();
    }
}

package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.SystemParamRepositoryPort;
import com.booking.domain.model.SystemParam;
import com.booking.infrastructure.persistence.mapper.SystemParamEntityMapper;
import com.booking.infrastructure.persistence.repository.SystemParamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SystemParamRepositoryAdapter implements SystemParamRepositoryPort {

    private final SystemParamJpaRepository systemParamJpaRepository;

    private final SystemParamEntityMapper systemParamEntityMapper;

    @Override
    public List<SystemParam> findAll() {
        return systemParamJpaRepository.findAll()
                .stream()
                .map(systemParamEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SystemParam> findByKey(String key) {
        return systemParamJpaRepository.findById(key)
                .map(systemParamEntityMapper::toDomain);
    }

    @Override
    public SystemParam save(SystemParam systemParam) {
        return systemParamEntityMapper.toDomain(
                systemParamJpaRepository.save(
                        systemParamEntityMapper.toEntity(systemParam)
                )
        );
    }
}

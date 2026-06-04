package com.booking.application.port.out;

import com.booking.domain.model.SystemParam;

import java.util.List;
import java.util.Optional;

public interface SystemParamRepositoryPort {

    List<SystemParam> findAll();
    Optional<SystemParam> findByKey(String key);
    SystemParam save(SystemParam systemParam);

}

package com.booking.application.service.serviceimpl;

import com.booking.application.port.out.SystemParamRepositoryPort;
import com.booking.application.service.SystemParamService;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.ValidationException;
import com.booking.domain.model.SystemParam;
import com.booking.infrastructure.external.cache.SystemParamCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class SystemParamServiceImpl implements SystemParamService {

    private final SystemParamCacheService systemParamCacheService;
    private final SystemParamRepositoryPort systemParamRepositoryPort;


    // Load tất cả params vào Redis khi app khởi động xong
    @EventListener(ApplicationReadyEvent.class)
    public void loadAllOnStartUp() {
        log.info("[SysParam] Loading all params into cache...");
        List<SystemParam> params = systemParamRepositoryPort.findAll();
        params.forEach(p -> systemParamCacheService.put(p.getKey(), p.getValue()));
        log.info("[SysParam] Loaded {} params into cache", params.size());
    }

    @Override
    public List<SystemParam> getAll() {
        return systemParamRepositoryPort.findAll() ;
    }

    @Override
    public SystemParam getByKey(String key) {
        return systemParamRepositoryPort.findByKey(key)
                .orElseThrow(() -> new ValidationException(
                        ErrorCode.CMN_004,
                        ErrorCode.CMN_004_MSG + key
                ));
    }

    @Override
    @Transactional
    public SystemParam update(String key, String value, String updatedBy) {

        SystemParam param = getByKey(key);
        param.setValue(value);
        param.setUpdatedAt(ZonedDateTime.now());
        param.setUpdatedBy(updatedBy);
        SystemParam saved = systemParamRepositoryPort.save(param);

        // Update Redis cache ngay lập tức
        systemParamCacheService.put(key, value);
        log.info("[SysParam] Updated {}={} by {}", key, value, updatedBy);
        return saved;

    }

    @Override
    public String getValue(String key, String defaultValue) {
        String cached = systemParamCacheService.get(key);
        if(cached != null) return cached;

        return  systemParamRepositoryPort.findByKey(key)
                .map(SystemParam::getValue)
                .orElse(defaultValue);
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

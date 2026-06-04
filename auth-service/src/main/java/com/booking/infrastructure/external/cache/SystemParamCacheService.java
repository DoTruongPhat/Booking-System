package com.booking.infrastructure.external.cache;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class SystemParamCacheService {

    private static final String PREFIX = "sysparam:";

    private final StringRedisTemplate redisTemplate;

    public void put(String key, String value){
        redisTemplate.opsForValue().set(PREFIX + key,value);
        log.debug("[SysParam] Cached: {}={}", key, value);
    }

    public String get(String key){
        return redisTemplate.opsForValue().get(PREFIX + key);
    }

    public void delete(String key){
        redisTemplate.delete(PREFIX + key);
    }

    public void putAll(Map<String, String> map){
        map.forEach(this::put);
        log.info("[SysParam] Cached {} params", map.size());
    }

}

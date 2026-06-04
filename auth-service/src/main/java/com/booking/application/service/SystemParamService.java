package com.booking.application.service;

import com.booking.domain.model.SystemParam;

import java.util.List;

public interface SystemParamService {

    List<SystemParam> getAll();
    SystemParam getByKey(String key);
    SystemParam update(String key, String value, String updatedBy);
    String getValue(String key, String defaultValue);
    int getIntValue(String key, int defaultValue);


}

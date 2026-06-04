package com.booking.domain.model;

import java.time.ZonedDateTime;

/**
 * SystemParam domain model - Pure Java
 * → Config động của hệ thống
 */
public class SystemParam {

    private String key;
    private String value;
    private String description;
    private ZonedDateTime updatedAt;
    private String updatedBy;

    public SystemParam() {}

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
package com.booking.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Permission domain model - Pure Java
 */
public class Permission {

    private UUID id;
    private String code;
    private String name;
    private String resource;
    private String action;
    private String description;
    private ZonedDateTime createdAt;

    public Permission() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
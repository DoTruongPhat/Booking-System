package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.UserKcLink;
import com.booking.infrastructure.persistence.entity.UserKcLinkEntity;
import org.springframework.stereotype.Component;

@Component
public class UserKcLinkMapper {

    public UserKcLink toDomain(UserKcLinkEntity entity) {
        UserKcLink link = new UserKcLink();
        link.setUserId(entity.getUserId());
        link.setKcUserId(entity.getKcUserId());
        link.setKcProvider(entity.getKcProvider());
        link.setAuthSource(entity.getAuthSource());
        link.setKcSyncedAt(entity.getKcSyncedAt());
        link.setSyncStatus(entity.getSyncStatus());
        link.setSyncVersion(entity.getSyncVersion());
        link.setCreatedAt(entity.getCreatedAt());
        link.setUpdatedAt(entity.getUpdatedAt());
        return link;
    }

    public UserKcLinkEntity toEntity(UserKcLink domain) {
        UserKcLinkEntity entity = new UserKcLinkEntity();
        entity.setUserId(domain.getUserId());
        entity.setKcUserId(domain.getKcUserId());
        entity.setKcProvider(domain.getKcProvider());
        entity.setAuthSource(domain.getAuthSource());
        entity.setKcSyncedAt(domain.getKcSyncedAt());
        entity.setSyncStatus(domain.getSyncStatus());
        entity.setSyncVersion(domain.getSyncVersion());
        return entity;
    }
}
package com.booking.keycloak.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.awt.*;

public class BookingUserModel extends AbstractUserAdapterFederatedStorage{

    private final String dbUserId;
    private final String username;
    private final String email;
    private final boolean enabled;

    public BookingUserModel(KeycloakSession session,
                            RealmModel realm,
                            ComponentModel storageProviderModel,
                            String dbUserId,
                            String username,
                            String email,
                            boolean enabled) {
        super(session, realm, storageProviderModel);
        this.dbUserId = dbUserId;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.storageId = new StorageId(
                storageProviderModel.getId(), dbUserId
        );
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
    }

    @Override
    public String getEmail() {
        return email;
    }
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }

    public String getDbUserId() {
        return dbUserId;
    }
}

package com.booking.keycloak.factory;

import com.booking.keycloak.provider.RemoteUserStorageProvider;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * RemoteUserStorageProviderFactory
 * → Factory tạo RemoteUserStorageProvider
 * → Đăng ký với Keycloak qua SPI mechanism
 *
 * Khác với BookingUserStorageProviderFactory:
 * → Không cần DataSource (không connect DB trực tiếp)
 * → Config: authServiceUrl + internalKey
 *   thay vì DB connection config
 *
 * Config hiển thị trong KC Admin UI khi setup User Federation
 */
public class RemoteUserStorageProviderFactory
        implements UserStorageProviderFactory<RemoteUserStorageProvider> {

    private static final Logger log = Logger.getLogger(
            RemoteUserStorageProviderFactory.class);

    // ID duy nhất để KC nhận diện provider này
    // → Hiển thị trong KC Admin UI dropdown
    public static final String PROVIDER_ID = "booking-remote-provider";

    // Config keys
    private static final String AUTH_SERVICE_URL = "authServiceUrl";
    private static final String INTERNAL_KEY = "internalKey";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Booking Remote User Federation - " +
                "Verify users via HTTP API";
    }

    @Override
    public RemoteUserStorageProvider create(
            KeycloakSession session,
            ComponentModel model) {

        // Lấy config từ KC Admin UI
        String authServiceUrl = model.getConfig()
                .getFirst(AUTH_SERVICE_URL);
        String internalKey = model.getConfig()
                .getFirst(INTERNAL_KEY);

        log.infof("[KC-Remote] Creating provider, url: %s",
                authServiceUrl);

        return new RemoteUserStorageProvider(
                session,
                model,
                authServiceUrl,
                internalKey
        );
    }

    /**
     * Config fields hiển thị trong KC Admin UI
     * → Admin điền authServiceUrl và internalKey
     *   khi setup User Federation
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // Config 1: Auth Service URL
        ProviderConfigProperty urlProp =
                new ProviderConfigProperty();
        urlProp.setName(AUTH_SERVICE_URL);
        urlProp.setLabel("Auth Service URL");
        urlProp.setHelpText(
                "URL của Auth Service, ví dụ: http://auth-service:8081");
        urlProp.setType(ProviderConfigProperty.STRING_TYPE);
        urlProp.setDefaultValue("http://localhost:8081");

        // Config 2: Internal Key
        ProviderConfigProperty keyProp =
                new ProviderConfigProperty();
        keyProp.setName(INTERNAL_KEY);
        keyProp.setLabel("Internal Key");
        keyProp.setHelpText(
                "X-Internal-Key header value " +
                        "để Auth Service verify request từ KC");
        keyProp.setType(ProviderConfigProperty.PASSWORD);
        keyProp.setSecret(true);

        return List.of(urlProp, keyProp);
    }
}

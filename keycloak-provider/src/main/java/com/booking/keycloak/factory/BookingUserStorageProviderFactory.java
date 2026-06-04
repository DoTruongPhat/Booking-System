package com.booking.keycloak.factory;

import com.booking.keycloak.provider.BookingUserStorageProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;


public class BookingUserStorageProviderFactory implements
        UserStorageProviderFactory<BookingUserStorageProvider> {


    public static final String PROVIDER_ID = "booking-user-storage-db";

    public static final String CFG_DB_URL = "dbUrl";
    public static final String CFG_DB_USERNAME = "dbUsername";
    public static final String CFG_DB_PASSWORD = "dbPassword";
    private static final Logger log =
            Logger.getLogger(BookingUserStorageProviderFactory.class);

    private volatile HikariDataSource dataSource;

    @Override
    public BookingUserStorageProvider create(KeycloakSession keycloakSession,
                                             ComponentModel componentModel) {
        if(dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    dataSource = initDataSource(componentModel);
                }
            }
        }
        return new BookingUserStorageProvider(keycloakSession, componentModel, dataSource);
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Booking System Database User Storage Provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(
                        CFG_DB_URL,
                        "Database URL",
                        "JDBC URL. Ví dụ: jdbc:postgresql://postgres:5432/booking_db",
                        ProviderConfigProperty.STRING_TYPE,
                        "jdbc:postgresql://postgres:5432/booking_db"
                ),
                new ProviderConfigProperty(
                        CFG_DB_USERNAME,
                        "Database Username",
                        "Username for connecting to the database",
                        ProviderConfigProperty.STRING_TYPE,
                        "booking_user"
                ),
                new ProviderConfigProperty(
                        CFG_DB_PASSWORD,
                        "Database Password",
                        "Password for connecting to the database",
                        ProviderConfigProperty.PASSWORD,
                        ""
                )
        );
    }

    private HikariDataSource initDataSource(ComponentModel componentModel) {
        String dbUrl = componentModel.get(CFG_DB_URL);
        String dbUsername = componentModel.get(CFG_DB_USERNAME);
        String dbPassword = componentModel.get(CFG_DB_PASSWORD);

        log.infof("[KC-DB] Initializing DataSource with URL: %s", dbUrl);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(dbUrl);
        cfg.setUsername(dbUsername);
        cfg.setPassword(dbPassword);
        cfg.setDriverClassName("org.postgresql.Driver");
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(5_000);
        cfg.setIdleTimeout(300_000);
        cfg.setPoolName("BookingKcPool");
        HikariDataSource ds = new HikariDataSource(cfg);
        log.info("[KC-DB] DataSource initialized successfully");
        return ds;
    }

}

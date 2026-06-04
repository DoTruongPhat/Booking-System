package com.booking.keycloak.provider;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    private static final Logger log = Logger.getLogger(
            BookingUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final DataSource dataSource;

    // Xóa Argon2, không cần nữa

    public BookingUserStorageProvider(
            KeycloakSession session,
            ComponentModel model,
            DataSource dataSource) {
        this.session = session;
        this.model = model;
        this.dataSource = dataSource;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm,
                                       String username) {
        log.infof("[KC-DB] getUserByUsername: %s", mask(username));
        return findUser("username", username, realm);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm,
                                    String email) {
        log.infof("[KC-DB] getUserByEmail: %s", maskEmail(email));
        return findUser("email", email, realm);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        log.infof("[KC-DB] getUserById: %s", externalId);
        return findUser("id::text", externalId, realm);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm,
                                   UserModel user,
                                   String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm,
                           UserModel userModel,
                           CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }
        String username = userModel.getUsername();
        String rawPassword = input.getChallengeResponse();
        log.infof("[KC-DB] Verify password for: %s", mask(username));
        try {
            return verifyPassword(username, rawPassword);
        } catch (Exception e) {
            log.errorf(e, "[KC-DB] Error verifying password: %s",
                    mask(username));
            return false;
        }
    }

    @Override
    public void close() {}

    private UserModel findUser(String field,
                               String value,
                               RealmModel realm) {
        String sql = "SELECT id::text, username, email, is_active " +
                "FROM auth.users " +
                "WHERE " + field + " = ? " +
                "AND is_active = true " +
                "AND is_locked = false";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BookingUserModel(
                            session, realm, model,
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getBoolean("is_active")
                    );
                }
            }
        } catch (Exception e) {
            log.errorf(e, "[KC-DB] Error finding user: %s", field);
        }
        return null;
    }

    private boolean verifyPassword(String username,
                                   String rawPassword)
            throws SQLException {
        String sql = "SELECT password_hash, password_salt " +
                "FROM auth.users " +
                "WHERE username = ? " +
                "AND is_active = true";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String storedSalt = rs.getString("password_salt");

                // Hash giống hệt BE:
                // SHA-256(SHA-256(password + username) + salt)
                String firstHash = sha256(rawPassword + username);
                String combined = sha256(firstHash + storedSalt);

                // Verify bằng BCrypt
                boolean valid = BCrypt.checkpw(combined, storedHash);

                if (valid) {
                    log.infof("[KC-DB] Password valid for: %s",
                            mask(username));
                } else {
                    log.warnf("[KC-DB] Password invalid for: %s",
                            mask(username));
                }
                return valid;
            }
        }
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    input.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder()
                    .encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private String mask(String input) {
        if (input == null || input.length() <= 2) return "****";
        return input.substring(0, 2) + "****";
    }

    private String maskEmail(String email) {
        if (email == null || email.length() <= 2) return "****";
        String[] p = email.split("@");
        return (p[0].length() > 2
                ? p[0].substring(0, 2) : p[0]) + "****@" + p[1];
    }
}
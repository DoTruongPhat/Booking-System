package com.booking.keycloak.provider;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * RemoteUserStorageProvider = KC SPI dùng HTTP API
 *
 * Khác với BookingUserStorageProvider:
 * → BookingUserStorageProvider: KC → JDBC → DB trực tiếp
 * → RemoteUserStorageProvider:  KC → HTTP API → Auth Service → DB
 *
 * Lợi ích:
 * → KC không cần biết DB schema
 * → Auth Service kiểm soát hoàn toàn logic verify
 * → Dễ thay đổi logic mà không cần rebuild SPI
 * → Bảo mật hơn (KC không cần DB credentials)
 */
public class RemoteUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {


    private static final Logger log = Logger.getLogger(
            RemoteUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;

    // URL của Auth Service API
    // Config trong KC Admin UI khi setup User Federation
    private final String authServiceUrl;

    // Internal key để bảo vệ API
    // Chỉ Keycloak biết key này
    private final String internalKey;

    public RemoteUserStorageProvider(
            KeycloakSession session,
            ComponentModel model,
            String authServiceUrl,
            String internalKey) {
        this.session = session;
        this.model = model;
        this.authServiceUrl = authServiceUrl;
        this.internalKey = internalKey;
    }

    // ── UserLookupProvider ────────────────────────────────────

    @Override
    public UserModel getUserByUsername(RealmModel realm,
                                       String username) {
        log.infof("[KC-Remote] getUserByUsername: %s", mask(username));

        // Gọi API để verify user tồn tại
        // → Dùng empty password để chỉ check existence
        // → Thực ra KC sẽ gọi isValid() sau
        // → Ở đây chỉ cần trả về UserModel để KC biết user tồn tại
        try {
            RemoteUserInfo info = fetchUserInfo(username);
            if (info != null) {
                return new BookingUserModel(
                        session, realm, model,
                        info.userId,
                        info.username,
                        info.email,
                        true
                );
            }
        } catch (Exception e) {
            log.errorf(e, "[KC-Remote] Error getting user: %s",
                    mask(username));
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        // Remote federation không support lookup by email
        // → KC sẽ dùng getUserByUsername thay thế
        return null;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        // KC gọi khi cần load user theo KC internal ID
        // → Không implement vì remote federation
        return null;
    }

    // ── CredentialInputValidator ──────────────────────────────

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
        String password = input.getChallengeResponse();

        log.infof("[KC-Remote] Verifying password for: %s",
                mask(username));

        try {
            // Gọi Auth Service API để verify password
            return verifyPassword(username, password);
        } catch (Exception e) {
            log.errorf(e, "[KC-Remote] Error verifying: %s",
                    mask(username));
            return false;
        }
    }

    @Override
    public void close() {}

    // ── Private methods ───────────────────────────────────────

    /**
     * Verify password qua HTTP API
     * POST /api/internal/users/verify
     */
    private boolean verifyPassword(String username,
                                   String password) throws Exception {
        String url = authServiceUrl + "/api/internal/users/verify";

        // Tạo JSON body thủ công (không dùng thư viện)
        // → KC SPI không có Jackson/Gson sẵn
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                escapeJson(username),
                escapeJson(password)
        );

        String response = httpPost(url, body);

        if (response == null) return false;

        // Parse response đơn giản
        // → Tìm "valid":true trong response JSON
        boolean valid = response.contains("\"valid\":true");

        if (valid) {
            log.infof("[KC-Remote] Password valid for: %s",
                    mask(username));
        } else {
            log.warnf("[KC-Remote] Password invalid for: %s",
                    mask(username));
        }
        return valid;
    }

    /**
     * Lấy user info từ API
     * → Dùng để build UserModel
     */
    private RemoteUserInfo fetchUserInfo(String username) throws Exception {
        // Gọi verify với empty password chỉ để lấy user info
        // → Nếu user tồn tại thì KC biết
        // → Password sẽ được verify sau bởi isValid()
        String url = authServiceUrl + "/api/internal/users/verify";
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                escapeJson(username), ""
        );

        String response = httpPost(url, body);
        if (response == null) return null;

        // Parse userId và email từ response
        String userId = extractJsonValue(response, "userId");
        String email = extractJsonValue(response, "email");

        if (userId == null || userId.isEmpty()) return null;

        RemoteUserInfo info = new RemoteUserInfo();
        info.userId = userId;
        info.username = username;
        info.email = email;
        return info;
    }

    /**
     * HTTP POST helper
     * → Gọi Auth Service API
     * → Gửi X-Internal-Key header
     */
    private String httpPost(String urlStr,
                            String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "application/json");
        // Gửi internal key → Auth Service verify
        conn.setRequestProperty("X-Internal-Key", internalKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000); // 5 giây timeout
        conn.setReadTimeout(5000);

        // Gửi body
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            log.warnf("[KC-Remote] API returned status: %d", status);
            return null;
        }

        // Đọc response
        try (java.io.InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(),
                    StandardCharsets.UTF_8);
        }
    }

    /**
     * Parse JSON value đơn giản
     * → Không dùng thư viện JSON
     */
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    /**
     * Escape special chars trong JSON string
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String mask(String input) {
        if (input == null || input.length() <= 2) return "****";
        return input.substring(0, 2) + "****";
    }

    /**
     * Simple DTO cho user info từ API
     */
    private static class RemoteUserInfo {
        String userId;
        String username;
        String email;
    }
}

package com.booking.keycloak.provider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test cho RemoteUserStorageProvider.
 *
 * Vì provider phụ thuộc nặng vào KeycloakSession mock, ta chỉ
 * test các helper pure-Java thông qua HttpServer built-in của JDK.
 *
 * Cover:
 *  - extractJsonValue: parse userId/email/roles từ JSON response
 *  - escapeJson: escape special chars trong password/username
 *  - HTTP GET: gọi endpoint thật, parse 200/404/401
 */
class RemoteUserStorageProviderTest {

    private HttpServer server;
    private int port;
    private final AtomicReference<String> receivedInternalKey =
            new AtomicReference<>();
    private final AtomicReference<String> receivedPath =
            new AtomicReference<>();
    private final AtomicReference<String> receivedMethod =
            new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    // ───────────────────────── extractJsonValue ─────────────────────────

    @Test
    @DisplayName("extractJsonValue: parse userId thành công")
    void extractJsonValue_userId() {
        String json = "{\"userId\":\"abc-123\","
                + "\"username\":\"alice\","
                + "\"email\":\"alice@example.com\"}";

        assertEquals("abc-123",
                RemoteUserStorageProvider.extractJsonValue(
                        json, "userId"));
        assertEquals("alice",
                RemoteUserStorageProvider.extractJsonValue(
                        json, "username"));
        assertEquals("alice@example.com",
                RemoteUserStorageProvider.extractJsonValue(
                        json, "email"));
    }

    @Test
    @DisplayName("extractJsonValue: trả null khi key không tồn tại")
    void extractJsonValue_missingKey() {
        String json = "{\"userId\":\"abc-123\"}";

        assertNull(RemoteUserStorageProvider.extractJsonValue(
                json, "nonexistent"));
    }

    @Test
    @DisplayName("extractJsonValue: trả null khi JSON rỗng")
    void extractJsonValue_emptyJson() {
        assertNull(RemoteUserStorageProvider.extractJsonValue(
                "", "userId"));
        assertNull(RemoteUserStorageProvider.extractJsonValue(
                null, "userId"));
    }

    // ───────────────────────── escapeJson ─────────────────────────

    @Test
    @DisplayName("escapeJson: escape special chars đúng")
    void escapeJson_specialChars() {
        // Khi escape phải có \\ trước "
        String result = RemoteUserStorageProvider.escapeJson(
                "a\"b\\c\nd");
        assertEquals("a\\\"b\\\\c\\nd", result);
    }

    @Test
    @DisplayName("escapeJson: input null → empty string")
    void escapeJson_nullInput() {
        assertEquals("",
                RemoteUserStorageProvider.escapeJson(null));
    }

    @Test
    @DisplayName("escapeJson: input thường giữ nguyên")
    void escapeJson_plainText() {
        assertEquals("alice",
                RemoteUserStorageProvider.escapeJson("alice"));
        assertEquals("p@ssw0rd!",
                RemoteUserStorageProvider.escapeJson("p@ssw0rd!"));
    }

    // ───────────────────────── HTTP GET (qua HttpServer) ─────────────────────────
    //
    // Test gián tiếp: build một HttpServer local, mount handler trả
    // JSON, rồi dùng reflection gọi private httpGet().
    //
    // Mục đích: cover 200 (parse body), 404 (return null),
    // 401 (return null), và verify X-Internal-Key header được gửi.

    @Test
    @DisplayName("httpGet: 200 + JSON body → trả về body")
    void httpGet_returns200Body() throws Exception {
        server.createContext("/api/internal/users/alice", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedInternalKey.set(
                    exchange.getRequestHeaders()
                            .getFirst("X-Internal-Key"));
            byte[] body = ("{\"userId\":\"u1\","
                    + "\"username\":\"alice\","
                    + "\"email\":\"a@x.com\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        String body = invokeHttpGet(
                "http://127.0.0.1:" + port
                        + "/api/internal/users/alice",
                "test-key");

        assertNotNull(body);
        assertEquals("u1",
                RemoteUserStorageProvider.extractJsonValue(
                        body, "userId"));
        assertEquals("GET", receivedMethod.get());
        assertEquals("/api/internal/users/alice",
                receivedPath.get());
        assertEquals("test-key", receivedInternalKey.get());
    }

    @Test
    @DisplayName("httpGet: 404 → trả null (user không tồn tại)")
    void httpGet_404_returnsNull() throws Exception {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        String body = invokeHttpGet(
                "http://127.0.0.1:" + port + "/missing", "k");
        assertNull(body);
    }

    @Test
    @DisplayName("httpGet: 401 → trả null (internal key sai)")
    void httpGet_401_returnsNull() throws Exception {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });

        String body = invokeHttpGet(
                "http://127.0.0.1:" + port + "/secret", "k");
        assertNull(body);
    }

    /**
     * Invoke private httpGet() bằng reflection.
     * Lý do: httpGet() là implementation detail; ta test
     * behavior thông qua public method, nhưng trong trường hợp
     * này public method cần KeycloakSession mock rất phức tạp.
     */
    private String invokeHttpGet(String url, String key)
            throws Exception {
        var provider = new RemoteUserStorageProvider(
                null, null, "http://unused", key);
        var method = RemoteUserStorageProvider.class
                .getDeclaredMethod("httpGet", String.class);
        method.setAccessible(true);
        return (String) method.invoke(provider, url);
    }
}

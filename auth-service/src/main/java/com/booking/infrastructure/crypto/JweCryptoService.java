package com.booking.infrastructure.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JweCryptoService
 * ─────────────────────────────────────────────────────────
 * JWE compact (RFC 7516) cho password encryption.
 * Format: header.encryptedKey.iv.ciphertext.authTag
 *
 * - alg: RSA-OAEP-256  (wrap CEK bằng RSA public key)
 * - enc: A256GCM        (encrypt payload bằng AES-256-GCM)
 *
 * Flow:
 *   FE generate ngẫu nhiên:
 *     - cek  = 32 bytes (AES-256 key)
 *     - iv   = 12 bytes (GCM nonce)
 *     - tag  = AES-GCM encrypt(password) → ciphertext + 16 byte tag
 *   FE wrap cek bằng RSA-OAEP-256 → encryptedKey
 *   FE gửi JWE compact string
 *
 *   BE parse 5 phần
 *   BE unwrap cek bằng RSA private key
 *   BE AES-GCM decrypt → password
 */
@Service
@Log4j2
public class JweCryptoService {

    public static final String ALGORITHM = "RSA-OAEP-256";
    private static final String ENC = "A256GCM";

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String BC_PROVIDER = "BC";

    private static final int CEK_BYTES = 32;   // 256 bit
    private static final int IV_BYTES = 12;    // 96 bit (GCM standard)
    private static final int GCM_TAG_BITS = 128;

    private static final OAEPParameterSpec OAEP_PARAMS =
            new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT);

    @Value("${app.rsa.private-key-pem:}")
    private String privateKeyPem;

    @Value("${app.rsa.public-key-pem:}")
    private String publicKeyPem;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final SecureRandom random = new SecureRandom();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("[JWE] BouncyCastle provider registered");
        }
    }

    @PostConstruct
    public void init() {
        try {
            if (privateKeyPem != null && !privateKeyPem.isBlank()
                    && publicKeyPem != null && !publicKeyPem.isBlank()) {
                this.privateKey = readPrivateKeyFromPem(privateKeyPem);
                this.publicKey = readPublicKeyFromPem(publicKeyPem);
                log.info("[JWE] Key pair loaded from PEM config");
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(
                        KEY_ALGORITHM, BC_PROVIDER);
                generator.initialize(KEY_SIZE);
                KeyPair pair = generator.generateKeyPair();
                this.privateKey = pair.getPrivate();
                this.publicKey = pair.getPublic();
                log.warn("[JWE] Generated ephemeral key pair (dev mode).");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize RSA keys", e);
        }
    }

    // ── Public API ────────────────────────────────────────

    public PublicKeyInfo getPublicKeyInfo() {
        String pem = convertPublicKeyToPem(publicKey);
        // Header template cho FE biết cách build JWE
        return new PublicKeyInfo(ALGORITHM, ENC, pem);
    }

    /**
     * Decrypt JWE compact string → raw password.
     * Format: header.encryptedKey.iv.ciphertext.authTag
     */
    public String decryptJwe(String jweCompact) {
        if (jweCompact == null || jweCompact.isBlank()) {
            throw new IllegalArgumentException("JWE must not be blank");
        }
        String[] parts = jweCompact.split("\\.");
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "Invalid JWE format. Expected 5 parts, got "
                            + parts.length);
        }
        try {
            // 1. Decrypt encryptedKey (RSA-OAEP-256) → CEK
            byte[] encryptedKey = base64UrlDecode(parts[1]);
            byte[] cek = rsaDecrypt(encryptedKey);

            // 2. Decrypt ciphertext (AES-256-GCM) → plaintext
            byte[] iv = base64UrlDecode(parts[2]);
            byte[] ciphertext = base64UrlDecode(parts[3]);
            byte[] authTag = base64UrlDecode(parts[4]);

            // GCM yêu cầu ciphertext + tag concat
            byte[] cipherWithTag = new byte[ciphertext.length + authTag.length];
            System.arraycopy(ciphertext, 0, cipherWithTag, 0, ciphertext.length);
            System.arraycopy(authTag, 0, cipherWithTag, ciphertext.length, authTag.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(
                    GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(cek, "AES"), gcmSpec);
            byte[] plain = cipher.doFinal(cipherWithTag);

            // Zero out CEK
            java.util.Arrays.fill(cek, (byte) 0);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[JWE] Decrypt failed: {}",
                    e.getClass().getSimpleName());
            throw new IllegalStateException(
                    "Failed to decrypt JWE", e);
        }
    }

    // ── Inner DTO ─────────────────────────────────────────

    public record PublicKeyInfo(
            String algorithm,
            String encryption,
            String publicKey
    ) {}

    // ── Helpers ───────────────────────────────────────────

    private byte[] rsaDecrypt(byte[] encryptedKey) throws Exception {
        Cipher cipher = Cipher.getInstance(
                RSA_TRANSFORMATION, BC_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAMS);
        return cipher.doFinal(encryptedKey);
    }

    private byte[] base64UrlDecode(String s) {
        // URL-safe → standard base64
        String standard = s.replace('-', '+').replace('_', '/');
        // Thêm padding nếu thiếu
        int pad = (4 - standard.length() % 4) % 4;
        standard += "====".substring(0, pad);
        return Base64.getDecoder().decode(standard);
    }

    private PrivateKey readPrivateKeyFromPem(String pem) throws Exception {
        String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        return KeyFactory.getInstance(KEY_ALGORITHM, BC_PROVIDER)
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey readPublicKeyFromPem(String pem) throws Exception {
        String body = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        return KeyFactory.getInstance(KEY_ALGORITHM, BC_PROVIDER)
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private String convertPublicKeyToPem(PublicKey key) {
        String base64 = Base64.getEncoder().encodeToString(
                key.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i,
                    Math.min(i + 64, base64.length()));
            sb.append("\n");
        }
        sb.append("-----END PUBLIC KEY-----");
        return sb.toString();
    }
}

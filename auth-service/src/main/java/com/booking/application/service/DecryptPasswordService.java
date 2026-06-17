package com.booking.application.service;

/**
 * DecryptPasswordService - decrypt JWE password từ FE
 *
 * Tại sao tách ra?
 * → Controller không cần biết "BouncyCastle là gì"
 * → Controller chỉ cần gọi: decryptPasswordService.decrypt(...)
 * → Đổi thư viện crypto → chỉ sửa implementation
 */
public interface DecryptPasswordService {

    /**
     * Decrypt JWE compact string → plaintext password
     * @param jweCompact format: header.encryptedKey.iv.ciphertext.authTag
     * @return plaintext password
     */
    String decrypt(String jweCompact);
}

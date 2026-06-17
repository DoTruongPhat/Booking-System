package com.booking.application.port.in;

import com.booking.infrastructure.crypto.JweCryptoService.PublicKeyInfo;

/**
 * Use case: lấy public key (PEM) để FE encrypt password
 */
public interface GetPublicKeyUseCase {
    PublicKeyInfo get();
}

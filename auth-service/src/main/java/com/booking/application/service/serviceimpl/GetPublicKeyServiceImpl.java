package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.GetPublicKeyUseCase;
import com.booking.infrastructure.crypto.JweCryptoService;
import com.booking.infrastructure.crypto.JweCryptoService.PublicKeyInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * GetPublicKeyServiceImpl = implement GetPublicKeyUseCase
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class GetPublicKeyServiceImpl implements GetPublicKeyUseCase {

    private final JweCryptoService jweCryptoService;

    @Override
    public PublicKeyInfo get() {
        log.info("[GetPublicKey] Public key requested");
        return jweCryptoService.getPublicKeyInfo();
    }
}

package com.booking.application.service.serviceimpl;

import com.booking.application.service.DecryptPasswordService;
import com.booking.infrastructure.crypto.JweCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * DecryptPasswordServiceImpl = implement DecryptPasswordService
 *
 * Vẫn dùng JweCryptoService (infrastructure) nhưng qua 1 lớp trung gian
 * → Controller inject interface, không inject JweCryptoService trực tiếp
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class DecryptPasswordServiceImpl implements DecryptPasswordService {

    // Vẫn cần JweCryptoService để làm việc thật
    private final JweCryptoService jweCryptoService;

    @Override
    public String decrypt(String jweCompact) {
        if (jweCompact == null || jweCompact.isBlank()) {
            throw new IllegalArgumentException("JWE must not be blank");
        }
        log.debug("[DecryptPassword] Decrypting JWE password");
        return jweCryptoService.decryptJwe(jweCompact);
    }
}

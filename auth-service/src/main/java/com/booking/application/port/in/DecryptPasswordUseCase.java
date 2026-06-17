package com.booking.application.port.in;

public interface DecryptPasswordUseCase {
    String decrypt(String jweCompact);
}

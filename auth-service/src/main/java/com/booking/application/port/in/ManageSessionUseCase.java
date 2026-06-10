package com.booking.application.port.in;

import java.util.UUID;

public interface ManageSessionUseCase {
    void revokeAllSessions(UUID userId);
    void revokeSession(String jti);
}

package com.booking.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface BlockRoomDatesUseCase {
    void blockDates(UUID roomId, LocalDate startDate, LocalDate endDate, UUID ownerUserId);
}
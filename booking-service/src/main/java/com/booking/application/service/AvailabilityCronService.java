package com.booking.application.service;

import com.booking.application.port.out.RoomAvailabilityRepositoryPort;
import com.booking.application.port.out.RoomRepositoryPort;
import com.booking.domain.enums.AvailabilityStatus;
import com.booking.domain.model.Room;
import com.booking.domain.model.RoomAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityCronService {

    private static final int ROLLING_WINDOW_DAYS = 30;

    private final RoomRepositoryPort roomRepository;
    private final RoomAvailabilityRepositoryPort availabilityRepository;

    /**
     * BR-CRON-003: extends availability window by 1 day for every AVAILABLE room,
     * keeping a rolling 30-day horizon. Runs daily at 00:00.
     *
     * Simple approach for this scale: fetch all rooms in pages, append tomorrow's
     * slot at (today + ROLLING_WINDOW_DAYS) if it doesn't already exist.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void extendAvailabilityWindow() {
        LocalDate targetDate = LocalDate.now().plusDays(ROLLING_WINDOW_DAYS);
        int page = 0;
        int pageSize = 100;
        int totalExtended = 0;

        List<Room> rooms;
        do {
            rooms = roomRepository.findAll(PageRequest.of(page, pageSize)).getContent();

            for (Room room : rooms) {
                if (!room.isAvailable()) continue;

                boolean exists = !availabilityRepository
                        .findByRoomIdAndDateRange(room.getId(), targetDate, targetDate.plusDays(1))
                        .isEmpty();

                if (!exists) {
                    RoomAvailability avail = new RoomAvailability();
                    avail.setRoomId(room.getId());
                    avail.setDate(targetDate);
                    avail.setAvailableCount(room.getTotalRooms());
                    avail.setStatus(AvailabilityStatus.AVAILABLE);
                    availabilityRepository.saveAll(List.of(avail));
                    totalExtended++;
                }
            }
            page++;
        } while (!rooms.isEmpty());

        log.info("Availability window extended: date={}, roomsExtended={}", targetDate, totalExtended);
    }
}
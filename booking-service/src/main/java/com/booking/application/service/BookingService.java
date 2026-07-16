package com.booking.application.service;

import com.booking.application.port.in.CancelBookingUseCase;
import com.booking.application.port.in.ConfirmBookingUseCase;
import com.booking.application.port.in.CreateBookingUseCase;
import com.booking.application.port.in.QueryBookingUseCase;
import com.booking.application.port.out.*;
import com.booking.domain.enums.BookingStatus;
import com.booking.domain.enums.CancelledBy;
import com.booking.domain.enums.PaymentStatus;
import com.booking.domain.event.BookingConfirmedEvent;
import com.booking.domain.event.CoreDomainEvent;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Booking;
import com.booking.domain.model.Hotel;
import com.booking.domain.model.Room;
import com.booking.domain.model.RoomAvailability;
import com.booking.infrastructure.cache.RoomCacheAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService implements CreateBookingUseCase, QueryBookingUseCase, CancelBookingUseCase, ConfirmBookingUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final RoomRepositoryPort roomRepository;
    private final HotelRepositoryPort hotelRepository;
    private final RoomAvailabilityRepositoryPort availabilityRepository;
    private final BookingEventPublisherPort eventPublisher;
    private final RoomCacheAdapter roomCacheAdapter;
    private final AuditEventPort auditEventPort;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.booking-confirmed:booking-confirmed-events}")
    private String bookingConfirmedTopic;

    // ─── CreateBookingUseCase ────────────────

    @Override
    public Booking createBooking(BookingCommand command, UUID userId) {

        // 1. Load & validate room + hotel
        Room room = roomRepository.findById(command.roomId())
                .orElseThrow(() -> new CoreException(CoreErrorCode.ROOM_NOT_FOUND));

        if (!room.isAvailable()) {
            throw new CoreException(CoreErrorCode.ROOM_NOT_AVAILABLE);
        }

        Hotel hotel = hotelRepository.findById(room.getHotelId())
                .orElseThrow(() -> new CoreException(CoreErrorCode.HOTEL_NOT_FOUND));

        if (!hotel.isActive()) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_ACTIVE);
        }

        // 2. Validate dates
        LocalDate checkIn = command.checkInDate();
        LocalDate checkOut = command.checkOutDate();
        validateDates(checkIn, checkOut);

        // 3. Validate capacity (BR-BOOK-008)
        if (!room.canAccommodate(command.numGuests(), command.numRooms())) {
            throw new CoreException(CoreErrorCode.BOOKING_GUEST_EXCEEDS_CAPACITY);
        }

        // 4. Check duplicate booking (BR-BOOK-018)
        if (bookingRepository.existsOverlapping(userId, command.roomId(), checkIn, checkOut)) {
            throw new CoreException(CoreErrorCode.BOOKING_DUPLICATE,
                    "You already have a booking for this room with overlapping dates");
        }

        // 5. Pessimistic lock + check availability (BR-BOOK-010)
        int numNights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        List<RoomAvailability> locked = availabilityRepository
                .findByRoomIdAndDateRangeForUpdate(command.roomId(), checkIn, checkOut);

        if (locked.size() != numNights) {
            throw new CoreException(CoreErrorCode.AVAILABILITY_NOT_FOUND);
        }

        for (RoomAvailability avail : locked) {
            if (!avail.hasEnoughRooms(command.numRooms())) {
                throw new CoreException(CoreErrorCode.AVAILABILITY_INSUFFICIENT,
                        "Not enough rooms on " + avail.getDate() + " (available: "
                                + avail.getAvailableCount() + ", requested: " + command.numRooms() + ")");
            }
        }

        // 6. Decrement availability (BR-AVL-009)
        availabilityRepository.decrementAvailability(
                command.roomId(), checkIn, checkOut, command.numRooms());

        // 7. Calculate price snapshot (BR-PRICE-001→004)
        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;
        for (RoomAvailability avail : locked) {
            totalBeforeDiscount = totalBeforeDiscount.add(avail.getEffectivePrice());
        }
        BigDecimal unitPrice = totalBeforeDiscount.divide(
                BigDecimal.valueOf(numNights), 2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = totalBeforeDiscount
                .multiply(BigDecimal.valueOf(command.numRooms()));

        // 8. Generate booking code (BR-BOOK-021)
        String bookingCode = generateBookingCode();

        // 9. Build & save booking (BR-BOOK-017: status=PENDING)
        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setUserId(userId);
        booking.setRoomId(command.roomId());
        booking.setHotelId(hotel.getId());
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setNumNights(numNights);
        booking.setNumGuests(command.numGuests());
        booking.setNumRooms(command.numRooms());
        booking.setUnitPrice(unitPrice);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setSpecialRequest(command.specialRequest());
        booking.setGuestName(command.guestName());
        booking.setGuestEmail(command.guestEmail());
        booking.setGuestPhone(command.guestPhone());


        Booking saved = bookingRepository.save(booking);

        log.info("Booking created: code={}, user={}, room={}, hotel={}, dates={}/{}, nights={}, total={}",
                bookingCode, userId, command.roomId(), hotel.getId(),
                checkIn, checkOut, numNights, totalPrice);

        eventPublisher.publishBookingCreated(new CoreDomainEvent.BookingCreated(
                saved.getId(), bookingCode, userId, command.roomId(), hotel.getId(),
                checkIn, checkOut, command.numRooms(), totalPrice, Instant.now()
        ));

        roomCacheAdapter.invalidateOnBookingChange(command.roomId());
        auditEventPort.publish(AuditEventPort.AuditEvent.bookingCreate(
                saved.getId().toString(),
                userId.toString(),
                "USER",
                command.roomId().toString(),
                hotel.getName()
        ));

        return saved;
    }

    // ─── CancelBookingUseCase ────────────────

    @Override
    public Booking cancelBooking(UUID bookingId, UUID requesterId, CancelledBy cancelledBy, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.BOOKING_NOT_FOUND));

        // Ownership check (only for USER-initiated cancel)
        if (cancelledBy == CancelledBy.USER && !booking.isOwnedBy(requesterId)) {
            throw new CoreException(CoreErrorCode.BOOKING_NOT_OWNED);
        }

        // State check (BR-CANCEL-004, BR-STATE-012): only PENDING or CONFIRMED
        if (!booking.isCancellable()) {
            throw new CoreException(CoreErrorCode.BOOKING_CANNOT_CANCEL,
                    "Booking in status " + booking.getStatus() + " cannot be cancelled");
        }

        // Calculate refund based on cancellation policy (BR-CANCEL-001→003)
        BigDecimal refundAmount = calculateRefund(booking, cancelledBy);

        // Restore availability (BR-CANCEL-006, BR-AVL-010)
        availabilityRepository.incrementAvailability(
                booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate(),
                booking.getNumRooms());

        // Update booking state
        booking.cancel(cancelledBy, reason);
        booking.setRefundAmount(refundAmount);

        // Update payment status only if it was paid (BR-CANCEL-007)
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            if (refundAmount.compareTo(booking.getTotalPrice()) == 0) {
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
            } else if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                booking.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            // refundAmount = 0 → payment stays PAID (no refund, per policy)
        }

        Booking saved = bookingRepository.save(booking);

        log.info("Booking cancelled: code={}, by={}, refund={}, reason={}",
                saved.getBookingCode(), cancelledBy, refundAmount, reason);

        eventPublisher.publishBookingCancelled(new CoreDomainEvent.BookingCancelled(
                saved.getId(), saved.getBookingCode(), saved.getUserId(), saved.getRoomId(),
                saved.getCheckInDate(), saved.getCheckOutDate(), saved.getNumRooms(),
                cancelledBy.name(), Instant.now()
        ));
        roomCacheAdapter.invalidateOnBookingChange(saved.getRoomId());
        auditEventPort.publish(AuditEventPort.AuditEvent.bookingCancel(
                saved.getId().toString(),
                requesterId.toString(),
                cancelledBy.name(),
                reason,
                cancelledBy.name()
        ));

        return saved;
    }

    /**
     * BR-CANCEL-001→003: refund tiered by hours until check-in.
     * HOST/ADMIN force cancel → always 100% refund (BR-CANCEL-011, 012 imply full refund for host-fault cancels).
     */
    private BigDecimal calculateRefund(Booking booking, CancelledBy cancelledBy) {
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            return BigDecimal.ZERO; // nothing was paid, nothing to refund
        }

        if (cancelledBy == CancelledBy.HOST || cancelledBy == CancelledBy.SYSTEM) {
            return booking.getTotalPrice(); // 100% — not the guest's fault
        }

        long hoursUntilCheckIn = Duration.between(
                Instant.now(), booking.getCheckInDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        ).toHours();

        BigDecimal refundPercent;
        if (hoursUntilCheckIn >= 48) {
            refundPercent = BigDecimal.ONE; // 100%
        } else if (hoursUntilCheckIn >= 24) {
            refundPercent = new BigDecimal("0.5"); // 50%
        } else {
            refundPercent = BigDecimal.ZERO; // 0%
        }

        return booking.getTotalPrice().multiply(refundPercent).setScale(2, RoundingMode.HALF_UP);
    }

    // ─── QueryBookingUseCase ─────────────────

    @Override
    @Transactional(readOnly = true)
    public Booking getById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.BOOKING_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> getByUserId(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> getByHotelId(UUID hotelId, Pageable pageable) {
        return bookingRepository.findByHotelId(hotelId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> getByHotelId(UUID hotelId, String status, Pageable pageable) {
        return bookingRepository.findByHotelId(hotelId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> getAll(String status, Pageable pageable) {
        return bookingRepository.findAll(status, pageable);
    }

    // ─── Private helpers ─────────────────────

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new CoreException(CoreErrorCode.BOOKING_INVALID_DATES);
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new CoreException(CoreErrorCode.BOOKING_PAST_CHECKIN);
        }
        if (checkIn.isAfter(LocalDate.now().plusDays(365))) {
            throw new CoreException(CoreErrorCode.BOOKING_TOO_FAR_AHEAD);
        }
    }

    private String generateBookingCode() {
        String prefix = "BK-" + LocalDate.now().toString().replace("-", "");
        long count = bookingRepository.countByBookingCodePrefix(prefix);
        return String.format("%s-%03d", prefix, count + 1);
    }

    @Override
    public Booking confirmBooking(UUID bookingId, UUID confirmedByUserId) {

        // 1. Load booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.BOOKING_NOT_FOUND));

        // 2. Validate status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new CoreException(CoreErrorCode.BOOKING_INVALID_STATUS,
                    "Only PENDING bookings can be confirmed. Current: " + booking.getStatus());
        }

        // 3. Confirm
        booking.confirm();
        Booking saved = bookingRepository.save(booking);

        log.info("Booking confirmed: code={}, by={}", booking.getBookingCode(), confirmedByUserId);

        // 4. Publish event → Kafka → ReportGenerationConsumer → PDF + Email
        eventPublisher.publishBookingConfirmed(new CoreDomainEvent.BookingConfirmed(
                saved.getId(), saved.getBookingCode(), Instant.now()
        ));

        publishBookingConfirmedEvent(saved);

        // 5. Audit
        auditEventPort.publish(AuditEventPort.AuditEvent.bookingCreate(
                saved.getId().toString(),
                confirmedByUserId.toString(),
                "CONFIRM",
                saved.getRoomId().toString(),
                "Booking confirmed"
        ));

        return saved;
    }

    private void publishBookingConfirmedEvent(Booking booking) {
        BookingConfirmedEvent event = BookingConfirmedEvent.of(
                booking.getId(),
                booking.getBookingCode(),
                booking.getGuestEmail()
        );
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(bookingConfirmedTopic, booking.getId().toString(), payload);
            log.info("Published BookingConfirmedEvent: {}", booking.getBookingCode());
        } catch (Exception e) {
            log.error("Failed to publish BookingConfirmedEvent — email will not be sent", e);
            // Fail-open: booking vẫn confirmed, chỉ email không gửi
        }
    }
}

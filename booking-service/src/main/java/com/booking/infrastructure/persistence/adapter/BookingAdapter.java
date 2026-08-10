package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.domain.enums.BookingStatus;
import com.booking.domain.model.Booking;
import com.booking.infrastructure.persistence.entity.BookingEntity;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.RoomEntity;
import com.booking.infrastructure.persistence.mapper.BookingMapper;
import com.booking.infrastructure.persistence.repository.BookingJpaRepository;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import com.booking.infrastructure.persistence.repository.RoomJpaRepository;
import com.booking.infrastructure.report.BookingExportFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingAdapter implements BookingRepositoryPort {

    private final BookingJpaRepository bookingJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    private final HotelJpaRepository hotelJpaRepository;
    private final BookingMapper mapper;

    @Override
    public Booking save(Booking booking) {
        BookingEntity entity;

        if (booking.getId() != null) {
            entity = bookingJpaRepository.findById(booking.getId()).orElse(null);
            if (entity != null) {
                mapper.updateEntity(entity, booking);
            } else {
                entity = buildNewEntity(booking);
            }
        } else {
            entity = buildNewEntity(booking);
        }

        BookingEntity saved = bookingJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return bookingJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Booking> findByUserId(UUID userId, Pageable pageable) {
        return bookingJpaRepository.findByUserId(userId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Booking> findByUserId(UUID userId, String status, Pageable pageable) {
        return bookingJpaRepository.findByUserIdAndStatus(userId, status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Booking> findByHotelId(UUID hotelId, Pageable pageable) {
        return bookingJpaRepository.findByHotelId(hotelId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Booking> findByHotelId(UUID hotelId, String status, Pageable pageable) {
        return bookingJpaRepository.findByHotelIdAndStatus(hotelId, status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Booking> findByOwnerUserId(UUID ownerUserId, String status, Pageable pageable) {
        return bookingJpaRepository.findByOwnerUserIdAndStatus(ownerUserId, status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Booking> findAll(String status, Pageable pageable) {
        return bookingJpaRepository.findAllByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsOverlapping(UUID userId, UUID roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingJpaRepository.existsOverlapping(userId, roomId, checkIn, checkOut);
    }

    @Override
    public long countByBookingCodePrefix(String prefix) {
        return bookingJpaRepository.countByBookingCodePrefix(prefix);
    }

    @Override
    public List<Booking> findExpiredPending(Instant cutoff) {
        return bookingJpaRepository.findExpiredPending(cutoff)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Booking> findNoShowCandidates(LocalDate checkInCutoff) {
        return bookingJpaRepository.findNoShowCandidates(checkInCutoff)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countActiveBookingsForRoom(UUID roomId, LocalDate today) {
        return bookingJpaRepository.countActiveBookingsForRoom(roomId, today);
    }

    private BookingEntity buildNewEntity(Booking booking) {
        RoomEntity roomEntity = roomJpaRepository.getReferenceById(booking.getRoomId());
        HotelEntity hotelEntity = hotelJpaRepository.getReferenceById(booking.getHotelId());
        return mapper.toEntity(booking, roomEntity, hotelEntity);
    }

    @Override
    public List<Booking> findByFilter(BookingExportFilter filter) {
        Specification<BookingEntity> spec = (root, query, cb) -> cb.conjunction();

        if (filter.from() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.greaterThanOrEqualTo(root.get("checkInDate"), filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.lessThanOrEqualTo(root.get("checkInDate"), filter.to()));
        }
        if (filter.hotelId() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(root.get("hotel").get("id"), filter.hotelId()));
        }
        if (filter.ownerUserId() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(root.get("hotel").get("ownerUserId"), filter.ownerUserId()));
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(root.get("status"), BookingStatus.valueOf(filter.status())));
        }

        return bookingJpaRepository.findAll(spec).stream()
                .map(mapper::toDomain)
                .toList();
    }




}

package com.booking.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

import static com.booking.infrastructure.cache.config.RedisCacheConfig.*;

/**
 * Cache adapter for room-related data.
 *
 * Cache strategy:
 * - Search results: TTL 1 min, invalidated on booking/cancel/block
 * - Room availability: TTL 5 min, invalidated on booking/cancel/block
 * - Room detail: TTL 10 min, invalidated on room update
 * - Hotel detail: TTL 10 min, invalidated on hotel update
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomCacheAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    // ──────────────────────────────────────────────
    // Invalidation methods
    // ──────────────────────────────────────────────

    /**
     * Invalidate all search result caches.
     * Called when any booking/cancellation/block changes room availability.
     */
    public void invalidateSearchResults() {
        evictByPattern(CACHE_SEARCH_RESULTS + "::*");
        log.debug("[Cache] Invalidated all search results");
    }

    /**
     * Invalidate availability cache for a specific room.
     */
    public void invalidateRoomAvailability(UUID roomId) {
        evictByPattern(CACHE_ROOM_AVAILABILITY + "::*roomId=" + roomId + "*");
        // Also evict room detail since availability changed
        evictByPattern(CACHE_ROOM_DETAIL + "::detail:roomId=" + roomId);
        log.debug("[Cache] Invalidated availability for room {}", roomId);
    }

    /**
     * Invalidate all availability-related caches.
     * Called when a booking/cancellation affects multiple rooms.
     */
    public void invalidateAllAvailability() {
        evictByPattern(CACHE_ROOM_AVAILABILITY + "::*");
        evictByPattern(CACHE_SEARCH_RESULTS + "::*");
        log.debug("[Cache] Invalidated all availability caches");
    }

    /**
     * Invalidate room detail cache.
     * Called when room is updated.
     */
    public void invalidateRoomDetail(UUID roomId) {
        evictByPattern(CACHE_ROOM_DETAIL + "::detail:roomId=" + roomId);
        log.debug("[Cache] Invalidated detail for room {}", roomId);
    }

    /**
     * Invalidate hotel detail cache.
     * Called when hotel is updated or approved.
     */
    public void invalidateHotelDetail(UUID hotelId) {
        evictByPattern(CACHE_HOTEL_DETAIL + "::detail:hotelId=" + hotelId);
        log.debug("[Cache] Invalidated detail for hotel {}", hotelId);
    }

    /**
     * Invalidate everything after booking/cancel/block.
     * This is the safe "nuke everything" approach.
     */
    public void invalidateOnBookingChange(UUID roomId) {
        invalidateRoomAvailability(roomId);
        invalidateSearchResults();
        log.info("[Cache] Full invalidation after booking change for room {}", roomId);
    }

    // ──────────────────────────────────────────────
    // Internal
    // ──────────────────────────────────────────────

    private void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("[Cache] Evicted {} keys matching {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("[Cache] Failed to evict keys matching {}: {}", pattern, e.getMessage());
            // Fail-open: cache miss is acceptable, stale data is not critical
        }
    }
}

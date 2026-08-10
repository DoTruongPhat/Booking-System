package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.in.SearchRoomUseCase.RoomSearchResult;
import com.booking.application.port.in.SearchRoomUseCase.SearchCriteria;
import com.booking.application.port.out.RoomSearchPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoomSearchAdapter implements RoomSearchPort {

    private final EntityManager entityManager;

    @Override
    public Page<RoomSearchResult> search(SearchCriteria criteria, Pageable pageable) {

        long totalDays = ChronoUnit.DAYS.between(criteria.checkIn(), criteria.checkOut());

        // Build WHERE clause dynamically
        StringBuilder where = buildWhereClause(criteria);
        Map<String, Object> params = buildParams(criteria, totalDays);

        // Count query
        String countSql = """
                SELECT COUNT(*) FROM (
                    SELECT r.id
                    FROM booking.rooms r
                    JOIN booking.hotels h ON r.hotel_id = h.id
                    JOIN booking.room_availability ra ON ra.room_id = r.id
                    """ + where + """
                    GROUP BY r.id
                    HAVING COUNT(ra.id) = :totalDays
                       AND MIN(ra.available_count) >= 1
                    """ + buildHavingFilters(criteria) + """
                ) sub
                """;

        // Data query — default sort: rating DESC (BR-SEARCH-009)
        String dataSql = """
                SELECT r.id AS room_id,
                       h.id AS hotel_id,
                       h.name AS hotel_name,
                       h.city AS hotel_city,
                       r.name AS room_name,
                       r.room_type,
                       r.capacity,
                       r.total_rooms,
                       MIN(COALESCE(ra.price_override, r.base_price)) AS min_price,
                       r.base_price,
                       r.amenities AS room_amenities,
                       h.amenities AS hotel_amenities,
                       r.images AS room_images,
                       h.rating AS hotel_rating
                FROM booking.rooms r
                JOIN booking.hotels h ON r.hotel_id = h.id
                JOIN booking.room_availability ra ON ra.room_id = r.id
                """ + where + """
                GROUP BY r.id, h.id, h.name, h.city, r.name, r.room_type,
                         r.capacity, r.total_rooms, r.base_price,
                         r.amenities, h.amenities, r.images, h.rating
                HAVING COUNT(ra.id) = :totalDays
                   AND MIN(ra.available_count) >= 1
                """ + buildHavingFilters(criteria) + """
                ORDER BY h.rating DESC, min_price ASC
                """;

        // Execute count
        Query countQuery = entityManager.createNativeQuery(countSql);
        setParams(countQuery, params);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Execute data with pagination
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        setParams(dataQuery, params);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();

        List<RoomSearchResult> results = rows.stream()
                .map(this::mapRow)
                .toList();

        return new PageImpl<>(results, pageable, total);
    }

    // ─── Query builders ──────────────────────

    private StringBuilder buildWhereClause(SearchCriteria criteria) {
        StringBuilder where = new StringBuilder();
        where.append("WHERE LOWER(h.city) LIKE LOWER(:city) ");
        where.append("AND h.status = 'ACTIVE' ");
        where.append("AND r.status = 'AVAILABLE' ");
        where.append("AND ra.date >= :checkIn AND ra.date < :checkOut ");
        where.append("AND ra.status = 'AVAILABLE' ");
        where.append("AND r.capacity >= :guests ");

        if (criteria.minRating() != null) {
            where.append("AND h.rating >= :minRating ");
        }

        return where;
    }

    private String buildHavingFilters(SearchCriteria criteria) {
        StringBuilder having = new StringBuilder();

        if (criteria.minPrice() != null) {
            having.append("AND MIN(COALESCE(ra.price_override, r.base_price)) >= :minPrice ");
        }
        if (criteria.maxPrice() != null) {
            having.append("AND MIN(COALESCE(ra.price_override, r.base_price)) <= :maxPrice ");
        }

        return having.toString();
    }

    private Map<String, Object> buildParams(SearchCriteria criteria, long totalDays) {
        Map<String, Object> params = new HashMap<>();
        params.put("city", "%" + (criteria.city() == null ? "" : criteria.city()) + "%");
        params.put("checkIn", criteria.checkIn());
        params.put("checkOut", criteria.checkOut());
        params.put("guests", criteria.guests());
        params.put("totalDays", totalDays);

        if (criteria.minRating() != null) {
            params.put("minRating", criteria.minRating());
        }
        if (criteria.minPrice() != null) {
            params.put("minPrice", criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            params.put("maxPrice", criteria.maxPrice());
        }

        return params;
    }

    private void setParams(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    // ─── Row mapper ─────────────────────────

    private RoomSearchResult mapRow(Object[] row) {
        return new RoomSearchResult(
                (UUID) row[0],
                (UUID) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                ((Number) row[6]).intValue(),
                ((Number) row[7]).intValue(),
                (BigDecimal) row[8],
                (BigDecimal) row[9],
                parseJsonbArray(row[10]),
                parseJsonbArray(row[11]),
                parseJsonbArray(row[12]),
                (BigDecimal) row[13]
        );
    }

    private List<String> parseJsonbArray(Object value) {
        String jsonb = value != null ? value.toString() : null;
        if (jsonb == null || jsonb.isBlank() || "[]".equals(jsonb)) {
            return List.of();
        }
        String cleaned = jsonb.replaceAll("[\\[\\]\"]", "");
        return Arrays.asList(cleaned.split(","));
    }
}

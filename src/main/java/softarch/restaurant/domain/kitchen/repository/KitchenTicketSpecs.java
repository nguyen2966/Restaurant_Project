package softarch.restaurant.domain.kitchen.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import softarch.restaurant.domain.kitchen.entity.KitchenTicket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications cho KitchenTicket.
 * Tránh lỗi PostgreSQL "could not determine data type of parameter $N"
 * bằng cách chỉ thêm predicate khi param != null.
 */
public class KitchenTicketSpecs {

    private KitchenTicketSpecs() {}

    /**
     * Build WHERE clause động từ filter params.
     *
     * @param status    filter theo currentState (QUEUED/COOKING/READY/PAUSED)
     * @param stationId filter theo station FK (null = tất cả stations)
     * @param deadline  chỉ lấy tickets có deadlineTime <= deadline
     */
    public static Specification<KitchenTicket> withFilters(
            String status,
            Long stationId,
            LocalDateTime deadline) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("statusEnumValue"), status));
            }

            // Join sang station để filter theo station_id
            if (stationId != null) {
                predicates.add(cb.equal(
                    root.get("assignedStation").get("id"), stationId));
            }

            if (deadline != null) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("deadlineTime"), deadline));
            }

            query.orderBy(cb.asc(root.get("deadlineTime")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter theo danh sách stationIds (multi-station IN query).
     */
    public static Specification<KitchenTicket> withStationIds(List<Long> stationIds) {
        return (root, query, cb) ->
            root.get("assignedStation").get("id").in(stationIds);
    }
}
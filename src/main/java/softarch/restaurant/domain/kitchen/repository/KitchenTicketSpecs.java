package softarch.restaurant.domain.kitchen.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import softarch.restaurant.domain.kitchen.entity.KitchenTicket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications cho KitchenTicket.
 *
 * Giải quyết lỗi PostgreSQL:
 *   "could not determine data type of parameter $N"
 *
 * Root cause: JPQL "(:param IS NULL OR col <= :param)" với LocalDateTime param
 * bị PostgreSQL JDBC driver không thể infer kiểu khi param = null.
 * Specification xây dựng WHERE clause động — chỉ thêm predicate khi param != null,
 * nên không bao giờ truyền null vào prepared statement.
 */
public class KitchenTicketSpecs {

    private KitchenTicketSpecs() {}

    /**
     * Build Specification từ các filter parameters.
     * Chỉ thêm condition vào WHERE khi param khác null.
     *
     * @param status   filter theo currentState (e.g. "QUEUED", "COOKING")
     * @param station  filter theo kitchen station (e.g. "GRILL")
     * @param deadline chỉ trả về tickets có expectedCompletionTime <= deadline
     */
    public static Specification<KitchenTicket> withFilters(
            String status,
            String station,
            LocalDateTime deadline) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ thêm predicate khi param != null — không bao giờ truyền null cho PostgreSQL
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("statusEnumValue"), status));
            }

            if (station != null && !station.isBlank()) {
                predicates.add(cb.equal(root.get("station"), station));
            }

            if (deadline != null) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("deadlineTime"), deadline));
            }

            // Mặc định sort theo deadlineTime ASC (tickets gần hết giờ lên đầu)
            query.orderBy(cb.asc(root.get("deadlineTime")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

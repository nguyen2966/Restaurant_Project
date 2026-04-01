package softarch.restaurant.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.order.entity.OrderItem;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Returns true if the given menu item appears in any order that is not
     * CANCELLED or PAID — used by MenuService.isItemInActiveOrder().
     */
    @Query("""
        SELECT COUNT(oi) > 0
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.menuItemId = :menuItemId
          AND o.status NOT IN ('CANCELLED', 'PAID')
        """)
    boolean existsInActiveOrder(Long menuItemId);
}
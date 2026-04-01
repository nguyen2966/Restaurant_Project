package softarch.restaurant.domain.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByStatus(ItemStatus status);

    List<MenuItem> findByNameContainingIgnoreCase(String name);

    List<MenuItem> findByStatusAndNameContainingIgnoreCase(ItemStatus status, String name);

    /**
     * Best sellers — joins with order_item to count total quantity sold.
     * Returns up to 10 items sorted by times ordered descending.
     */
    @Query("""
        SELECT m FROM MenuItem m
        JOIN OrderItem oi ON oi.menuItemId = m.id
        GROUP BY m.id
        ORDER BY SUM(oi.quantity) DESC
        LIMIT 10
        """)
    List<MenuItem> findBestSellers();

    /**
     * Used by MenuServiceImpl to check active status of multiple items at once
     * (called by OrderingFacade before placing an order).
     */
    @Query("SELECT m FROM MenuItem m WHERE m.id IN :ids AND m.status = 'AVAILABLE'")
    List<MenuItem> findAvailableByIds(List<Long> ids);
}
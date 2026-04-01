package softarch.restaurant.domain.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<MenuItem, Long> {

    // Matches diagram: findByStatus(ItemStatus s)
    List<MenuItem> findByStatus(ItemStatus status);

    // Matches diagram: findByNameContaining(String n)
    List<MenuItem> findByNameContainingIgnoreCase(String name);

    List<MenuItem> findByStatusAndNameContainingIgnoreCase(ItemStatus status, String name);

    // Used by validateItemsActive — only returns ACTIVE items
    @Query("SELECT m FROM MenuItem m WHERE m.id IN :ids AND m.status = 'ACTIVE'")
    List<MenuItem> findActiveByIds(List<Long> ids);

    // Best sellers via join with order_item aggregate
    @Query(value = """
        SELECT m.* FROM menu_item m
        JOIN order_item oi ON oi.menu_item_id = m.id
        JOIN orders o ON o.id = oi.order_id
        WHERE o.status = 'PAID'
        GROUP BY m.id
        ORDER BY SUM(oi.quantity) DESC
        LIMIT 10
        """, nativeQuery = true)
    List<MenuItem> findBestSellers();
}
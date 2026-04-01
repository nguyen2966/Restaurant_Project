package softarch.restaurant.domain.promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.promotion.entity.PromoItem;
import softarch.restaurant.domain.promotion.entity.PromoStatus;

import java.util.List;

@Repository
public interface PromoRepository extends JpaRepository<PromoItem, Long> {

    // Matches diagram: findByStatus(PromoStatus s)
    List<PromoItem> findByStatus(PromoStatus status);

    // Matches diagram: findByNameContaining(String n)
    List<PromoItem> findByNameContainingIgnoreCase(String name);

    /**
     * Finds all ACTIVE promos that include a specific menu item.
     * Used by PromoService.isItemInActivePromo().
     */
    @Query("""
        SELECT p FROM PromoItem p
        WHERE p.status = 'ACTIVE'
          AND :menuItemId MEMBER OF p.menuItemIds
        """)
    List<PromoItem> findActiveByMenuItemId(Long menuItemId);
}
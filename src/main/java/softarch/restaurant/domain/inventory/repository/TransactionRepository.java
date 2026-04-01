package softarch.restaurant.domain.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.inventory.entity.InventoryTransaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByIngredientIdAndTimestampBetween(
        Long ingredientId, LocalDateTime from, LocalDateTime to);
}
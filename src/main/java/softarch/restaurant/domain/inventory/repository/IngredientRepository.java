package softarch.restaurant.domain.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.inventory.entity.Ingredient;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    /** Returns all ingredients where current_stock <= min_threshold */
    @Query("SELECT i FROM Ingredient i WHERE i.currentStock <= i.minThreshold")
    List<Ingredient> findLowStockIngredients();
}
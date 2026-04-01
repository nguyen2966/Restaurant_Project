package softarch.restaurant.domain.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.inventory.entity.RecipeItem;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeItem, Long> {

    List<RecipeItem> findByMenuItemId(Long menuItemId);
}
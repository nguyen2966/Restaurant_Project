package softarch.restaurant.domain.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Maps a MenuItem to the ingredients it consumes when prepared.
 * Used by InventoryService to auto-deduct stock when an order item is completed.
 */
@Entity
@Table(name = "recipe_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"menu_item_id", "ingredient_id"}))
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "required_amount", nullable = false, precision = 10, scale = 3)
    private BigDecimal requiredAmount;

    protected RecipeItem() {}

    public Long getId()                  { return id; }
    public Long getMenuItemId()          { return menuItemId; }
    public Long getIngredientId()        { return ingredientId; }
    public BigDecimal getRequiredAmount() { return requiredAmount; }
}
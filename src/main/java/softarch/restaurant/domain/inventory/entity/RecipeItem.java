package softarch.restaurant.domain.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Maps a MenuItem to the ingredients it consumes when one portion is prepared.
 *
 * Used by InventoryService.autoDeductForMenuItem() to know how much of each
 * ingredient to deduct when a KitchenTicket transitions to DONE.
 *
 * Updated: added public constructor and updateAmount() for RecipeController.
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

    /**
     * Public constructor — used by RecipeController when creating new lines.
     */
    public RecipeItem(Long menuItemId, Long ingredientId, BigDecimal requiredAmount) {
        this.menuItemId      = menuItemId;
        this.ingredientId    = ingredientId;
        this.requiredAmount  = requiredAmount;
    }

    /**
     * Updates the required amount — used by PUT /api/menu/{id}/recipe/{lineId}.
     */
    public void updateAmount(BigDecimal newAmount) {
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("requiredAmount must be positive");
        }
        this.requiredAmount = newAmount;
    }

    public Long getId()                  { return id; }
    public Long getMenuItemId()          { return menuItemId; }
    public Long getIngredientId()        { return ingredientId; }
    public BigDecimal getRequiredAmount() { return requiredAmount; }
}
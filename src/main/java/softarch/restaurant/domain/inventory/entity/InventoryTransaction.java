package softarch.restaurant.domain.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    /** Positive = restock, Negative = usage/deduction */
    @Column(name = "amount_changed", nullable = false, precision = 10, scale = 3)
    private BigDecimal amountChanged;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsageReason reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    protected InventoryTransaction() {}

    public InventoryTransaction(Long ingredientId, BigDecimal amountChanged,
                                UsageReason reason, Long createdByUserId) {
        this.ingredientId = ingredientId;
        this.amountChanged = amountChanged;
        this.reason = reason;
        this.createdByUserId = createdByUserId;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId()                  { return id; }
    public Long getIngredientId()        { return ingredientId; }
    public BigDecimal getAmountChanged() { return amountChanged; }
    public UsageReason getReason()       { return reason; }
    public LocalDateTime getTimestamp()  { return timestamp; }
    public Long getCreatedByUserId()     { return createdByUserId; }
}
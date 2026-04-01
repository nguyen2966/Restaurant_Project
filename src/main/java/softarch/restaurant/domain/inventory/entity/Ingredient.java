package softarch.restaurant.domain.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "current_stock", nullable = false, precision = 10, scale = 3)
    private BigDecimal currentStock;

    @Column(name = "min_threshold", nullable = false, precision = 10, scale = 3)
    private BigDecimal minThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitType unit;

    @Column(name = "last_restock_date")
    private LocalDateTime lastRestockDate;

    protected Ingredient() {}

    public Ingredient(String name, BigDecimal currentStock, BigDecimal minThreshold,
                      UnitType unit, LocalDateTime lastRestockDate) {
        this.name = name;
        this.currentStock = currentStock;
        this.minThreshold = minThreshold;
        this.unit = unit;
        this.lastRestockDate = lastRestockDate;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean isLowStock() {
        return currentStock.compareTo(minThreshold) <= 0;
    }

    public void deduct(BigDecimal amount) {
        if (amount.compareTo(currentStock) > 0) {
            throw new IllegalStateException(
                "Insufficient stock for ingredient '" + name +
                "': available=" + currentStock + ", requested=" + amount);
        }
        this.currentStock = this.currentStock.subtract(amount);
    }

    public void restock(BigDecimal amount) {
        this.currentStock = this.currentStock.add(amount);
        this.lastRestockDate = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                    { return id; }
    public String getName()               { return name; }
    public BigDecimal getCurrentStock()   { return currentStock; }
    public BigDecimal getMinThreshold()   { return minThreshold; }
    public UnitType getUnit()             { return unit; }
    public LocalDateTime getLastRestockDate() { return lastRestockDate; }
}
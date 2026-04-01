package softarch.restaurant.domain.menu.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "menu_item")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Stored as JSONB in PostgreSQL.
     * Example: ["gluten", "nuts"]
     * Requires hypersistence-utils dependency.
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> allergens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected MenuItem() {}

    public MenuItem(String name, BigDecimal basePrice, String description,
                    List<String> allergens, ItemStatus status) {
        this.name = name;
        this.basePrice = basePrice;
        this.description = description;
        this.allergens = allergens;
        this.status = status;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean isAvailable() {
        return status == ItemStatus.AVAILABLE;
    }

    public void activate() {
        if (status == ItemStatus.ARCHIVED) {
            throw new IllegalStateException("Archived items cannot be re-activated.");
        }
        this.status = ItemStatus.AVAILABLE;
    }

    public void deactivate() {
        if (status == ItemStatus.ARCHIVED) {
            throw new IllegalStateException("Item is already archived.");
        }
        this.status = ItemStatus.UNAVAILABLE;
    }

    public void archive() {
        this.status = ItemStatus.ARCHIVED;
    }

    public void update(String name, BigDecimal basePrice, String description, List<String> allergens) {
        this.name = name;
        this.basePrice = basePrice;
        this.description = description;
        this.allergens = allergens;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId()               { return id; }
    public String getName()           { return name; }
    public BigDecimal getBasePrice()  { return basePrice; }
    public String getDescription()    { return description; }
    public List<String> getAllergens() { return allergens; }
    public ItemStatus getStatus()     { return status; }
}
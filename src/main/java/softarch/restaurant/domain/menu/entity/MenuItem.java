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

    // Matches diagram field name "basePrice" / DB column base_price
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Stored as JSONB — e.g. ["gluten","nuts"]
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> allergens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    protected MenuItem() {}

    public MenuItem(String name, BigDecimal basePrice, String description,
                    List<String> allergens) {
        this.name        = name;
        this.basePrice   = basePrice;
        this.description = description;
        this.allergens   = allergens;
        this.status      = ItemStatus.ACTIVE;  // new items start ACTIVE
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean isActive() {
        return status == ItemStatus.ACTIVE;
    }

    public void update(String name, BigDecimal basePrice,
                       String description, List<String> allergens) {
        this.name        = name;
        this.basePrice   = basePrice;
        this.description = description;
        this.allergens   = allergens;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                { return id; }
    public String getName()            { return name; }
    public BigDecimal getBasePrice()   { return basePrice; }
    public String getDescription()     { return description; }
    public List<String> getAllergens() { return allergens; }
    public ItemStatus getStatus()      { return status; }
}
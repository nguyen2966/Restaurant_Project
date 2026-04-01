package softarch.restaurant.domain.order.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(name = "special_notes", length = 255)
    private String specialNotes;

    /**
     * Flexible customization options stored as JSONB.
     * Example: {"size": "large", "spice": "mild", "noodle": "extra"}
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> options;

    @Column(name = "is_allergy_alert", nullable = false)
    private boolean isAllergyAlert;

    protected OrderItem() {}

    OrderItem(Order order, Long menuItemId, int quantity,
              BigDecimal priceAtPurchase, String specialNotes, boolean isAllergyAlert) {
        this.order            = order;
        this.menuItemId       = menuItemId;
        this.quantity         = quantity;
        this.priceAtPurchase  = priceAtPurchase;
        this.specialNotes     = specialNotes;
        this.isAllergyAlert   = isAllergyAlert;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public BigDecimal lineTotal() {
        return priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }

    public void addNote(String note) {
        this.specialNotes = note;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                    { return id; }
    public Order getOrder()                { return order; }
    public Long getMenuItemId()            { return menuItemId; }
    public Integer getQuantity()           { return quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public String getSpecialNotes()        { return specialNotes; }
    public Map<String, String> getOptions() { return options; }
    public boolean isAllergyAlert()        { return isAllergyAlert; }
}
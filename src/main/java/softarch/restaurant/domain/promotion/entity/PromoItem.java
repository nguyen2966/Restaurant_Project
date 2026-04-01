package softarch.restaurant.domain.promotion.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promo_item")
public class PromoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Matches diagram: String name
    @Column(nullable = false, length = 100)
    private String name;

    // Matches diagram: PromoType promoType
    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 20)
    private PromoType promoType;

    // Matches diagram: String condition
    @Column(name = "condition_desc", length = 255)
    private String condition;

    /**
     * Matches diagram: List<Long> menuItemIds.
     * Stored in the promo_menu_mapping join table.
     */
    @ElementCollection
    @CollectionTable(name = "promo_menu_mapping",
        joinColumns = @JoinColumn(name = "promo_id"))
    @Column(name = "menu_item_id")
    private List<Long> menuItemIds = new ArrayList<>();

    // Matches diagram: DateTime startDate / dueDate
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime dueDate;

    // Discount value — percent (0-100) or fixed amount depending on promoType
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    // Matches diagram: PromoStatus status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromoStatus status;

    protected PromoItem() {}

    public PromoItem(String name, PromoType promoType, String condition,
                     List<Long> menuItemIds, LocalDateTime startDate,
                     LocalDateTime dueDate, BigDecimal discountValue) {
        this.name          = name;
        this.promoType     = promoType;
        this.condition     = condition;
        this.menuItemIds   = new ArrayList<>(menuItemIds);
        this.startDate     = startDate;
        this.dueDate       = dueDate;
        this.discountValue = discountValue;
        this.status        = PromoStatus.ACTIVE;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean isActive() {
        return status == PromoStatus.ACTIVE
            && (startDate == null || !LocalDateTime.now().isBefore(startDate))
            && (dueDate   == null || !LocalDateTime.now().isAfter(dueDate));
    }

    public boolean coversMenuItem(Long menuItemId) {
        return menuItemIds.contains(menuItemId);
    }

    public void update(String name, PromoType promoType, String condition,
                       List<Long> menuItemIds, LocalDateTime startDate,
                       LocalDateTime dueDate, BigDecimal discountValue) {
        this.name          = name;
        this.promoType     = promoType;
        this.condition     = condition;
        this.menuItemIds   = new ArrayList<>(menuItemIds);
        this.startDate     = startDate;
        this.dueDate       = dueDate;
        this.discountValue = discountValue;
    }

    public void setStatus(PromoStatus status) { this.status = status; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public PromoType getPromoType()      { return promoType; }
    public String getCondition()         { return condition; }
    public List<Long> getMenuItemIds()   { return menuItemIds; }
    public LocalDateTime getStartDate()  { return startDate; }
    public LocalDateTime getDueDate()    { return dueDate; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public PromoStatus getStatus()       { return status; }
}
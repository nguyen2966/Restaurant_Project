package softarch.restaurant.domain.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "table_id")
    private Long tableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Order createDraft(Long tableId, OrderType type, Long createdByUserId) {
        Order order = new Order();
        order.orderCode       = generateCode();
        order.tableId         = tableId;
        order.type            = type;
        order.status          = OrderStatus.DRAFT;
        order.subTotal        = BigDecimal.ZERO;
        order.createdByUserId = createdByUserId;
        order.createdAt       = LocalDateTime.now();
        return order;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public OrderItem addItem(Long menuItemId, int quantity,
                             BigDecimal unitPrice, String specialNotes,
                             boolean isAllergyAlert) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot add items to an order in status: " + status);
        }
        OrderItem item = new OrderItem(this, menuItemId, quantity, unitPrice,
                                       specialNotes, isAllergyAlert);
        items.add(item);
        recalculateSubTotal();
        return item;
    }

    public void confirm() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT orders can be confirmed.");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty order.");
        }
        this.status = OrderStatus.PLACED;
    }

    public void markAsPaid() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only PLACED orders can be marked as paid.");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("Paid orders cannot be cancelled.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void recalculateSubTotal() {
        this.subTotal = items.stream()
            .map(OrderItem::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String generateCode() {
        return "ORD-" + System.currentTimeMillis();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()               { return id; }
    public String getOrderCode()      { return orderCode; }
    public Long getTableId()          { return tableId; }
    public OrderType getType()        { return type; }
    public OrderStatus getStatus()    { return status; }
    public BigDecimal getSubTotal()   { return subTotal; }
    public Long getCreatedByUserId()  { return createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
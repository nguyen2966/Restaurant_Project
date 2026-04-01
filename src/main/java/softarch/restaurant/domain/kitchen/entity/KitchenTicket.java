package softarch.restaurant.domain.kitchen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * KitchenTicket uses the State pattern.
 * currentState is a transient object; statusEnumValue is the DB-persisted string.
 */
@Entity
@Table(name = "kitchen_ticket")
public class KitchenTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * The string persisted in DB (e.g. "QUEUED", "COOKING", "READY", "PAUSED", "DELIVERED").
     * The actual state object is re-hydrated in @PostLoad.
     */
    @Column(name = "current_state", nullable = false, length = 20)
    private String statusEnumValue;

    /** Transient — not mapped to DB, reconstructed from statusEnumValue on load. */
    @Transient
    private TicketState currentState;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "expected_completion_time", nullable = false)
    private LocalDateTime deadlineTime;

    @Column(length = 50)
    private String station;

    protected KitchenTicket() {}

    public KitchenTicket(Long orderItemId, Long menuItemId, int quantity,
                         LocalDateTime deadlineTime, String station) {
        this.orderItemId      = orderItemId;
        this.menuItemId       = menuItemId;
        this.quantity         = quantity;
        this.deadlineTime     = deadlineTime;
        this.station          = station;
        this.currentState     = QueuedState.INSTANCE;
        this.statusEnumValue  = currentState.name();
    }

    // ── State restoration after JPA load ──────────────────────────────────────

    @PostLoad
    void restoreState() {
        this.currentState = switch (statusEnumValue) {
            case "QUEUED"    -> QueuedState.INSTANCE;
            case "COOKING"   -> CookingState.INSTANCE;
            case "READY"     -> ReadyState.INSTANCE;
            case "PAUSED"    -> PausedState.INSTANCE;
            case "DELIVERED" -> DeliveredState.INSTANCE;
            default          -> throw new IllegalStateException("Unknown state: " + statusEnumValue);
        };
    }

    // ── Delegating methods (diagram: startCooking, markDone, pause, undo) ─────

    public void startCooking() { currentState.startCooking(this); }
    public void markDone()     { currentState.markDone(this); }
    public void pause()        { currentState.pause(this); }
    public void deliver()      { currentState.deliver(this); }
    public void undo()         { currentState.undo(this); }

    /** Called by concrete state objects to transition. */
    public void changeState(TicketState newState) {
        this.currentState    = newState;
        this.statusEnumValue = newState.name();
    }

    public boolean isNearDeadline() {
        return deadlineTime != null &&
               LocalDateTime.now().isAfter(deadlineTime.minusMinutes(5));
    }

    // ── Getters / package-private setters (used by state objects) ─────────────

    public Long getId()               { return id; }
    public Long getOrderItemId()      { return orderItemId; }
    public Long getMenuItemId()       { return menuItemId; }
    public Integer getQuantity()      { return quantity; }
    public String getStatusEnumValue() { return statusEnumValue; }
    public String getCurrentStateName() { return statusEnumValue; }
    public LocalDateTime getStartedAt()    { return startedAt; }
    public LocalDateTime getFinishedAt()   { return finishedAt; }
    public LocalDateTime getDeadlineTime() { return deadlineTime; }
    public String getStation()             { return station; }

    void setStartedAt(LocalDateTime t)  { this.startedAt  = t; }
    void setFinishedAt(LocalDateTime t) { this.finishedAt = t; }
}
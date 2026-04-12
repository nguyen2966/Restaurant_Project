package softarch.restaurant.domain.kitchen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * KitchenTicket — matches updated diagram.
 *
 * Thay đổi so với phiên bản cũ:
 *  - station (String) → assignedStation (@ManyToOne Station)
 *  - startCooking(station) nhận Station thay vì không có tham số
 *  - freeStation() — giải phóng station khi DONE/PAUSE/UNDO
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
     * Persisted as string in DB — restored to state object via @PostLoad.
     * Values: QUEUED | COOKING | READY | PAUSED | DELIVERED
     */
    @Column(name = "current_state", nullable = false, length = 20)
    private String statusEnumValue;

    @Transient
    private TicketState currentState;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "expected_completion_time", nullable = false)
    private LocalDateTime deadlineTime;

    /**
     * Matches diagram: KitchenTicket o--> Station.
     * Nullable — ticket bắt đầu ở QUEUED chưa có station.
     * Chef gán khi gọi startCooking().
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station assignedStation;

    protected KitchenTicket() {}

    public KitchenTicket(Long orderItemId, Long menuItemId, int quantity,
                         LocalDateTime deadlineTime) {
        this.orderItemId     = orderItemId;
        this.menuItemId      = menuItemId;
        this.quantity        = quantity;
        this.deadlineTime    = deadlineTime;
        this.currentState    = QueuedState.INSTANCE;
        this.statusEnumValue = currentState.name();
        // station null — assigned when chef picks one
    }

    // ── State restoration ─────────────────────────────────────────────────────

    @PostLoad
    void restoreState() {
        this.currentState = switch (statusEnumValue) {
            case "QUEUED"    -> QueuedState.INSTANCE;
            case "COOKING"   -> CookingState.INSTANCE;
            case "READY"     -> ReadyState.INSTANCE;
            case "PAUSED"    -> PausedState.INSTANCE;
            default          -> throw new IllegalStateException("Unknown state: " + statusEnumValue);
        };
    }

    // ── Delegating methods — matches diagram ──────────────────────────────────

    /** UC9: Chef bấm "Start" → phải chọn station. */
    public void startCooking(Station station) { currentState.startCooking(this, station); }

    public void markDone()  { currentState.markDone(this); }
    public void pause()     { currentState.pause(this); }
    public void deliver()   { currentState.deliver(this); }
    public void undo()      { currentState.undo(this); }

    /** Called by concrete state objects to transition. */
    public void changeState(TicketState newState) {
        this.currentState    = newState;
        this.statusEnumValue = newState.name();
    }

    /**
     * Matches diagram: freeStation().
     * Giải phóng station về AVAILABLE, xóa FK.
     * Called by CookingState.markDone(), .pause(), .undo().
     */
    public void freeStation() {
        if (assignedStation != null) {
            assignedStation.markAvailable();
            assignedStation = null;
        }
    }

    /** Called by QueuedState/PausedState.startCooking() after station.markInUse(). */
    void assignStation(Station station) {
        this.assignedStation = station;
    }

    // ── Business helpers ──────────────────────────────────────────────────────

    public boolean isNearDeadline() {
        return deadlineTime != null &&
               LocalDateTime.now().isAfter(deadlineTime.minusMinutes(5));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long          getId()               { return id; }
    public Long          getOrderItemId()      { return orderItemId; }
    public Long          getMenuItemId()       { return menuItemId; }
    public Integer       getQuantity()         { return quantity; }
    public String        getStatusEnumValue()  { return statusEnumValue; }
    public String        getCurrentStateName() { return statusEnumValue; }
    public LocalDateTime getStartedAt()        { return startedAt; }
    public LocalDateTime getFinishedAt()       { return finishedAt; }
    public LocalDateTime getDeadlineTime()     { return deadlineTime; }
    public Station       getAssignedStation()  { return assignedStation; }

    void setStartedAt(LocalDateTime t)  { this.startedAt  = t; }
    void setFinishedAt(LocalDateTime t) { this.finishedAt = t; }
}
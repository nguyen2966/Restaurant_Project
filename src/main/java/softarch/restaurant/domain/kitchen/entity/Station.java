package softarch.restaurant.domain.kitchen.entity;

import jakarta.persistence.*;
import softarch.restaurant.shared.exception.RestaurantException;

/**
 * Matches diagram: Station entity.
 *
 * Một station là một vị trí nấu cụ thể trong bếp (Grill 1, Cold 2, Bar 1...).
 * Khi chef bắt đầu nấu, họ phải chọn một station AVAILABLE.
 * Station chuyển IN_USE cho đến khi ticket DONE, UNDO, hoặc PAUSE.
 *
 * type  : nhóm chức năng (GRILL, COLD, BAR, FRY, PASTRY, UNASSIGNED)
 * status: AVAILABLE | IN_USE | OFFLINE
 */
@Entity
@Table(name = "station")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** GRILL | COLD | BAR | FRY | PASTRY | UNASSIGNED */
    @Column(nullable = false, length = 50)
    private String type;

    /** AVAILABLE | IN_USE | OFFLINE */
    @Column(nullable = false, length = 20)
    private String status;

    protected Station() {}

    // ── Domain behaviour — matches diagram: markInUse(), markAvailable() ──────

    /** Chef chọn station này để bắt đầu nấu. */
    public void markInUse() {
        if ("OFFLINE".equals(status)) {
            throw RestaurantException.conflict(
                "Station '" + name + "' is OFFLINE and cannot be used.");
        }
        if ("IN_USE".equals(status)) {
            throw RestaurantException.conflict(
                "Station '" + name + "' is already IN_USE by another ticket.");
        }
        this.status = "IN_USE";
    }

    /** Ticket DONE / UNDO / PAUSE → trả station về AVAILABLE. */
    public void markAvailable() {
        this.status = "AVAILABLE";
    }

    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long   getId()     { return id; }
    public String getName()   { return name; }
    public String getType()   { return type; }
    public String getStatus() { return status; }
}
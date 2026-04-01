package softarch.restaurant.domain.seating.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurant_table")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_code", nullable = false, unique = true, length = 20)
    private String tableCode;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status;

    // Default constructor for JPA
    protected RestaurantTable() {}

    public RestaurantTable(String tableCode, int capacity) {
        this.tableCode = tableCode;
        this.capacity  = capacity;
        this.status    = TableStatus.AVAILABLE;
    }

    // ── Business methods ──────────────────────────────
    public void markAsSeated()  { this.status = TableStatus.SEATED; }
    public void markAsDirty()   { this.status = TableStatus.DIRTY; }
    public void markAvailable() { this.status = TableStatus.AVAILABLE; }
    public void markOrdering()  { this.status = TableStatus.ORDERING; }

    public boolean isAvailable() { return this.status == TableStatus.AVAILABLE; }

    // ── Getters ───────────────────────────────────────
    public Long getId()           { return id; }
    public String getTableCode()  { return tableCode; }
    public Integer getCapacity()  { return capacity; }
    public TableStatus getStatus(){ return status; }
}
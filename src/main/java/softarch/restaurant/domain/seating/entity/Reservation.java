package softarch.restaurant.domain.seating.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "reserved_time", nullable = false)
    private LocalDateTime reservedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    protected Reservation() {}

    public Reservation(Long tableId, String customerName, String customerPhone,
                       int partySize, LocalDateTime reservedTime) {
        this.tableId      = tableId;
        this.customerName = customerName;
        this.customerPhone= customerPhone;
        this.partySize    = partySize;
        this.reservedTime = reservedTime;
        this.status       = ReservationStatus.BOOKED;
    }

    public void seat()     { this.status = ReservationStatus.SEATED; }
    public void noShow()   { this.status = ReservationStatus.NO_SHOW; }
    public void cancel()   { this.status = ReservationStatus.CANCELLED; }

    public Long getId()                     { return id; }
    public Long getTableId()                { return tableId; }
    public void setTableId(Long tableId)    { this.tableId = tableId; }
    public String getCustomerName()         { return customerName; }
    public String getCustomerPhone()        { return customerPhone; }
    public Integer getPartySize()           { return partySize; }
    public LocalDateTime getReservedTime()  { return reservedTime; }
    public ReservationStatus getStatus()    { return status; }
}

package softarch.restaurant.domain.seating.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entry")
public class WaitlistEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_notified", nullable = false)
    private Boolean isNotified;

    protected WaitlistEntry() {}

    public WaitlistEntry(String customerName, int partySize) {
        this.customerName = customerName;
        this.partySize    = partySize;
        this.joinedAt     = LocalDateTime.now();
        this.isNotified   = false;
    }

    public void markNotified() { this.isNotified = true; }

    public Long getId()              { return id; }
    public String getCustomerName()  { return customerName; }
    public Integer getPartySize()    { return partySize; }
    public LocalDateTime getJoinedAt(){ return joinedAt; }
    public Boolean getIsNotified()   { return isNotified; }
}

package softarch.restaurant.domain.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "tip_amount", precision = 10, scale = 2)
    private BigDecimal tipAmount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "gateway_ref_id", length = 100)
    private String gatewayReferenceId;

    protected PaymentTransaction() {}

    public PaymentTransaction(Long orderId, BigDecimal amount, BigDecimal tipAmount,
                              BigDecimal taxAmount, BigDecimal discountAmount,
                              PaymentMethod method) {
        this.orderId         = orderId;
        this.amount          = amount;
        this.tipAmount       = tipAmount;
        this.taxAmount       = taxAmount;
        this.discountAmount  = discountAmount;
        this.method          = method;
        this.status          = PaymentStatus.PENDING;
        this.transactionTime = LocalDateTime.now();
    }

    public void markCompleted(String gatewayRefId) {
        this.status            = PaymentStatus.COMPLETED;
        this.gatewayReferenceId = gatewayRefId;
    }

    public void markFailed()   { this.status = PaymentStatus.FAILED; }
    public void markRefunded() { this.status = PaymentStatus.REFUNDED; }

    public Long getId()                      { return id; }
    public Long getOrderId()                 { return orderId; }
    public BigDecimal getAmount()            { return amount; }
    public BigDecimal getTipAmount()         { return tipAmount; }
    public BigDecimal getTaxAmount()         { return taxAmount; }
    public BigDecimal getDiscountAmount()    { return discountAmount; }
    public PaymentMethod getMethod()         { return method; }
    public PaymentStatus getStatus()         { return status; }
    public LocalDateTime getTransactionTime(){ return transactionTime; }
    public String getGatewayReferenceId()   { return gatewayReferenceId; }
}

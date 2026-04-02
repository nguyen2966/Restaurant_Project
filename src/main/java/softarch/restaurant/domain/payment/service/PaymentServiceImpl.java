package softarch.restaurant.domain.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.payment.entity.PaymentMethod;
import softarch.restaurant.domain.payment.entity.PaymentStatus;
import softarch.restaurant.domain.payment.entity.PaymentTransaction;
import softarch.restaurant.domain.payment.event.PaymentCompletedEvent;
import softarch.restaurant.domain.payment.repository.PaymentRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository      repository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(PaymentRepository repository,
                              ApplicationEventPublisher eventPublisher) {
        this.repository      = repository;
        this.eventPublisher  = eventPublisher;
    }

    @Override
    public PaymentTransaction processPayment(Long orderId, BigDecimal amount,
                                             BigDecimal tipAmount, PaymentMethod method) {
        // Guard: no duplicate COMPLETED payment for same order
        boolean alreadyPaid = repository.findByOrderId(orderId).stream()
            .anyMatch(t -> t.getStatus() == PaymentStatus.COMPLETED);
        if (alreadyPaid) {
            throw RestaurantException.conflict("Order " + orderId + " is already paid.");
        }

        BigDecimal tip = tipAmount != null ? tipAmount : BigDecimal.ZERO;
        PaymentTransaction tx = new PaymentTransaction(
            orderId, amount, tip, BigDecimal.ZERO, BigDecimal.ZERO, method);

        // In production: call external gateway here, then:
        tx.markCompleted("GW-" + System.currentTimeMillis());
        PaymentTransaction saved = repository.save(tx);

        log.info("Payment completed for orderId={} amount={} method={}", orderId, amount, method);

        // Notify Order domain → marks order PAID
        eventPublisher.publishEvent(
            new PaymentCompletedEvent(orderId, amount.doubleValue(), method.name()));

        return saved;
    }

    @Override
    public PaymentTransaction refundPayment(Long paymentTransactionId) {
        PaymentTransaction tx = repository.findById(paymentTransactionId)
            .orElseThrow(() -> RestaurantException.notFound("PaymentTransaction", paymentTransactionId));

        if (tx.getStatus() != PaymentStatus.COMPLETED) {
            throw RestaurantException.conflict("Only COMPLETED transactions can be refunded.");
        }
        tx.markRefunded();
        return repository.save(tx);
    }

    @Override
    public List<PaymentTransaction> splitPayment(Long orderId,
                                                  List<BigDecimal> amounts,
                                                  List<PaymentMethod> methods) {
        if (amounts.size() != methods.size()) {
            throw RestaurantException.badRequest("amounts and methods lists must be same size.");
        }
        List<PaymentTransaction> results = new ArrayList<>();
        for (int i = 0; i < amounts.size(); i++) {
            results.add(processPayment(orderId, amounts.get(i), BigDecimal.ZERO, methods.get(i)));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId);
    }
}

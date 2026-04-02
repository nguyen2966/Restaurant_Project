package softarch.restaurant.domain.payment.service;

import softarch.restaurant.domain.payment.entity.PaymentMethod;
import softarch.restaurant.domain.payment.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    /**
     * Processes a payment for the given order.
     * On success publishes PaymentCompletedEvent → Order marked PAID.
     */
    PaymentTransaction processPayment(Long orderId, BigDecimal amount,
                                      BigDecimal tipAmount, PaymentMethod method);

    /** Issues a refund for the given transaction. */
    PaymentTransaction refundPayment(Long paymentTransactionId);

    /**
     * Splits the bill: processes multiple partial payments for the same order.
     * Each entry in amounts corresponds to one portion of the total.
     */
    List<PaymentTransaction> splitPayment(Long orderId,
                                          List<BigDecimal> amounts,
                                          List<PaymentMethod> methods);

    List<PaymentTransaction> findByOrderId(Long orderId);
}

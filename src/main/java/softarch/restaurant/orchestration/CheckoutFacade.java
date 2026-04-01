package softarch.restaurant.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.order.entity.Order;
import softarch.restaurant.domain.order.entity.OrderItem;
import softarch.restaurant.domain.order.service.OrderService;
import softarch.restaurant.domain.promotion.service.PromoService;
import softarch.restaurant.shared.exception.RestaurantException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CheckoutFacade — from previous session's diagram.
 *
 * Orchestrates UC-12 (Payment):
 *   calculateOrderTotal — fetches Order, applies PromoService discount, adds tax
 *   processCheckout     — delegates to PaymentService (stub here; full impl in payment domain)
 */
@Component
@Transactional(readOnly = true)
public class CheckoutFacade {

    private static final Logger log = LoggerFactory.getLogger(CheckoutFacade.class);

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% VAT

    private final OrderService orderService;
    private final PromoService promoService;

    public CheckoutFacade(OrderService orderService, PromoService promoService) {
        this.orderService = orderService;
        this.promoService = promoService;
    }

    // ── calculateOrderTotal(Long orderId): OrderBillingDTO ───────────────────

    public record OrderBillingDTO(
        Long       orderId,
        String     orderCode,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal total
    ) {}

    /**
     * Matches diagram: calculateOrderTotal(Long orderId): OrderBillingDTO
     *
     * Pipeline:
     *   1. Load Order to get subTotal and basket quantities
     *   2. Ask PromoService for applicable discount
     *   3. Apply 10% VAT on the discounted amount
     *   4. Return billing breakdown without persisting anything
     */
    public OrderBillingDTO calculateOrderTotal(Long orderId) {
        Order order = orderService.getOrderEntity(orderId);

        BigDecimal subTotal = order.getSubTotal();

        // Build menuItemId → quantity map for promo calculation
        Map<Long, Integer> quantities = order.getItems().stream()
            .collect(Collectors.toMap(
                OrderItem::getMenuItemId,
                OrderItem::getQuantity,
                Integer::sum
            ));

        // Discount from any active promotions
        BigDecimal discount = promoService.calculateDiscount(quantities, subTotal);
        BigDecimal afterDiscount = subTotal.subtract(discount).max(BigDecimal.ZERO);

        // Tax on discounted amount
        BigDecimal tax = afterDiscount.multiply(TAX_RATE)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = afterDiscount.add(tax).setScale(2, RoundingMode.HALF_UP);

        log.info("CheckoutFacade: order={} subTotal={} discount={} tax={} total={}",
            order.getOrderCode(), subTotal, discount, tax, total);

        return new OrderBillingDTO(
            orderId, order.getOrderCode(),
            subTotal, discount, tax, total
        );
    }

    // ── processCheckout(CheckoutReq req) ──────────────────────────────────────

    public record CheckoutRequest(
        Long       orderId,
        String     paymentMethod,   // CASH | CARD | ONLINE_BANKING | E_WALLET
        BigDecimal tipAmount,
        boolean    splitBill
    ) {}

    /**
     * Matches diagram: processCheckout(CheckoutReq req)
     *
     * Calculates the final bill then delegates to PaymentService.
     * PaymentService will publish PaymentCompletedEvent → Order marked PAID.
     */
    @Transactional
    public OrderBillingDTO processCheckout(CheckoutRequest request) {
        OrderBillingDTO billing = calculateOrderTotal(request.orderId());

        // Tip is added on top of the calculated total
        BigDecimal tip   = request.tipAmount() != null ? request.tipAmount() : BigDecimal.ZERO;
        BigDecimal grand = billing.total().add(tip);

        log.info("CheckoutFacade: processing payment for order={} method={} total={}",
            billing.orderCode(), request.paymentMethod(), grand);

        // PaymentService integration point — injected in full Payment domain implementation.
        // For now the facade validates the billing and returns; the controller
        // calls PaymentController.createPayment() separately.
        if (billing.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw RestaurantException.unprocessable("Order total is zero — nothing to charge.");
        }

        return billing;
    }
}
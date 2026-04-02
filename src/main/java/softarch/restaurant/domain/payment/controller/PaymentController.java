package softarch.restaurant.domain.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.payment.entity.PaymentMethod;
import softarch.restaurant.domain.payment.entity.PaymentTransaction;
import softarch.restaurant.domain.payment.service.PaymentService;
import softarch.restaurant.orchestration.CheckoutFacade;
import softarch.restaurant.orchestration.CheckoutFacade.CheckoutRequest;
import softarch.restaurant.orchestration.CheckoutFacade.OrderBillingDTO;
import softarch.restaurant.shared.dto.ApiResponse;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService  paymentService;
    private final CheckoutFacade  checkoutFacade;

    public PaymentController(PaymentService paymentService,
                             CheckoutFacade checkoutFacade) {
        this.paymentService = paymentService;
        this.checkoutFacade = checkoutFacade;
    }

    /**
     * GET /api/payments/orders/{orderId}/total
     * Preview the billing breakdown (subtotal, discount, tax, total)
     * before committing payment.
     */
    @GetMapping("/orders/{orderId}/total")
    public ResponseEntity<ApiResponse<OrderBillingDTO>> getTotal(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
            ApiResponse.ok(checkoutFacade.calculateOrderTotal(orderId)));
    }

    /**
     * POST /api/payments
     * Process a full payment. Routes through CheckoutFacade to
     * validate total, then delegates to PaymentService.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentTransaction>> createPayment(
            @RequestParam Long          orderId,
            @RequestParam BigDecimal    amount,
            @RequestParam PaymentMethod method,
            @RequestParam(required = false, defaultValue = "0") BigDecimal tip) {

        // Validate billing via facade before charging
        checkoutFacade.processCheckout(
            new CheckoutRequest(orderId, method.name(), tip, false));

        PaymentTransaction tx = paymentService.processPayment(orderId, amount, tip, method);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(tx, "Payment processed"));
    }

    /**
     * POST /api/payments/{id}/refund
     * Issue a refund for a completed payment transaction.
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentTransaction>> refund(
            @PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.ok(paymentService.refundPayment(id), "Refund issued"));
    }

    /**
     * GET /api/payments/orders/{orderId}
     * List all payment transactions for an order (useful for split-bill view).
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> listByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
            ApiResponse.ok(paymentService.findByOrderId(orderId)));
    }
}

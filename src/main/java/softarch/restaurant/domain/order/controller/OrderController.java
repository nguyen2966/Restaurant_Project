package softarch.restaurant.domain.order.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.order.dto.OrderDTOs.NoteRequest;
import softarch.restaurant.domain.order.dto.OrderDTOs.OrderRequest;
import softarch.restaurant.domain.order.dto.OrderDTOs.OrderResponse;
import softarch.restaurant.domain.order.service.OrderService;
import softarch.restaurant.orchestration.OrderingFacade;
import softarch.restaurant.shared.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService    orderService;
    private final OrderingFacade  orderingFacade;

    public OrderController(OrderService orderService, OrderingFacade orderingFacade) {
        this.orderService   = orderService;
        this.orderingFacade = orderingFacade;
    }

    /**
     * POST /api/orders
     * Routes through OrderingFacade for full validation (menu active + stock).
     * Matches diagram: createOrder()
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.status(201)
            .body(ApiResponse.ok(orderingFacade.validateAndPlaceOrder(request, userId),
                "Order placed"));
    }

    /** GET /api/orders/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getById(id)));
    }

    /** GET /api/orders?tableId=3 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getByTable(
            @RequestParam Long tableId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getByTable(tableId)));
    }

    /**
     * PATCH /api/orders/{orderId}/items/{itemId}/note
     * Matches diagram: addNotes()
     */
    @PatchMapping("/{orderId}/items/{itemId}/note")
    public ResponseEntity<ApiResponse<OrderResponse>> addNote(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            orderService.addNoteToItem(orderId, itemId, request.note())));
    }

    /** DELETE /api/orders/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrder(id),
            "Order cancelled"));
    }
}
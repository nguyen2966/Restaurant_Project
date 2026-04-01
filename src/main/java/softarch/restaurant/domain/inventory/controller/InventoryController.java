package softarch.restaurant.domain.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.LowStockAlert;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.UsageRequest;
import softarch.restaurant.domain.inventory.service.InventoryService;
import softarch.restaurant.shared.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * POST /api/inventory/usage
     * Staff manually records ingredient usage (e.g. end-of-shift or waste log).
     *
     * Header: X-User-Id  — resolved from JWT in a real system; passed directly here for simplicity.
     */
    @PostMapping("/usage")
    public ResponseEntity<ApiResponse<Void>> recordUsage(
            @Valid @RequestBody List<UsageRequest> requests,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        inventoryService.recordManualUsage(requests, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Usage recorded"));
    }

    /**
     * GET /api/inventory/alerts
     * Returns all ingredients below their minimum threshold.
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<LowStockAlert>>> alerts() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.checkLowStockAlerts()));
    }
}
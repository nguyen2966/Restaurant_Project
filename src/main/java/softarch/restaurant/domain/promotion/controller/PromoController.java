package softarch.restaurant.domain.promotion.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.promotion.dto.PromoDTOs.*;
import softarch.restaurant.domain.promotion.entity.PromoStatus;
import softarch.restaurant.domain.promotion.service.PromoService;
import softarch.restaurant.shared.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromoController {

    private final PromoService promoService;

    public PromoController(PromoService promoService) {
        this.promoService = promoService;
    }

    /** GET /api/promotions?status=ACTIVE */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoResponse>>> list(
            @RequestParam(required = false, defaultValue = "ACTIVE") PromoStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(promoService.findByStatus(status)));
    }

    /** POST /api/promotions — matches diagram: createPromo() */
    @PostMapping
    public ResponseEntity<ApiResponse<PromoResponse>> createPromo(
            @Valid @RequestBody PromoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(promoService.createItem(request), "Promotion created"));
    }

    /** PUT /api/promotions/{id} — matches diagram: updateItem() */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody PromoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promoService.updateItem(id, request)));
    }

    /** DELETE /api/promotions/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promoService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Promotion deactivated"));
    }

    /** PATCH /api/promotions/{id}/status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PromoResponse>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promoService.setStatus(id, request.status())));
    }

    /** POST /api/promotions/simulate */
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<SimulationResult>> simulate(
            @RequestBody SimulateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promoService.simulatePromo(request)));
    }
}
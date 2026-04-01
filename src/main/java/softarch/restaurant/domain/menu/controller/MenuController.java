package softarch.restaurant.domain.menu.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuItemResponse;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuRequest;
import softarch.restaurant.domain.menu.dto.MenuDTOs.StatusRequest;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.service.MenuService;
import softarch.restaurant.shared.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** GET /api/menu?query=pho&status=AVAILABLE */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ItemStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.search(query, status)));
    }

    /** GET /api/menu/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getById(id)));
    }

    /** GET /api/menu/best-sellers */
    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> bestSellers() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getBestSellers()));
    }

    /** POST /api/menu  — MANAGER only */
    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(
            @Valid @RequestBody MenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(menuService.createItem(request), "Menu item created"));
    }

    /** PUT /api/menu/{id}  — MANAGER only */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MenuRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.updateItem(id, request)));
    }

    /** DELETE /api/menu/{id}  — MANAGER only (soft-archives) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        menuService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Menu item archived"));
    }

    /** PATCH /api/menu/{id}/status  — MANAGER only */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MenuItemResponse>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.setStatus(id, request.status())));
    }
}
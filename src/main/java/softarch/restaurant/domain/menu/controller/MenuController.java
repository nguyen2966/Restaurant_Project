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
import softarch.restaurant.orchestration.AdminCatalogFacade;
import softarch.restaurant.shared.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService        menuService;
    private final AdminCatalogFacade adminCatalogFacade;

    public MenuController(MenuService menuService, AdminCatalogFacade adminCatalogFacade) {
        this.menuService        = menuService;
        this.adminCatalogFacade = adminCatalogFacade;
    }

    // ── viewMenu() ────────────────────────────────────────────────────────────

    /** GET /api/menu?query=pho  — all staff */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> viewMenu(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ItemStatus status) {
        List<MenuItemResponse> result = (status != null)
            ? menuService.filterByStatus(status)
            : menuService.search(query);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** GET /api/menu/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.search("").stream()
            .filter(m -> m.id().equals(id)).findFirst()
            .orElseThrow(() -> softarch.restaurant.shared.exception.RestaurantException.notFound("MenuItem", id))));
    }

    /** GET /api/menu/best-sellers */
    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> bestSellers() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getBestSellers()));
    }

    // ── manageItem() — MANAGER only ───────────────────────────────────────────

    /** POST /api/menu */
    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(
            @Valid @RequestBody MenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(menuService.createItem(request), "Menu item created"));
    }

    /** PUT /api/menu/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MenuRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.updateItem(id, request)));
    }

    /** DELETE /api/menu/{id} — routes through AdminCatalogFacade for cross-domain validation */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminCatalogFacade.safeDeleteMenuItem(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Menu item deactivated"));
    }

    /** PATCH /api/menu/{id}/status — routes through AdminCatalogFacade when disabling */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MenuItemResponse>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        MenuItemResponse result = adminCatalogFacade.changeMenuItemStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
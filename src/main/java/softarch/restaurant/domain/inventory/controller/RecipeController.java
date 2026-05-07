package softarch.restaurant.domain.inventory.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.inventory.entity.Ingredient;
import softarch.restaurant.domain.inventory.entity.RecipeItem;
import softarch.restaurant.domain.inventory.repository.IngredientRepository;
import softarch.restaurant.domain.inventory.repository.RecipeRepository;
import softarch.restaurant.shared.dto.ApiResponse;
import softarch.restaurant.shared.exception.RestaurantException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RecipeController — manages the ingredient-to-menu-item mapping (recipe_item
 * table).
 *
 * A menu item without recipe entries will silently skip inventory deduction
 * when cooked (by design — items like drinks may not need tracking).
 * This controller lets managers define exactly what each dish consumes.
 */
@RestController
@RequestMapping("/api/menu/{menuItemId}/recipe")
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;

    public RecipeController(RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record RecipeLineRequest(
            @NotNull(message = "ingredientId is required") Long ingredientId,

            @NotNull(message = "requiredAmount is required") @DecimalMin(value = "0.001", message = "requiredAmount must be positive") BigDecimal requiredAmount) {
    }

    public record RecipeLineResponse(
            Long id,
            Long menuItemId,
            Long ingredientId,
            String ingredientName,
            String unit,
            BigDecimal requiredAmount) {
    }

    // ── GET /api/menu/{menuItemId}/recipe ─────────────────────────────────────

    /**
     * Returns all recipe lines for a menu item.
     * Frontend uses this to pre-populate the recipe editor.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipeLineResponse>>> getRecipe(
            @PathVariable Long menuItemId) {

        List<RecipeItem> items = recipeRepository.findByMenuItemId(menuItemId);
        List<RecipeLineResponse> response = items.stream()
                .map(ri -> toResponse(ri))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── POST /api/menu/{menuItemId}/recipe ────────────────────────────────────

    /**
     * Adds a single ingredient line to the recipe.
     * Throws 409 if ingredient already exists in this recipe.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<RecipeLineResponse>>> addLines(
            @PathVariable Long menuItemId,
            @Valid @RequestBody List<RecipeLineRequest> requests) {

        // Existing ingredients in recipe
        Set<Long> existingIngredientIds = recipeRepository.findByMenuItemId(menuItemId)
                .stream()
                .map(RecipeItem::getIngredientId)
                .collect(Collectors.toSet());

        // Check duplicate ingredient inside request body
        Set<Long> requestIngredientIds = new HashSet<>();

        for (RecipeLineRequest request : requests) {

            // Duplicate inside request payload
            if (!requestIngredientIds.add(request.ingredientId())) {
                throw RestaurantException.conflict(
                        "Duplicate ingredient id=" + request.ingredientId() + " in request.");
            }

            // Already exists in DB
            if (existingIngredientIds.contains(request.ingredientId())) {
                throw RestaurantException.conflict(
                        "Ingredient id=" + request.ingredientId() +
                                " already exists in this recipe. Use PUT to update.");
            }
        }

        List<RecipeLineResponse> responses = new ArrayList<>();

        for (RecipeLineRequest request : requests) {

            Ingredient ingredient = ingredientRepository.findById(request.ingredientId())
                    .orElseThrow(() -> RestaurantException.notFound("Ingredient", request.ingredientId()));

            RecipeItem saved = recipeRepository.save(
                    new RecipeItem(
                            menuItemId,
                            request.ingredientId(),
                            request.requiredAmount()));

            responses.add(toResponse(saved, ingredient));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(responses, "Recipe lines added"));
    }

    // ── PUT /api/menu/{menuItemId}/recipe/{recipeItemId} ──────────────────────

    /**
     * Updates the requiredAmount of an existing recipe line.
     */
    @PutMapping("/{recipeItemId}")
    public ResponseEntity<ApiResponse<RecipeLineResponse>> updateLine(
            @PathVariable Long menuItemId,
            @PathVariable Long recipeItemId,
            @Valid @RequestBody RecipeLineRequest request) {

        RecipeItem item = recipeRepository.findById(recipeItemId)
                .orElseThrow(() -> RestaurantException.notFound("RecipeItem", recipeItemId));

        if (!item.getMenuItemId().equals(menuItemId)) {
            throw RestaurantException.badRequest("Recipe item does not belong to menuItem " + menuItemId);
        }

        item.updateAmount(request.requiredAmount());
        RecipeItem saved = recipeRepository.save(item);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(saved)));
    }

    // ── DELETE /api/menu/{menuItemId}/recipe/{recipeItemId} ───────────────────

    /**
     * Removes an ingredient line from the recipe.
     */
    @DeleteMapping("/{recipeItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteLine(
            @PathVariable Long menuItemId,
            @PathVariable Long recipeItemId) {

        RecipeItem item = recipeRepository.findById(recipeItemId)
                .orElseThrow(() -> RestaurantException.notFound("RecipeItem", recipeItemId));

        if (!item.getMenuItemId().equals(menuItemId)) {
            throw RestaurantException.badRequest("Recipe item does not belong to menuItem " + menuItemId);
        }

        recipeRepository.delete(item);
        return ResponseEntity.ok(ApiResponse.ok(null, "Recipe line removed"));
    }

    // ── PUT /api/menu/{menuItemId}/recipe/bulk ────────────────────────────────

    /**
     * Replaces the entire recipe at once (delete-all + insert).
     * Most convenient for the frontend form: submit the full recipe in one call.
     */
    @PutMapping("/bulk")
    public ResponseEntity<ApiResponse<List<RecipeLineResponse>>> bulkReplace(
            @PathVariable Long menuItemId,
            @Valid @RequestBody List<RecipeLineRequest> requests) {

        // Delete all existing lines for this menu item
        List<RecipeItem> existing = recipeRepository.findByMenuItemId(menuItemId);
        recipeRepository.deleteAll(existing);

        // Insert the new lines
        List<RecipeItem> saved = requests.stream()
                .map(req -> recipeRepository.save(
                        new RecipeItem(menuItemId, req.ingredientId(), req.requiredAmount())))
                .toList();

        List<RecipeLineResponse> response = saved.stream()
                .map(ri -> toResponse(ri))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response,
                "Recipe updated: " + response.size() + " lines"));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private RecipeLineResponse toResponse(RecipeItem ri) {
        Ingredient ingredient = ingredientRepository.findById(ri.getIngredientId())
                .orElse(null);
        return new RecipeLineResponse(
                ri.getId(), ri.getMenuItemId(), ri.getIngredientId(),
                ingredient != null ? ingredient.getName() : "Unknown",
                ingredient != null ? ingredient.getUnit().name() : "?",
                ri.getRequiredAmount());
    }

    private RecipeLineResponse toResponse(RecipeItem ri, Ingredient ingredient) {
        return new RecipeLineResponse(
                ri.getId(), ri.getMenuItemId(), ri.getIngredientId(),
                ingredient.getName(), ingredient.getUnit().name(),
                ri.getRequiredAmount());
    }
}
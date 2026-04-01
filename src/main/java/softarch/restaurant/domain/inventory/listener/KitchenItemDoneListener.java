package softarch.restaurant.domain.inventory.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import softarch.restaurant.domain.inventory.service.InventoryService;

/**
 * Listens for KitchenItemDoneEvent and triggers automatic stock deduction.
 *
 * Runs @Async so that the kitchen ticket status update transaction
 * is not blocked by inventory writes.
 */
@Component
public class KitchenItemDoneListener {

    private static final Logger log = LoggerFactory.getLogger(KitchenItemDoneListener.class);

    private final InventoryService inventoryService;

    public KitchenItemDoneListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Async
    @EventListener
    public void handleItemDoneEvent(KitchenItemDoneEvent event) {
        log.info("Auto-deducting inventory for menuItemId={} qty={}",
            event.getMenuItemId(), event.getQuantity());
        try {
            inventoryService.autoDeductForMenuItem(event.getMenuItemId(), event.getQuantity());
        } catch (Exception e) {
            // Log and continue — inventory deduction failure must not
            // roll back the already-committed kitchen ticket update
            log.error("Inventory auto-deduct failed for menuItemId={}: {}",
                event.getMenuItemId(), e.getMessage(), e);
        }
    }
}
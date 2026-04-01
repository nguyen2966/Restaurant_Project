package softarch.restaurant.domain.kitchen.event;

/**
 * Published by the Kitchen domain when a KitchenTicket transitions to DONE.
 * Consumed by KitchenItemDoneListener to trigger inventory auto-deduction.
 */
public class KitchenItemDoneEvent {

    private final Long menuItemId;
    private final int  quantity;
    private final Long orderItemId;

    public KitchenItemDoneEvent(Long menuItemId, int quantity, Long orderItemId) {
        this.menuItemId  = menuItemId;
        this.quantity    = quantity;
        this.orderItemId = orderItemId;
    }

    public Long getMenuItemId()  { return menuItemId; }
    public int  getQuantity()    { return quantity; }
    public Long getOrderItemId() { return orderItemId; }
}
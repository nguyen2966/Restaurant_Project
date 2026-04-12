package softarch.restaurant.domain.kitchen.entity;

/**
 * READY → deliver hoặc undo (về COOKING).
 * Station đã được giải phóng khi markDone.
 */
public class ReadyState implements TicketState {

    public static final ReadyState INSTANCE = new ReadyState();
    private ReadyState() {}


    @Override
    public void undo(KitchenTicket ticket) {
        // Undo từ READY về COOKING — không cần re-assign station (chef tự chọn lại)
        ticket.setFinishedAt(null);
        ticket.changeState(CookingState.INSTANCE);
    }

    @Override
    public String name() { return "READY"; }
}
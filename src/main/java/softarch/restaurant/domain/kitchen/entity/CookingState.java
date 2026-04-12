package softarch.restaurant.domain.kitchen.entity;

import java.time.LocalDateTime;

/**
 * COOKING → markDone, pause, undo.
 * markDone và pause giải phóng station.
 * undo trả station về AVAILABLE và clear assignment.
 */
public class CookingState implements TicketState {

    public static final CookingState INSTANCE = new CookingState();
    private CookingState() {}

    @Override
    public void markDone(KitchenTicket ticket) {
        ticket.freeStation();             // station → AVAILABLE
        ticket.setFinishedAt(LocalDateTime.now());
        ticket.changeState(ReadyState.INSTANCE);
    }

    @Override
    public void pause(KitchenTicket ticket) {
        ticket.freeStation();             // station → AVAILABLE (có thể dùng cho ticket khác khi pause)
        ticket.changeState(PausedState.INSTANCE);
    }

    @Override
    public void undo(KitchenTicket ticket) {
        ticket.freeStation();             // station → AVAILABLE
        ticket.setStartedAt(null);
        ticket.changeState(QueuedState.INSTANCE);
    }

    @Override
    public String name() { return "COOKING"; }
}
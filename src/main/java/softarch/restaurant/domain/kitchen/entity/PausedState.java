package softarch.restaurant.domain.kitchen.entity;

/**
 * PAUSED → startCooking(station) để resume, hoặc undo về QUEUED.
 * Khi pause, station đã được giải phóng (CookingState.pause làm việc này).
 * Khi resume, chef phải chọn lại station.
 */
public class PausedState implements TicketState {

    public static final PausedState INSTANCE = new PausedState();
    private PausedState() {}

    @Override
    public void startCooking(KitchenTicket ticket, Station station) {
        station.markInUse();
        ticket.assignStation(station);
        ticket.changeState(CookingState.INSTANCE);
    }

    @Override
    public void undo(KitchenTicket ticket) {
        ticket.changeState(QueuedState.INSTANCE);
    }

    @Override
    public String name() { return "PAUSED"; }
}
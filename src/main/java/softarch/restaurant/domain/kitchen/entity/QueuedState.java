package softarch.restaurant.domain.kitchen.entity;

import java.time.LocalDateTime;

/**
 * QUEUED → chỉ có thể startCooking(station).
 * Chef phải chọn một Station còn AVAILABLE trước khi bắt đầu nấu.
 */
public class QueuedState implements TicketState {

    public static final QueuedState INSTANCE = new QueuedState();
    private QueuedState() {}

    @Override
    public void startCooking(KitchenTicket ticket, Station station) {
        station.markInUse();              // throws nếu OFFLINE hoặc IN_USE
        ticket.assignStation(station);    // gán station vào ticket
        ticket.setStartedAt(LocalDateTime.now());
        ticket.changeState(CookingState.INSTANCE);
    }

    @Override
    public String name() { return "QUEUED"; }
}
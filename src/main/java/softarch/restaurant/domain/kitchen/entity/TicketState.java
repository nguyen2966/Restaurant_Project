package softarch.restaurant.domain.kitchen.entity;

/**
 * State pattern interface — matches updated diagram.
 *
 * startCooking() nhận thêm Station để gán vào ticket khi bắt đầu nấu.
 * Các transition khác (markDone, pause, undo) không cần station.
 */
public interface TicketState {

    default void startCooking(KitchenTicket ticket, Station station) {
        throw new IllegalStateException(
            "Cannot start cooking from state: " + ticket.getCurrentStateName());
    }

    default void markDone(KitchenTicket ticket) {
        throw new IllegalStateException(
            "Cannot mark done from state: " + ticket.getCurrentStateName());
    }

    default void pause(KitchenTicket ticket) {
        throw new IllegalStateException(
            "Cannot pause from state: " + ticket.getCurrentStateName());
    }

    default void deliver(KitchenTicket ticket) {
        throw new IllegalStateException(
            "Cannot deliver from state: " + ticket.getCurrentStateName());
    }

    default void undo(KitchenTicket ticket) {
        throw new IllegalStateException(
            "Cannot undo from state: " + ticket.getCurrentStateName());
    }

    String name();
}
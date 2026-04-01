package softarch.restaurant.domain.kitchen.entity;

/**
 * State pattern interface for KitchenTicket.
 * Each concrete state only permits the transitions that are valid from that state,
 * throwing IllegalStateException for invalid ones.
 */
public interface TicketState {

    default void startCooking(KitchenTicket ticket) {
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

    /** Returns the string name stored in the DB column. */
    String name();
}
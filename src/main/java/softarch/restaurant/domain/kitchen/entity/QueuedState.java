package softarch.restaurant.domain.kitchen.entity;

import java.time.LocalDateTime;

// ═══════════════════════════════════════════════════════════════════════════
// QUEUED  →  can only startCooking
// ═══════════════════════════════════════════════════════════════════════════
class QueuedState implements TicketState {

    static final QueuedState INSTANCE = new QueuedState();
    private QueuedState() {}

    @Override
    public void startCooking(KitchenTicket ticket) {
        ticket.setStartedAt(LocalDateTime.now());
        ticket.changeState(CookingState.INSTANCE);
    }

    @Override
    public String name() { return "QUEUED"; }
}

// ═══════════════════════════════════════════════════════════════════════════
// COOKING  →  can markDone, pause, or undo (back to QUEUED)
// ═══════════════════════════════════════════════════════════════════════════
class CookingState implements TicketState {

    static final CookingState INSTANCE = new CookingState();
    private CookingState() {}

    @Override
    public void markDone(KitchenTicket ticket) {
        ticket.setFinishedAt(LocalDateTime.now());
        ticket.changeState(ReadyState.INSTANCE);
    }

    @Override
    public void pause(KitchenTicket ticket) {
        ticket.changeState(PausedState.INSTANCE);
    }

    @Override
    public void undo(KitchenTicket ticket) {
        ticket.setStartedAt(null);
        ticket.changeState(QueuedState.INSTANCE);
    }

    @Override
    public String name() { return "COOKING"; }
}

// ═══════════════════════════════════════════════════════════════════════════
// READY  →  can deliver or undo (back to COOKING)
// ═══════════════════════════════════════════════════════════════════════════
class ReadyState implements TicketState {

    static final ReadyState INSTANCE = new ReadyState();
    private ReadyState() {}

    @Override
    public void deliver(KitchenTicket ticket) {
        ticket.changeState(DeliveredState.INSTANCE);
    }

    @Override
    public void undo(KitchenTicket ticket) {
        ticket.setFinishedAt(null);
        ticket.changeState(CookingState.INSTANCE);
    }

    @Override
    public String name() { return "READY"; }
}

// ═══════════════════════════════════════════════════════════════════════════
// PAUSED  →  can startCooking (resume) or undo (back to QUEUED)
// ═══════════════════════════════════════════════════════════════════════════
class PausedState implements TicketState {

    static final PausedState INSTANCE = new PausedState();
    private PausedState() {}

    @Override
    public void startCooking(KitchenTicket ticket) {
        ticket.changeState(CookingState.INSTANCE);
    }

    @Override
    public void undo(KitchenTicket ticket) {
        ticket.setStartedAt(null);
        ticket.changeState(QueuedState.INSTANCE);
    }

    @Override
    public String name() { return "PAUSED"; }
}

// ═══════════════════════════════════════════════════════════════════════════
// DELIVERED  →  terminal state, no transitions
// ═══════════════════════════════════════════════════════════════════════════
class DeliveredState implements TicketState {

    static final DeliveredState INSTANCE = new DeliveredState();
    private DeliveredState() {}

    @Override
    public String name() { return "DELIVERED"; }
}
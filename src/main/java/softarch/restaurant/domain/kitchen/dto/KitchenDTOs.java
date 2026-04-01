package softarch.restaurant.domain.kitchen.dto;

import softarch.restaurant.domain.kitchen.entity.KitchenTicket;

import java.time.LocalDateTime;
import java.util.List;

public final class KitchenDTOs {

    private KitchenDTOs() {}

    // ── Filter DTO (matches diagram: KitchenTicketFilter) ─────────────────────

    public record KitchenTicketFilter(
        List<String> stations,       // e.g. ["GRILL","COLD"]
        String       status,         // e.g. "COOKING"
        Boolean      isNearDeadline, // true = only tickets within 5 min of deadline
        String       sortBy          // "deadline" | "station" | "created"
    ) {}

    // ── Response ──────────────────────────────────────────────────────────────

    public record KitchenTicketResponse(
        Long          id,
        Long          orderItemId,
        Long          menuItemId,
        Integer       quantity,
        String        status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime deadlineTime,
        boolean       nearDeadline,
        String        station
    ) {
        public static KitchenTicketResponse from(KitchenTicket t) {
            return new KitchenTicketResponse(
                t.getId(), t.getOrderItemId(), t.getMenuItemId(),
                t.getQuantity(), t.getCurrentStateName(),
                t.getStartedAt(), t.getFinishedAt(), t.getDeadlineTime(),
                t.isNearDeadline(), t.getStation()
            );
        }
    }

    // ── SLA data (matches diagram: getSLAData()) ───────────────────────────────

    public record SLAData(
        Long   menuItemId,
        String menuItemName,
        double avgMinutesToComplete,
        long   totalTickets,
        long   overdueTickets
    ) {}
}
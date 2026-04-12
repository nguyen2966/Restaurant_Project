// package softarch.restaurant.domain.kitchen.dto;

// import softarch.restaurant.domain.kitchen.entity.KitchenTicket;

// import java.time.LocalDateTime;
// import java.util.List;

// public final class KitchenDTOs {

//     private KitchenDTOs() {}

//     // ── Filter DTO (matches diagram: KitchenTicketFilter) ─────────────────────

//     public record KitchenTicketFilter(
//         List<String> stations,       // e.g. ["GRILL","COLD"]
//         String       status,         // e.g. "COOKING"
//         Boolean      isNearDeadline, // true = only tickets within 5 min of deadline
//         String       sortBy          // "deadline" | "station" | "created"
//     ) {}

//     // ── Response ──────────────────────────────────────────────────────────────

//     public record KitchenTicketResponse(
//         Long          id,
//         Long          orderItemId,
//         Long          menuItemId,
//         Integer       quantity,
//         String        status,
//         LocalDateTime startedAt,
//         LocalDateTime finishedAt,
//         LocalDateTime deadlineTime,
//         boolean       nearDeadline,
//         String        station
//     ) {
//         public static KitchenTicketResponse from(KitchenTicket t) {
//             return new KitchenTicketResponse(
//                 t.getId(), t.getOrderItemId(), t.getMenuItemId(),
//                 t.getQuantity(), t.getCurrentStateName(),
//                 t.getStartedAt(), t.getFinishedAt(), t.getDeadlineTime(),
//                 t.isNearDeadline(), t.getStation()
//             );
//         }
//     }

//     // ── SLA data (matches diagram: getSLAData()) ───────────────────────────────

//     public record SLAData(
//         Long   menuItemId,
//         String menuItemName,
//         double avgMinutesToComplete,
//         long   totalTickets,
//         long   overdueTickets
//     ) {}
// }

package softarch.restaurant.domain.kitchen.dto;

import softarch.restaurant.domain.kitchen.entity.KitchenTicket;
import softarch.restaurant.domain.kitchen.entity.Station;

import java.time.LocalDateTime;
import java.util.List;

public final class KitchenDTOs {

    private KitchenDTOs() {}

    // ── Filter ────────────────────────────────────────────────────────────────

    public record KitchenTicketFilter(
        List<String> stations,       // filter theo station name hoặc type
        String       status,         // QUEUED | COOKING | READY | PAUSED
        Boolean      isNearDeadline, // true = chỉ tickets trong 5 phút tới
        String       sortBy          // "deadline" | "station"
    ) {}

    // ── Station response ──────────────────────────────────────────────────────

    public record StationResponse(
        Long   id,
        String name,
        String type,
        String status
    ) {
        public static StationResponse from(Station s) {
            return new StationResponse(s.getId(), s.getName(), s.getType(), s.getStatus());
        }
    }

    // ── Ticket response ───────────────────────────────────────────────────────

    public record KitchenTicketResponse(
        Long            id,
        Long            orderItemId,
        Long            menuItemId,
        Integer         quantity,
        String          status,
        LocalDateTime   startedAt,
        LocalDateTime   finishedAt,
        LocalDateTime   deadlineTime,
        boolean         nearDeadline,
        StationResponse assignedStation    // null khi ticket còn QUEUED
    ) {
        public static KitchenTicketResponse from(KitchenTicket t) {
            StationResponse stationResponse = t.getAssignedStation() != null
                ? StationResponse.from(t.getAssignedStation())
                : null;
            return new KitchenTicketResponse(
                t.getId(), t.getOrderItemId(), t.getMenuItemId(),
                t.getQuantity(), t.getCurrentStateName(),
                t.getStartedAt(), t.getFinishedAt(), t.getDeadlineTime(),
                t.isNearDeadline(), stationResponse
            );
        }
    }

    // ── SLA ───────────────────────────────────────────────────────────────────

    public record SLAData(
        Long   menuItemId,
        String menuItemName,
        double avgMinutesToComplete,
        long   totalTickets,
        long   overdueTickets
    ) {}
}


package softarch.restaurant.domain.kitchen.dto;

import softarch.restaurant.domain.kitchen.entity.KitchenTicket;
import softarch.restaurant.domain.kitchen.entity.Station;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // ── Ticket response ──────────────────────────────────────────────────────
     public record MenuItemResponse(
        Long id,
        String name
    ) {
        // Constructor phụ để khởi tạo nhanh khi chỉ có ID
        public MenuItemResponse(Long id) {
            this(id, null);
        }
    }

    public record OrderItemResponse(
        Long id,
        Long menuItemId,
        Integer quantity,
        String specialNotes,
        Map<String, String> options
    ) {
        // Constructor phụ để khởi tạo nhanh khi chỉ có ID
        public OrderItemResponse(Long id) {
            this(id, null, null, null, null);
        }
    }

    public record KitchenTicketResponse(
        Long id,
        OrderItemResponse orderItem,
        MenuItemResponse menuItem,
        Integer quantity,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime deadlineTime,
        boolean nearDeadline,
        StationResponse assignedStation
    ) {
        public static KitchenTicketResponse from(KitchenTicket t) {
            StationResponse stationResponse = t.getAssignedStation() != null
                ? StationResponse.from(t.getAssignedStation())
                : null;

            // Sửa lỗi: Truyền đầy đủ các tham số theo đúng thứ tự của record
            return new KitchenTicketResponse(
                t.getId(),                               // 1. id
                new OrderItemResponse(t.getOrderItemId()), // 2. orderItem (DTO)
                new MenuItemResponse(t.getMenuItemId()),  // 3. menuItem (DTO)
                t.getQuantity(),                         // 4. quantity
                t.getCurrentStateName(),                 // 5. status
                t.getStartedAt(),                        // 6. startedAt
                t.getFinishedAt(),                       // 7. finishedAt
                t.getDeadlineTime(),                     // 8. deadlineTime
                t.isNearDeadline(),                      // 9. nearDeadline
                stationResponse                          // 10. assignedStation
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
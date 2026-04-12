package softarch.restaurant.domain.kitchen.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.KitchenTicketFilter;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.KitchenTicketResponse;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.SLAData;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.StationResponse;
import softarch.restaurant.domain.kitchen.service.KitchenService;
import softarch.restaurant.shared.dto.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    /**
     * GET /api/kitchen/queue
     * UC7: Hiển thị hàng đợi. Filter: stations, status, nearDeadline, sortBy.
     */
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<KitchenTicketResponse>>> getQueue(
            @RequestParam(required = false) List<String> stations,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean nearDeadline,
            @RequestParam(required = false, defaultValue = "deadline") String sortBy) {

        KitchenTicketFilter filter = new KitchenTicketFilter(stations, status, nearDeadline, sortBy);
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.viewQueue(filter)));
    }

    /**
     * GET /api/kitchen/stations?type=GRILL
     * Matches diagram: getAvailableStations(type).
     * UC9: Chef gọi để xem station nào còn trống trước khi bấm Start.
     * type null = trả tất cả AVAILABLE stations.
     */
    @GetMapping("/stations")
    public ResponseEntity<ApiResponse<List<StationResponse>>> getAvailableStations(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getAvailableStations(type)));
    }

    /**
     * PATCH /api/kitchen/tickets/{id}/start?stationId={stationId}
     * Matches diagram: processStartCooking(ticketId, stationId).
     * UC9: Chef chọn station khi bắt đầu nấu.
     */
    @PatchMapping("/tickets/{id}/start")
    public ResponseEntity<ApiResponse<KitchenTicketResponse>> start(
            @PathVariable Long id,
            @RequestParam Long stationId) {
        return ResponseEntity.ok(ApiResponse.ok(
            kitchenService.processStartCooking(id, stationId)));
    }

    /**
     * PATCH /api/kitchen/tickets/{id}/done
     * UC9: Hoàn thành → READY, giải phóng station, publish KitchenItemDoneEvent.
     */
    @PatchMapping("/tickets/{id}/done")
    public ResponseEntity<ApiResponse<KitchenTicketResponse>> done(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.processMarkDone(id)));
    }

    /**
     * PATCH /api/kitchen/tickets/{id}/pause
     * UC9 A2 (Hold): tạm dừng, giải phóng station cho ticket khác dùng.
     */
    @PatchMapping("/tickets/{id}/pause")
    public ResponseEntity<ApiResponse<KitchenTicketResponse>> pause(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.processPause(id)));
    }

    /**
     * PATCH /api/kitchen/tickets/{id}/undo
     * UC9 A1: Chef bấm nhầm Done → Undo trong policy time.
     */
    @PatchMapping("/tickets/{id}/undo")
    public ResponseEntity<ApiResponse<KitchenTicketResponse>> undo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.processUndo(id)));
    }

    /**
     * GET /api/kitchen/sla?from=...&to=...
     * Báo cáo SLA theo khoảng thời gian.
     */
    @GetMapping("/sla")
    public ResponseEntity<ApiResponse<List<SLAData>>> sla(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getSLAData(from, to)));
    }
}
package softarch.restaurant.domain.seating.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.domain.seating.dto.SeatingDTOs.*;
import softarch.restaurant.domain.seating.service.SeatingService;
import softarch.restaurant.shared.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/seating")
public class SeatingController {

    private final SeatingService seatingService;

    public SeatingController(SeatingService seatingService) {
        this.seatingService = seatingService;
    }

    /** GET /api/seating/tables */
    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<TableResponse>>> tables() {
        return ResponseEntity.ok(ApiResponse.ok(seatingService.getAllTables()));
    }

    /** POST /api/seating/tables/{id}/seat-walkin */
    @PostMapping("/tables/{id}/seat-walkin")
    public ResponseEntity<ApiResponse<TableResponse>> seatWalkIn(
            @PathVariable Long id,
            @RequestParam Integer partySize) {
        return ResponseEntity.ok(ApiResponse.ok(
            seatingService.seatWalkInCustomer(id, partySize)));
    }

    /** POST /api/seating/reservations */
    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(seatingService.createReservation(request), "Reservation created"));
    }

    /** POST /api/seating/reservations/{id}/seat */
    @PostMapping("/reservations/{id}/seat")
    public ResponseEntity<ApiResponse<ReservationResponse>> seatReservation(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(seatingService.seatReservation(id)));
    }

    /** POST /api/seating/reservations/{id}/no-show */
    @PostMapping("/reservations/{id}/no-show")
    public ResponseEntity<ApiResponse<ReservationResponse>> noShow(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(seatingService.markNoShow(id)));
    }

    /** GET /api/seating/waitlist */
    @GetMapping("/waitlist")
    public ResponseEntity<ApiResponse<List<WaitlistResponse>>> waitlist() {
        return ResponseEntity.ok(ApiResponse.ok(seatingService.getWaitlist()));
    }

    /** POST /api/seating/waitlist */
    @PostMapping("/waitlist")
    public ResponseEntity<ApiResponse<WaitlistResponse>> joinWaitlist(
            @Valid @RequestBody WaitlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(seatingService.addToWaitlist(request)));
    }

    /** POST /api/seating/tables/move?from=1&to=2 */
    @PostMapping("/tables/move")
    public ResponseEntity<ApiResponse<Void>> moveTable(
            @RequestParam Long from,
            @RequestParam Long to) {
        seatingService.moveTable(from, to);
        return ResponseEntity.ok(ApiResponse.ok(null, "Table moved"));
    }

    /** POST /api/seating/tables/merge */
    @PostMapping("/tables/merge")
    public ResponseEntity<ApiResponse<Void>> mergeTables(
            @RequestBody List<Long> tableIds) {
        seatingService.mergeTables(tableIds);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tables merged"));
    }
}
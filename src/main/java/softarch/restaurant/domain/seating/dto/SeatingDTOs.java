package softarch.restaurant.domain.seating.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;


public final class SeatingDTOs {

    private SeatingDTOs() {}

    // ── Inbound ───────────────────────────────────────────────────────────────

    public record ReservationRequest(
        Long tableId,   // optional — system can auto-assign

        @NotBlank(message = "customerName is required")
        String customerName,

        String customerPhone,

        @NotNull @Min(1)
        Integer partySize,

        @NotNull(message = "reservedTime is required")
        LocalDateTime reservedTime
    ) {}

    public record WaitlistRequest(
        @NotBlank String customerName,
        @NotNull @Min(1) Integer partySize
    ) {}

    // ── Outbound ──────────────────────────────────────────────────────────────

    public record TableResponse(
        Long   id,
        String tableCode,
        int    capacity,
        String status
    ) {}

    public record ReservationResponse(
        Long          id,
        Long          tableId,
        String        customerName,
        String        customerPhone,
        int           partySize,
        LocalDateTime reservedTime,
        String        status
    ) {}

    public record WaitlistResponse(
        Long          id,
        String        customerName,
        int           partySize,
        LocalDateTime joinedAt,
        boolean       isNotified
    ) {}

    // Matches diagram: getTableTurnover — analytics DTO
    public record TableData(
        Long   tableId,
        String tableCode,
        long   totalSessions,
        double avgSessionMinutes,
        double revenue
    ) {}
}

package softarch.restaurant.domain.report.dto;

import jakarta.validation.constraints.NotNull;
import softarch.restaurant.domain.report.entity.ReportType;

import java.time.LocalDateTime;
import java.util.Map;

public final class reportDTOs {

    private reportDTOs() {}

    // ── Matches diagram: ReportReq ────────────────────────────────────────────

    public record ReportRequest(
        @NotNull(message = "type is required")
        ReportType type,

        LocalDateTime startDate,
        LocalDateTime dueDate
    ) {}

    // ── Matches diagram: ReportData ───────────────────────────────────────────

    public record ReportData(
        String              summary,
        Map<String, Object> dataPoints
    ) {}

    // ── Report history response ───────────────────────────────────────────────

    public record ReportResponse(
        Long          id,
        ReportType    type,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        LocalDateTime createdAt,
        String        fileUrl
    ) {}
}
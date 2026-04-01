package softarch.restaurant.domain.report.strategy;

import org.springframework.stereotype.Component;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportData;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportRequest;
import softarch.restaurant.domain.report.entity.ReportType;
import softarch.restaurant.orchestration.ReportingDataFacade;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Matches diagram: implied TABLE strategy for ReportType.TABLE.
 * Uses SeatingService turnover data from ReportingDataFacade.
 */
@Component
public class TableReportStrategy implements ReportStrategy {

    @Override
    public boolean isSupported(ReportType type) {
        return type == ReportType.TABLE;
    }

    @Override
    public ReportData execute(ReportRequest request, ReportingDataFacade dataFacade) {
        var turnoverData = dataFacade.fetchTableTurnover(request.startDate(), request.dueDate());

        double avgTurnover = turnoverData.stream()
            .mapToDouble(t -> t.totalSessions())
            .average()
            .orElse(0.0);

        Map<String, Object> dataPoints = new LinkedHashMap<>();
        dataPoints.put("tables", turnoverData);
        dataPoints.put("avgSessionsPerTable", Math.round(avgTurnover * 10.0) / 10.0);
        dataPoints.put("totalSessions",
            turnoverData.stream().mapToLong(t -> t.totalSessions()).sum());

        return new ReportData(
            String.format("Table report: %.1f avg sessions per table", avgTurnover),
            dataPoints
        );
    }
}
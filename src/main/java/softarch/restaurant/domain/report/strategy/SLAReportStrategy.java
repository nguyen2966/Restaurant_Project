package softarch.restaurant.domain.report.strategy;

import org.springframework.stereotype.Component;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportData;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportRequest;
import softarch.restaurant.domain.report.entity.ReportType;
import softarch.restaurant.orchestration.ReportingDataFacade;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Matches diagram: SLAReportStrategy.
 * Analyses kitchen ticket completion times against SLA targets.
 */
@Component
public class SLAReportStrategy implements ReportStrategy {

    @Override
    public boolean isSupported(ReportType type) {
        return type == ReportType.SLA;
    }

    @Override
    public ReportData execute(ReportRequest request, ReportingDataFacade dataFacade) {
        var slaData = dataFacade.fetchKitchenSLA(request.startDate(), request.dueDate());

        long totalTickets  = slaData.stream().mapToLong(s -> s.totalTickets()).sum();
        long overdueTotal  = slaData.stream().mapToLong(s -> s.overdueTickets()).sum();
        double slaBreachPct = totalTickets > 0
            ? (overdueTotal * 100.0 / totalTickets) : 0.0;

        Map<String, Object> dataPoints = new LinkedHashMap<>();
        dataPoints.put("totalTickets", totalTickets);
        dataPoints.put("overdueTickets", overdueTotal);
        dataPoints.put("slaBreachPercent", Math.round(slaBreachPct * 10.0) / 10.0);
        dataPoints.put("byMenuItem", slaData);

        return new ReportData(
            String.format("SLA report: %.1f%% breach rate (%d/%d tickets overdue)",
                slaBreachPct, overdueTotal, totalTickets),
            dataPoints
        );
    }
}
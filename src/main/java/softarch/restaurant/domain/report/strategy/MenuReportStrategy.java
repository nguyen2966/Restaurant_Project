package softarch.restaurant.domain.report.strategy;

import org.springframework.stereotype.Component;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportData;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportRequest;
import softarch.restaurant.domain.report.entity.ReportType;
import softarch.restaurant.orchestration.ReportingDataFacade;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Matches diagram: MenuReportStrategy.
 * Aggregates menu item performance: best sellers, revenue per item.
 */
@Component
public class MenuReportStrategy implements ReportStrategy {

    @Override
    public boolean isSupported(ReportType type) {
        return type == ReportType.MENU;
    }

    @Override
    public ReportData execute(ReportRequest request, ReportingDataFacade dataFacade) {
        var salesData = dataFacade.fetchSalesData(request.startDate(), request.dueDate());

        // Group by menuItemId — find top sellers by quantity
        var topSellers = salesData.stream()
            .sorted((a, b) -> Long.compare(b.totalQuantity(), a.totalQuantity()))
            .limit(10)
            .toList();

        Map<String, Object> dataPoints = new LinkedHashMap<>();
        dataPoints.put("topSellers", topSellers);
        dataPoints.put("totalMenuItemsSold",
            salesData.stream().mapToLong(s -> s.totalQuantity()).sum());

        return new ReportData(
            "Menu report: top 10 best-selling items in period",
            dataPoints
        );
    }
}
package softarch.restaurant.domain.report.strategy;

import org.springframework.stereotype.Component;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportData;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportRequest;
import softarch.restaurant.domain.report.entity.ReportType;
import softarch.restaurant.orchestration.ReportingDataFacade;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Matches diagram: SalesReportStrategy.
 * Pulls sales data from OrderService via ReportingDataFacade and
 * assembles a revenue summary with hourly breakdowns.
 */
@Component
public class SalesReportStrategy implements ReportStrategy {

    @Override
    public boolean isSupported(ReportType type) {
        return type == ReportType.SALE;
    }

    @Override
    public ReportData execute(ReportRequest request, ReportingDataFacade dataFacade) {
        var salesData = dataFacade.fetchSalesData(request.startDate(), request.dueDate());

        // Aggregate: total revenue, total orders, revenue by hour
        double totalRevenue = salesData.stream()
            .mapToDouble(s -> s.revenue().doubleValue())
            .sum();
        long totalOrders = salesData.size();

        Map<String, Object> dataPoints = new LinkedHashMap<>();
        dataPoints.put("totalRevenue", totalRevenue);
        dataPoints.put("totalOrders", totalOrders);
        dataPoints.put("averageOrderValue",
            totalOrders > 0 ? totalRevenue / totalOrders : 0.0);
        dataPoints.put("breakdown", salesData);

        return new ReportData(
            String.format("Sales report: %.0f VND revenue across %d orders", totalRevenue, totalOrders),
            dataPoints
        );
    }
}
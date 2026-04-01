package softarch.restaurant.orchestration;

import org.springframework.stereotype.Component;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.SLAData;
import softarch.restaurant.domain.kitchen.service.KitchenService;
import softarch.restaurant.domain.order.dto.SaleData;
import softarch.restaurant.domain.order.service.OrderService;
import softarch.restaurant.domain.seating.dto.SeatingDTOs.TableData;
import softarch.restaurant.domain.seating.service.SeatingService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReportingDataFacade — matches diagram exactly.
 *
 * Aggregates raw data from multiple bounded contexts for the Analytics domain.
 * ReportServiceImpl holds a reference to this facade and passes it to each
 * ReportStrategy so strategies never import domain services directly.
 *
 * Data flow:
 *   ReportController
 *     → ReportServiceImpl
 *       → ReportStrategy.execute(req, dataFacade)
 *         → ReportingDataFacade.fetch*()
 *           → OrderService / KitchenService / SeatingService
 */
@Component
public class ReportingDataFacade {

    // Matches diagram dependencies
    private final OrderService   orderService;
    private final KitchenService kitchenService;
    private final SeatingService seatingService;

    public ReportingDataFacade(OrderService orderService,
                               KitchenService kitchenService,
                               SeatingService seatingService) {
        this.orderService   = orderService;
        this.kitchenService = kitchenService;
        this.seatingService = seatingService;
    }

    // ── fetchSalesData(startDate, endDate): List<SaleData> ───────────────────
    // Matches diagram method
    public List<SaleData> fetchSalesData(LocalDateTime startDate, LocalDateTime endDate) {
        return orderService.getSalesData(startDate, endDate);
    }

    // ── fetchKitchenSLA(startDate, endDate): List<SLAData> ───────────────────
    // Matches diagram method
    public List<SLAData> fetchKitchenSLA(LocalDateTime startDate, LocalDateTime endDate) {
        return kitchenService.getSLAData(startDate, endDate);
    }

    // ── fetchTableTurnover(startDate, endDate): List<TableData> ──────────────
    // Matches diagram method
    public List<TableData> fetchTableTurnover(LocalDateTime startDate, LocalDateTime endDate) {
        return seatingService.getTableTurnover(startDate, endDate);
    }
}
package softarch.restaurant.domain.order.dto;

import java.math.BigDecimal;

/**
 * Aggregated sales data per menu item over a time period.
 * Returned by OrderService.getSalesData() and consumed by ReportingDataFacade.
 */
public record SaleData(
    Long       menuItemId,
    String     menuItemName,
    long       totalQuantity,
    BigDecimal revenue
) {}
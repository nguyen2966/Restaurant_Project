package softarch.restaurant.domain.order.service;

import softarch.restaurant.domain.order.dto.OrderDTOs.OrderResponse;
import softarch.restaurant.domain.order.dto.SaleData;
import softarch.restaurant.domain.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(Order order);

    Order getOrderEntity(Long orderId);

    OrderResponse getById(Long orderId);

    List<OrderResponse> getByTable(Long tableId);

    OrderResponse addNoteToItem(Long orderId, Long itemId, String note);

    OrderResponse cancelOrder(Long orderId);

    OrderResponse markAsPaid(Long orderId);

    boolean isItemInActiveOrder(Long menuItemId);

    /**
     * Aggregates paid order items into per-menu-item revenue summaries.
     * Used by ReportingDataFacade → SalesReportStrategy / MenuReportStrategy.
     */
    List<SaleData> getSalesData(LocalDateTime startDate, LocalDateTime endDate);
}
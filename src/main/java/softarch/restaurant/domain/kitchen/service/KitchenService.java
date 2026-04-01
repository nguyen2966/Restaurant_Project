package softarch.restaurant.domain.kitchen.service;

import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.KitchenTicketFilter;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.KitchenTicketResponse;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.SLAData;

import java.time.LocalDateTime;
import java.util.List;

public interface KitchenService {

    // Matches diagram: createTicket(event: OrderPlacedEvent)
    void createTicketsForOrder(Long orderId, List<Long> orderItemIds);

    // Matches diagram: viewQueue(filter): List<KitchenTicket>
    List<KitchenTicketResponse> viewQueue(KitchenTicketFilter filter);

    // Matches diagram: processStartCooking / processMarkDone / processPause / processUndo
    KitchenTicketResponse processStartCooking(Long ticketId);
    KitchenTicketResponse processMarkDone(Long ticketId);
    KitchenTicketResponse processPause(Long ticketId);
    KitchenTicketResponse processUndo(Long ticketId);

    // Matches diagram: getSLAData(startDate, endDate): List<SLAData>
    List<SLAData> getSLAData(LocalDateTime startDate, LocalDateTime endDate);
}
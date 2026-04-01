package softarch.restaurant.domain.seating.service;

import softarch.restaurant.domain.seating.dto.SeatingDTOs.*;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatingService {

    // Matches diagram: seatWalkInCustomer(Long tableId, Integer size)
    TableResponse seatWalkInCustomer(Long tableId, Integer partySize);

    // Matches diagram: createReservation(ReservationReq)
    ReservationResponse createReservation(ReservationRequest request);

    // Matches diagram: seatReservation(Long reservationId)
    ReservationResponse seatReservation(Long reservationId);

    // Matches diagram: addToWaitlist(WaitlistReq)
    WaitlistResponse addToWaitlist(WaitlistRequest request);

    List<WaitlistResponse> getWaitlist();

    // Matches diagram: moveTable(Long fromId, Long toId)
    void moveTable(Long fromTableId, Long toTableId);

    // Matches diagram: mergeTables(List<Long> tableIds)
    void mergeTables(List<Long> tableIds);

    // Matches diagram: markNoShow(Long reservationId)
    ReservationResponse markNoShow(Long reservationId);

    // Called by OrderPaymentListener when order is paid
    void clearTable(Long tableId);

    // Matches diagram: getTableTurnover(startDate, endDate): List<TableData>
    List<TableData> getTableTurnover(LocalDateTime startDate, LocalDateTime endDate);

    List<TableResponse> getAllTables();
}

package softarch.restaurant.domain.seating.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.seating.dto.SeatingDTOs.*;
import softarch.restaurant.domain.seating.entity.*;
import softarch.restaurant.domain.seating.repository.*;
import softarch.restaurant.shared.exception.RestaurantException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SeatingServiceImpl implements SeatingService {

    private final TableRepository       tableRepo;
    private final ReservationRepository resRepo;
    private final WaitlistRepository    waitRepo;

    public SeatingServiceImpl(TableRepository tableRepo,
                              ReservationRepository resRepo,
                              WaitlistRepository waitRepo) {
        this.tableRepo = tableRepo;
        this.resRepo   = resRepo;
        this.waitRepo  = waitRepo;
    }

    // ── seatWalkInCustomer ────────────────────────────────────────────────────

    @Override
    public TableResponse seatWalkInCustomer(Long tableId, Integer partySize) {
        RestaurantTable table = findTableOrThrow(tableId);
        if (!table.isAvailable()) {
            throw RestaurantException.conflict(
                "Table " + table.getTableCode() + " is not available (status: " + table.getStatus() + ")");
        }
        if (table.getCapacity() < partySize) {
            throw RestaurantException.badRequest(
                "Table capacity " + table.getCapacity() + " is too small for party of " + partySize);
        }
        table.markAsSeated();
        return toTableResponse(tableRepo.save(table));
    }

    // ── createReservation ────────────────────────────────────────────────────

    @Override
    public ReservationResponse createReservation(ReservationRequest req) {
        Long tableId = req.tableId();

        // Auto-assign smallest available table that fits the party
        if (tableId == null) {
            tableId = tableRepo.findAvailableWithCapacity(req.partySize()).stream()
                .findFirst()
                .map(RestaurantTable::getId)
                .orElseThrow(() -> RestaurantException.unprocessable(
                    "No available table for party of " + req.partySize()));
        }

        Reservation res = new Reservation(tableId, req.customerName(),
            req.customerPhone(), req.partySize(), req.reservedTime());
        return toResResponse(resRepo.save(res));
    }

    // ── seatReservation ───────────────────────────────────────────────────────

    @Override
    public ReservationResponse seatReservation(Long reservationId) {
        Reservation res = resRepo.findById(reservationId)
            .orElseThrow(() -> RestaurantException.notFound("Reservation", reservationId));
        RestaurantTable table = findTableOrThrow(res.getTableId());

        table.markAsSeated();
        tableRepo.save(table);
        res.seat();
        return toResResponse(resRepo.save(res));
    }

    // ── addToWaitlist ─────────────────────────────────────────────────────────

    @Override
    public WaitlistResponse addToWaitlist(WaitlistRequest req) {
        WaitlistEntry entry = new WaitlistEntry(req.customerName(), req.partySize());
        return toWaitResponse(waitRepo.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistResponse> getWaitlist() {
        return waitRepo.findByIsNotifiedFalseOrderByJoinedAtAsc()
            .stream().map(this::toWaitResponse).toList();
    }

    // ── moveTable ────────────────────────────────────────────────────────────

    @Override
    public void moveTable(Long fromTableId, Long toTableId) {
        RestaurantTable from = findTableOrThrow(fromTableId);
        RestaurantTable to   = findTableOrThrow(toTableId);

        if (!to.isAvailable()) {
            throw RestaurantException.conflict("Target table is not available");
        }
        // Transfer status
        to.markAsSeated();
        from.markAsDirty();
        tableRepo.save(from);
        tableRepo.save(to);
    }

    // ── mergeTables ───────────────────────────────────────────────────────────

    @Override
    public void mergeTables(List<Long> tableIds) {
        tableIds.forEach(id -> {
            RestaurantTable t = findTableOrThrow(id);
            t.markAsSeated();
            tableRepo.save(t);
        });
    }

    // ── markNoShow ────────────────────────────────────────────────────────────

    @Override
    public ReservationResponse markNoShow(Long reservationId) {
        Reservation res = resRepo.findById(reservationId)
            .orElseThrow(() -> RestaurantException.notFound("Reservation", reservationId));
        res.noShow();
        return toResResponse(resRepo.save(res));
    }

    // ── clearTable (called by OrderPaymentListener) ───────────────────────────

    @Override
    public void clearTable(Long tableId) {
        RestaurantTable table = findTableOrThrow(tableId);
        table.markAsDirty(); // Needs cleaning before next seating
        tableRepo.save(table);
    }

    // ── getTableTurnover ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TableData> getTableTurnover(LocalDateTime startDate, LocalDateTime endDate) {
        // Real implementation would join with orders and payment_transactions.
        // Returning a stub — full impl requires a native query across domains.
        return tableRepo.findAll().stream().map(t ->
            new TableData(t.getId(), t.getTableCode(), 0L, 0.0, 0.0)
        ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables() {
        return tableRepo.findAll().stream().map(this::toTableResponse).toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private TableResponse toTableResponse(RestaurantTable t) {
        return new TableResponse(t.getId(), t.getTableCode(),
            t.getCapacity(), t.getStatus().name());
    }

    private ReservationResponse toResResponse(Reservation r) {
        return new ReservationResponse(r.getId(), r.getTableId(), r.getCustomerName(),
            r.getCustomerPhone(), r.getPartySize(), r.getReservedTime(), r.getStatus().name());
    }

    private WaitlistResponse toWaitResponse(WaitlistEntry w) {
        return new WaitlistResponse(w.getId(), w.getCustomerName(),
            w.getPartySize(), w.getJoinedAt(), w.getIsNotified());
    }

    private RestaurantTable findTableOrThrow(Long id) {
        return tableRepo.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("Table", id));
    }
}

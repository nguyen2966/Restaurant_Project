// package softarch.restaurant.domain.kitchen.service;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.KitchenTicketFilter;
// import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.KitchenTicketResponse;
// import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.SLAData;
// import softarch.restaurant.domain.kitchen.event.KitchenItemDoneEvent;
// import softarch.restaurant.domain.kitchen.entity.KitchenTicket;
// import softarch.restaurant.domain.kitchen.repository.KitchenRepository;
// import softarch.restaurant.domain.kitchen.repository.KitchenTicketSpecs;
// import softarch.restaurant.domain.order.entity.OrderItem;
// import softarch.restaurant.domain.order.repository.OrderItemRepository;
// import softarch.restaurant.shared.exception.RestaurantException;

// import java.time.Duration;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;
// import java.util.Comparator;

// @Service
// @Transactional
// public class KitchenServiceImpl implements KitchenService {

//     private static final Logger log = LoggerFactory.getLogger(KitchenServiceImpl.class);

//     // Default SLA: 20 minutes per ticket
//     private static final int DEFAULT_SLA_MINUTES = 20;

//     private final KitchenRepository      kitchenRepo;
//     private final OrderItemRepository    orderItemRepo;
//     private final ApplicationEventPublisher eventPublisher;

//     public KitchenServiceImpl(KitchenRepository kitchenRepo,
//                               OrderItemRepository orderItemRepo,
//                               ApplicationEventPublisher eventPublisher) {
//         this.kitchenRepo    = kitchenRepo;
//         this.orderItemRepo  = orderItemRepo;
//         this.eventPublisher = eventPublisher;
//     }

//     // ── createTicket(event: OrderPlacedEvent) ─────────────────────────────────

//     @Override
//     public void createTicketsForOrder(Long orderId, List<Long> orderItemIds) {
//         LocalDateTime deadline = LocalDateTime.now().plusMinutes(DEFAULT_SLA_MINUTES);

//         for (Long itemId : orderItemIds) {
//             OrderItem oi = orderItemRepo.findById(itemId)
//                 .orElseThrow(() -> RestaurantException.notFound("OrderItem", itemId));

//             // Derive kitchen station from menu item id (simplified — real impl looks up recipe/station mapping)
//             KitchenTicket ticket = new KitchenTicket(
//                 oi.getId(),
//                 oi.getMenuItemId(),
//                 oi.getQuantity(),
//                 deadline,
//                 null   // station resolved by manager or recipe config
//             );
//             kitchenRepo.save(ticket);
//             log.info("Kitchen ticket created for orderItemId={} menuItemId={}", itemId, oi.getMenuItemId());
//         }
//     }

//     // ── viewQueue(filter) ─────────────────────────────────────────────────────

//     // @Override
//     // @Transactional(readOnly = true)
//     // public List<KitchenTicketResponse> viewQueue(KitchenTicketFilter filter) {
//     //     // Resolve filter parameters
//     //     String        status   = filter.status();
//     //     String        station  = (filter.stations() != null && filter.stations().size() == 1)
//     //                                 ? filter.stations().get(0) : null;
//     //     LocalDateTime deadline = Boolean.TRUE.equals(filter.isNearDeadline())
//     //                                 ? LocalDateTime.now().plusMinutes(5) : null;

//     //     List<KitchenTicket> tickets = kitchenRepo.findByFilters(status, station, deadline);

//     //     // Multi-station filter applied in memory (JPA spec doesn't easily do IN with optional param)
//     //     if (filter.stations() != null && filter.stations().size() > 1) {
//     //         tickets = tickets.stream()
//     //             .filter(t -> filter.stations().contains(t.getStation()))
//     //             .toList();
//     //     }

//     //     // Sort
//     //     if ("station".equals(filter.sortBy())) {
//     //         tickets = tickets.stream()
//     //             .sorted(Comparator.comparing(KitchenTicket::getStation,
//     //                 Comparator.nullsLast(Comparator.naturalOrder())))
//     //             .toList();
//     //     }

//     //     return tickets.stream().map(KitchenTicketResponse::from).toList();
//     // }
//     @Override
//     @Transactional(readOnly = true)
//     public List<KitchenTicketResponse> viewQueue(KitchenTicketFilter filter) {
//         String        status   = filter.status();
//         String        station  = (filter.stations() != null && filter.stations().size() == 1)
//                                      ? filter.stations().get(0) : null;
//         LocalDateTime deadline = Boolean.TRUE.equals(filter.isNearDeadline())
//                                      ? LocalDateTime.now().plusMinutes(5) : null;

//         // KitchenTicketSpecs.withFilters() chỉ thêm WHERE clause khi param != null
//         // → không bao giờ truyền null vào PostgreSQL prepared statement
//         List<KitchenTicket> tickets = kitchenRepo.findAll(
//             KitchenTicketSpecs.withFilters(status, station, deadline));

//         // Multi-station IN filter áp dụng in-memory (Specification đã sort theo deadline)
//         if (filter.stations() != null && filter.stations().size() > 1) {
//             tickets = tickets.stream()
//                 .filter(t -> filter.stations().contains(t.getStation()))
//                 .toList();
//         }

//         // Override sort nếu client yêu cầu theo station
//         if ("station".equals(filter.sortBy())) {
//             tickets = tickets.stream()
//                 .sorted(Comparator.comparing(KitchenTicket::getStation,
//                     Comparator.nullsLast(Comparator.naturalOrder())))
//                 .toList();
//         }

//         return tickets.stream().map(KitchenTicketResponse::from).toList();
//     }

//     // ── State transition commands ─────────────────────────────────────────────

//     @Override
//     public KitchenTicketResponse processStartCooking(Long ticketId) {
//         KitchenTicket ticket = findOrThrow(ticketId);
//         ticket.startCooking();
//         return KitchenTicketResponse.from(kitchenRepo.save(ticket));
//     }

//     @Override
//     public KitchenTicketResponse processMarkDone(Long ticketId) {
//         KitchenTicket ticket = findOrThrow(ticketId);
//         ticket.markDone();  // COOKING → READY
//         KitchenTicket saved = kitchenRepo.save(ticket);

//         // Publish event so Inventory domain can auto-deduct stock
//         eventPublisher.publishEvent(new KitchenItemDoneEvent(
//             saved.getMenuItemId(),
//             saved.getQuantity(), saved.getOrderItemId()
//         ));

//         log.info("Ticket {} marked READY — KitchenItemDoneEvent published", ticketId);
//         return KitchenTicketResponse.from(saved);
//     }

//     @Override
//     public KitchenTicketResponse processPause(Long ticketId) {
//         KitchenTicket ticket = findOrThrow(ticketId);
//         ticket.pause();
//         return KitchenTicketResponse.from(kitchenRepo.save(ticket));
//     }

//     @Override
//     public KitchenTicketResponse processUndo(Long ticketId) {
//         KitchenTicket ticket = findOrThrow(ticketId);
//         ticket.undo();
//         return KitchenTicketResponse.from(kitchenRepo.save(ticket));
//     }

//     // ── getSLAData(startDate, endDate) ────────────────────────────────────────

//     @Override
//     @Transactional(readOnly = true)
//     public List<SLAData> getSLAData(LocalDateTime startDate, LocalDateTime endDate) {
//         List<KitchenTicket> completed = kitchenRepo.findCompletedBetween(startDate, endDate);

//         // Group by menuItemId and compute avg completion time
//         Map<Long, List<KitchenTicket>> byItem = completed.stream()
//             .collect(Collectors.groupingBy(KitchenTicket::getMenuItemId));

//         return byItem.entrySet().stream().map(entry -> {
//             Long menuItemId = entry.getKey();
//             List<KitchenTicket> tickets = entry.getValue();

//             double avgMinutes = tickets.stream()
//                 .filter(t -> t.getStartedAt() != null && t.getFinishedAt() != null)
//                 .mapToLong(t -> Duration.between(t.getStartedAt(), t.getFinishedAt()).toMinutes())
//                 .average()
//                 .orElse(0);

//             long overdue = tickets.stream()
//                 .filter(t -> t.getFinishedAt() != null && t.getDeadlineTime() != null
//                           && t.getFinishedAt().isAfter(t.getDeadlineTime()))
//                 .count();

//             return new SLAData(menuItemId, "Item #" + menuItemId,
//                 avgMinutes, tickets.size(), overdue);
//         }).toList();
//     }

//     // ── Helper ────────────────────────────────────────────────────────────────

//     private KitchenTicket findOrThrow(Long id) {
//         return kitchenRepo.findById(id)
//             .orElseThrow(() -> RestaurantException.notFound("KitchenTicket", id));
//     }
// }

package softarch.restaurant.domain.kitchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.kitchen.dto.KitchenDTOs.*;
import softarch.restaurant.domain.kitchen.entity.KitchenTicket;
import softarch.restaurant.domain.kitchen.entity.Station;
import softarch.restaurant.domain.kitchen.event.KitchenItemDoneEvent;
import softarch.restaurant.domain.kitchen.repository.KitchenRepository;
import softarch.restaurant.domain.kitchen.repository.KitchenTicketSpecs;
import softarch.restaurant.domain.kitchen.repository.StationRepository;
import softarch.restaurant.domain.menu.entity.MenuItem;
import softarch.restaurant.domain.order.entity.OrderItem;
import softarch.restaurant.domain.order.repository.OrderItemRepository;
import softarch.restaurant.domain.order.repository.OrderRepository;
import softarch.restaurant.domain.menu.repository.MenuRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class KitchenServiceImpl implements KitchenService {

    private static final Logger log = LoggerFactory.getLogger(KitchenServiceImpl.class);
    private static final int DEFAULT_SLA_MINUTES = 20;

    private final KitchenRepository         kitchenRepo;
    private final MenuRepository            menuRepo;
    private final StationRepository         stationRepo;
    private final OrderItemRepository       orderItemRepo;
    private final ApplicationEventPublisher eventPublisher;

    public KitchenServiceImpl(KitchenRepository kitchenRepo,
                              MenuRepository    menuRepo,
                              StationRepository stationRepo,
                              OrderItemRepository orderItemRepo,
                              OrderRepository orderRepo,
                              ApplicationEventPublisher eventPublisher) {
        this.menuRepo       =  menuRepo;
        this.kitchenRepo    = kitchenRepo;
        this.stationRepo    = stationRepo;
        this.orderItemRepo  = orderItemRepo;
        this.eventPublisher = eventPublisher;
    }

    // ── createTicketsForOrder ─────────────────────────────────────────────────

    @Override
    public void createTicketsForOrder(Long orderId, List<Long> orderItemIds) {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(DEFAULT_SLA_MINUTES);

        for (Long itemId : orderItemIds) {
            OrderItem oi = orderItemRepo.findById(itemId)
                .orElseThrow(() -> RestaurantException.notFound("OrderItem", itemId));

            // UC6 A3: nếu không map được station → không fail, ticket tạo với station null
            KitchenTicket ticket = new KitchenTicket(
                oi.getId(), oi.getMenuItemId(), oi.getQuantity(), deadline);
            kitchenRepo.save(ticket);
            log.info("Ticket created: orderItemId={} menuItemId={} deadline={}",
                itemId, oi.getMenuItemId(), deadline);
        }
    }

    // ── viewQueue ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<KitchenTicketResponse> viewQueue(KitchenTicketFilter filter) {
        String        status   = filter.status();
        LocalDateTime deadline = Boolean.TRUE.equals(filter.isNearDeadline())
                                     ? LocalDateTime.now().plusMinutes(5) : null;

        // Resolve station names → IDs nếu filter.stations có giá trị
        Long singleStationId = resolveSingleStationId(filter.stations());

        Specification<KitchenTicket> spec =
            KitchenTicketSpecs.withFilters(status, singleStationId, deadline);

        List<KitchenTicket> tickets = kitchenRepo.findAll(spec);

        // Multi-station filter in-memory (IN query)
        if (filter.stations() != null && filter.stations().size() > 1) {
            List<Long> ids = resolveStationIds(filter.stations());
            if (!ids.isEmpty()) {
                tickets = tickets.stream()
                    .filter(t -> t.getAssignedStation() != null
                              && ids.contains(t.getAssignedStation().getId()))
                    .toList();
            }
        }

        if ("station".equals(filter.sortBy())) {
            tickets = tickets.stream()
                .sorted(Comparator.comparing(
                    t -> t.getAssignedStation() != null ? t.getAssignedStation().getName() : "zzz",
                    Comparator.naturalOrder()))
                .toList();
        }

        // 1. Get IDs for batch fetching
        List<Long> menuIds = tickets.stream().map(KitchenTicket::getMenuItemId).distinct().toList();
        List<Long> itemIds = tickets.stream().map(KitchenTicket::getOrderItemId).distinct().toList();

        // 2. Fetch related data (assuming repositories are injected)
        Map<Long, MenuItem> menuMap = menuRepo.findAllById(menuIds).stream()
            .collect(Collectors.toMap(MenuItem::getId, m -> m));
        Map<Long, OrderItem> itemMap = orderItemRepo.findAllById(itemIds).stream()
            .collect(Collectors.toMap(OrderItem::getId, i -> i));

        // 3. Map to new Response format
        return tickets.stream().map(t -> {
            MenuItem menu = menuMap.get(t.getMenuItemId());
            OrderItem orderItem = itemMap.get(t.getOrderItemId());

            return new KitchenTicketResponse(
                t.getId(),
                new OrderItemResponse(orderItem.getId(), orderItem.getMenuItemId(), orderItem.getQuantity(),orderItem.getSpecialNotes(), orderItem.getOptions() ),
                new MenuItemResponse(menu.getId(), menu.getName()),
                t.getQuantity(),
                t.getCurrentStateName(),
                t.getStartedAt(),
                t.getFinishedAt(),
                t.getDeadlineTime(),
                t.isNearDeadline(),
                t.getAssignedStation() != null ? StationResponse.from(t.getAssignedStation()) : null
            );
        }).toList();

        // return tickets.stream().map(KitchenTicketResponse::from).toList();
    }

    // ── getAvailableStations ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<StationResponse> getAvailableStations(String type) {
        List<Station> stations = (type != null && !type.isBlank())
            ? stationRepo.findByTypeAndStatus(type, "AVAILABLE")
            : stationRepo.findByStatus("AVAILABLE");
        return stations.stream().map(StationResponse::from).toList();
    }

    // ── processStartCooking(ticketId, stationId) ──────────────────────────────

    @Override
    public KitchenTicketResponse processStartCooking(Long ticketId, Long stationId) {
        KitchenTicket ticket  = findTicketOrThrow(ticketId);
        Station       station = findStationOrThrow(stationId);

        // State machine: QueuedState/PausedState.startCooking() → station.markInUse() + assign
        ticket.startCooking(station);

        // Persist: cần save cả station (status thay đổi) và ticket
        stationRepo.save(station);
        KitchenTicket saved = kitchenRepo.save(ticket);

        log.info("Ticket {} COOKING on station '{}' (id={})",
            ticketId, station.getName(), stationId);
        return KitchenTicketResponse.from(saved);
    }

    // ── processMarkDone ───────────────────────────────────────────────────────

    @Override
    public KitchenTicketResponse processMarkDone(Long ticketId) {
        KitchenTicket ticket  = findTicketOrThrow(ticketId);
        Station       station = ticket.getAssignedStation(); // lưu ref trước khi freeStation()

        ticket.markDone();  // CookingState.markDone() → freeStation() → station.markAvailable()

        if (station != null) stationRepo.save(station);  // persist station status = AVAILABLE
        KitchenTicket saved = kitchenRepo.save(ticket);

        eventPublisher.publishEvent(new KitchenItemDoneEvent(
            saved.getMenuItemId(),
            saved.getQuantity(), saved.getOrderItemId()
        ));

        log.info("Ticket {} READY — station freed, KitchenItemDoneEvent published", ticketId);
        return KitchenTicketResponse.from(saved);
    }

    // ── processPause ──────────────────────────────────────────────────────────

    @Override
    public KitchenTicketResponse processPause(Long ticketId) {
        KitchenTicket ticket  = findTicketOrThrow(ticketId);
        Station       station = ticket.getAssignedStation();

        ticket.pause();  // CookingState.pause() → freeStation()

        if (station != null) stationRepo.save(station);
        return KitchenTicketResponse.from(kitchenRepo.save(ticket));
    }

    // ── processUndo ───────────────────────────────────────────────────────────

    @Override
    public KitchenTicketResponse processUndo(Long ticketId) {
        KitchenTicket ticket  = findTicketOrThrow(ticketId);
        Station       station = ticket.getAssignedStation();

        ticket.undo();  // freeStation() nếu đang COOKING

        if (station != null) stationRepo.save(station);
        return KitchenTicketResponse.from(kitchenRepo.save(ticket));
    }

    // ── getSLAData ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SLAData> getSLAData(LocalDateTime startDate, LocalDateTime endDate) {
        List<KitchenTicket> completed = kitchenRepo.findCompletedBetween(startDate, endDate);

        Map<Long, List<KitchenTicket>> byItem = completed.stream()
            .collect(Collectors.groupingBy(KitchenTicket::getMenuItemId));

        return byItem.entrySet().stream().map(entry -> {
            Long menuItemId = entry.getKey();
            List<KitchenTicket> tickets = entry.getValue();

            double avgMinutes = tickets.stream()
                .filter(t -> t.getStartedAt() != null && t.getFinishedAt() != null)
                .mapToLong(t -> Duration.between(t.getStartedAt(), t.getFinishedAt()).toMinutes())
                .average().orElse(0);

            long overdue = tickets.stream()
                .filter(t -> t.getFinishedAt() != null && t.getDeadlineTime() != null
                          && t.getFinishedAt().isAfter(t.getDeadlineTime()))
                .count();

            return new SLAData(menuItemId, "Item #" + menuItemId,
                avgMinutes, tickets.size(), overdue);
        }).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private KitchenTicket findTicketOrThrow(Long id) {
        return kitchenRepo.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("KitchenTicket", id));
    }

    private Station findStationOrThrow(Long id) {
        return stationRepo.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("Station", id));
    }

    /** Trả về stationId nếu filter chỉ có 1 station name, dùng cho Spec query. */
    private Long resolveSingleStationId(List<String> stationNames) {
        if (stationNames == null || stationNames.size() != 1) return null;
        return stationRepo.findByType(stationNames.get(0)).stream()
            .findFirst().map(Station::getId).orElse(null);
    }

    /** Resolve list of station names/types → IDs for in-memory multi-filter. */
    private List<Long> resolveStationIds(List<String> stationNames) {
        return stationNames.stream()
            .flatMap(name -> stationRepo.findByType(name).stream())
            .map(Station::getId)
            .distinct()
            .toList();
    }
}
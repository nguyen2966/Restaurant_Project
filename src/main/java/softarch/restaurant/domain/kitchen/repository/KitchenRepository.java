package softarch.restaurant.domain.kitchen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.kitchen.entity.KitchenTicket;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KitchenRepository extends JpaRepository<KitchenTicket, Long> {

    List<KitchenTicket> findByStation(String station);

    List<KitchenTicket> findByStatusEnumValue(String status);

    /**
     * Flexible filtered query — matches diagram: findByFilters(filter).
     * All parameters are optional; null means "no filter on this field".
     */
    @Query("""
        SELECT kt FROM KitchenTicket kt
        WHERE (:status   IS NULL OR kt.statusEnumValue = :status)
          AND (:station  IS NULL OR kt.station = :station)
          AND (:deadline IS NULL OR kt.deadlineTime <= :deadline)
        ORDER BY kt.deadlineTime ASC
        """)
    List<KitchenTicket> findByFilters(String status, String station, LocalDateTime deadline);

    /** For SLA report — tickets completed between two dates. */
    @Query("""
        SELECT kt FROM KitchenTicket kt
        WHERE kt.finishedAt BETWEEN :from AND :to
        """)
    List<KitchenTicket> findCompletedBetween(LocalDateTime from, LocalDateTime to);
}
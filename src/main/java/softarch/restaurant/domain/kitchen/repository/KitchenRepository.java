package softarch.restaurant.domain.kitchen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.kitchen.entity.KitchenTicket;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KitchenRepository
        extends JpaRepository<KitchenTicket, Long>,
                JpaSpecificationExecutor<KitchenTicket> {

    List<KitchenTicket> findByStation(String station);

    List<KitchenTicket> findByStatusEnumValue(String status);

    /**
     * Dùng cho SLA report — tickets đã hoàn thành trong khoảng thời gian.
     * Cả hai params đều NOT NULL nên query này không bị lỗi type inference.
     */
    @Query("""
        SELECT kt FROM KitchenTicket kt
        WHERE kt.finishedAt BETWEEN :from AND :to
        """)
    List<KitchenTicket> findCompletedBetween(LocalDateTime from, LocalDateTime to);
}

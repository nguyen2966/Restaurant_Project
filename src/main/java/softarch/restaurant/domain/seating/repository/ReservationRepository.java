package softarch.restaurant.domain.seating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.seating.entity.*;

import java.util.List;


// ── Reservation ───────────────────────────────────────────────────────────

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByTableIdAndStatus(Long tableId, ReservationStatus status);
}

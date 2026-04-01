package softarch.restaurant.domain.seating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.seating.entity.WaitlistEntry;

import java.util.List;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByIsNotifiedFalseOrderByJoinedAtAsc();
}
package softarch.restaurant.domain.kitchen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.kitchen.entity.Station;

import java.util.List;

/**
 * Matches diagram: StationRepository.
 * findByTypeAndStatus() — dùng bởi KitchenService.getAvailableStations()
 */
@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    // Matches diagram: findByTypeAndStatus(type, status): List<Station>
    List<Station> findByTypeAndStatus(String type, String status);

    // Tất cả station available (không filter theo type)
    List<Station> findByStatus(String status);

    // Tất cả station theo type
    List<Station> findByType(String type);
}
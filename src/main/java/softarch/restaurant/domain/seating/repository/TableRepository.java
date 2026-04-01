package softarch.restaurant.domain.seating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.seating.entity.RestaurantTable;
import softarch.restaurant.domain.seating.entity.TableStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, Long> {

    List<RestaurantTable> findByStatus(TableStatus status);

    Optional<RestaurantTable> findByTableCode(String tableCode);

    @Query("SELECT t FROM RestaurantTable t WHERE t.status = 'AVAILABLE' AND t.capacity >= :minCapacity ORDER BY t.capacity ASC")
    List<RestaurantTable> findAvailableWithCapacity(int minCapacity);
}
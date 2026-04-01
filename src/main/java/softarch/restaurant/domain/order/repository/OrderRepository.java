package softarch.restaurant.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.order.entity.Order;
import softarch.restaurant.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByTableId(Long tableId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
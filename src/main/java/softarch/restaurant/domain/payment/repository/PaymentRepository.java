package softarch.restaurant.domain.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.payment.entity.PaymentTransaction;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrderId(Long orderId);
}

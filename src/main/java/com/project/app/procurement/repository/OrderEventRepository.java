package com.project.app.procurement.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.app.procurement.model.OrderEvent;
public interface OrderEventRepository extends JpaRepository<OrderEvent,Long> {
    List<OrderEvent> findByOrderIdOrderByIdAsc(Long orderId);
    boolean existsByOrderIdAndTargetStatus(Long orderId,String targetStatus);
}

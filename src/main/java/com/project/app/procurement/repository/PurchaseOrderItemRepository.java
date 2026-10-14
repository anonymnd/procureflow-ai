package com.project.app.procurement.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.app.procurement.model.PurchaseOrderItem;
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem,Long> {
    List<PurchaseOrderItem> findByOrderIdOrderByRfqItem_IdAsc(Long orderId);
}

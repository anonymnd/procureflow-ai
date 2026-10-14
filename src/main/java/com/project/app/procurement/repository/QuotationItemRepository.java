package com.project.app.procurement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.project.app.procurement.model.QuotationItem;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {
    List<QuotationItem> findByQuotationIdOrderByRfqItemIdAsc(Long quotationId);
    @Modifying(flushAutomatically = true)
    @Query("delete from QuotationItem i where i.quotation.id = :quotationId")
    void deleteForQuotation(@Param("quotationId") Long quotationId);
}

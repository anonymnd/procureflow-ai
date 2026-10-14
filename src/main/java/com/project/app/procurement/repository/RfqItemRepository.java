package com.project.app.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.app.procurement.model.RfqItem;

public interface RfqItemRepository extends JpaRepository<RfqItem, Long> {
    List<RfqItem> findByRfqIdOrderByIdAsc(Long rfqId);

    long countByRfqId(Long rfqId);

    @Query("select i from RfqItem i where i.id = :id and i.rfq.id = :rfqId and i.rfq.organization.id = :organizationId")
    Optional<RfqItem> findByIdAndRfqIdAndOwningOrganizationId(@Param("id") Long id, @Param("rfqId") Long rfqId,
            @Param("organizationId") Long organizationId);
}

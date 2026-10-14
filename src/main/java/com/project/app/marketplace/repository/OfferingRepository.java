package com.project.app.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.app.marketplace.model.Offering;
import com.project.app.marketplace.model.Offering.Category;

public interface OfferingRepository extends JpaRepository<Offering, Long> {
    @Query("select o from Offering o where o.id = :id and o.profile.organization.id = :organizationId")
    Optional<Offering> findByIdAndOwningOrganizationId(@Param("id") Long id,
            @Param("organizationId") Long organizationId);

    @Query("select o from Offering o where o.profile.organization.id = :organizationId order by o.id desc")
    List<Offering> findAllByOwningOrganizationId(@Param("organizationId") Long organizationId);

    long countByProfileId(Long profileId);

    /** {@code likePattern} must already be lower-cased and have %, _ and \ escaped by the caller; null skips the text filter. */
    @Query(value = """
            select o from Offering o
            where o.published = true and o.profile.published = true
              and (:category is null or o.category = :category)
              and (:likePattern is null
                   or lower(o.title) like :likePattern escape '\\'
                   or lower(o.description) like :likePattern escape '\\'
                   or lower(o.profile.organization.displayName) like :likePattern escape '\\')
            order by o.publishedAt desc, o.id desc
            """,
            countQuery = """
            select count(o) from Offering o
            where o.published = true and o.profile.published = true
              and (:category is null or o.category = :category)
              and (:likePattern is null
                   or lower(o.title) like :likePattern escape '\\'
                   or lower(o.description) like :likePattern escape '\\'
                   or lower(o.profile.organization.displayName) like :likePattern escape '\\')
            """)
    Page<Offering> searchPublished(@Param("category") Category category, @Param("likePattern") String likePattern,
            Pageable pageable);

    @Query("select o from Offering o where o.profile.id = :profileId and o.published = true order by o.publishedAt desc, o.id desc")
    List<Offering> findPublishedByProfileId(@Param("profileId") Long profileId);

    @Query("select o from Offering o where o.id = :id and o.published = true and o.profile.published = true")
    Optional<Offering> findPublishedById(@Param("id") Long id);
}

package com.project.app.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.app.identity.model.IdentityToken;

import jakarta.persistence.LockModeType;

public interface IdentityTokenRepository extends JpaRepository<IdentityToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from IdentityToken t where t.tokenHash = :hash and t.purpose = :purpose and t.consumedAt is null")
    Optional<IdentityToken> findUnusedForUpdate(@Param("hash") String hash,
            @Param("purpose") IdentityToken.Purpose purpose);

    @Query("select t from IdentityToken t where t.purpose = :purpose and t.account.id = :accountId and t.consumedAt is null")
    List<IdentityToken> findUnusedForAccount(@Param("purpose") IdentityToken.Purpose purpose,
            @Param("accountId") Long accountId);

    @Query("select t from IdentityToken t where t.purpose = :purpose and t.organization.id = :organizationId and t.email = :email and t.consumedAt is null")
    List<IdentityToken> findUnusedInvitations(@Param("purpose") IdentityToken.Purpose purpose,
            @Param("organizationId") Long organizationId, @Param("email") String email);
}

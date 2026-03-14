package com.portal.lostfound.repository;

import com.portal.lostfound.model.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByItemId(Long itemId);

    Page<Claim> findByClaimantId(Long claimantId, Pageable pageable);

    boolean existsByItemIdAndClaimantId(Long itemId, Long claimantId);

    Optional<Claim> findByItemIdAndClaimantId(Long itemId, Long claimantId);

    Page<Claim> findByStatus(Claim.ClaimStatus status, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.status = 'PENDING'")
    long countPendingClaims();

    @Query("SELECT c FROM Claim c WHERE c.item.postedBy.id = :ownerId " +
           "AND c.status = 'PENDING' ORDER BY c.createdAt DESC")
    List<Claim> findPendingClaimsForOwner(@Param("ownerId") Long ownerId);
}

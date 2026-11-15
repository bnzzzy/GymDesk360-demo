package com.benz.gymdesk360.repository;

import com.benz.gymdesk360.model.Membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Todas las membresías del gimnasio (incluye anuladas). */
    List<Membership> findAllByGymId(Long gymId);

    List<Membership> findAllByMember_IdOrderByStartDateDesc(Long memberId);

    Optional<Membership> findByIdAndGymId(Long id, Long gymId);

    boolean existsByMember_Id(Long memberId);

    boolean existsByPlanId(Long planId);
}

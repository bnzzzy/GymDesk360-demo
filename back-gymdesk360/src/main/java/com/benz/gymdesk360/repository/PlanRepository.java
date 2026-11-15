package com.benz.gymdesk360.repository;

import com.benz.gymdesk360.model.Plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findAllByGymIdOrderByActiveDescPriceAsc(Long gymId);

    Optional<Plan> findByIdAndGymId(Long id, Long gymId);

    boolean existsByGymIdAndNameIgnoreCase(Long gymId, String name);

    boolean existsByGymIdAndNameIgnoreCaseAndIdNot(Long gymId, String name, Long id);
}

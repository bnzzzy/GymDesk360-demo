package com.benz.gymdesk360.repository;

import com.benz.gymdesk360.model.Member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByGymIdOrderByLastNameAscFirstNameAsc(Long gymId);

    Optional<Member> findByIdAndGymId(Long id, Long gymId);

    boolean existsByGymIdAndRut(Long gymId, String rut);

    boolean existsByGymIdAndRutAndIdNot(Long gymId, String rut, Long id);
}

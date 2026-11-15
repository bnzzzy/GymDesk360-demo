package com.benz.gymdesk360.repository;

import com.benz.gymdesk360.model.Gym;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymRepository extends JpaRepository<Gym, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByActiveTrue();

    List<Gym> findAllByOrderByNameAsc();
}

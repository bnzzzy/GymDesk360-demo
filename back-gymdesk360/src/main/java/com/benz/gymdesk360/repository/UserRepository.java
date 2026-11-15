package com.benz.gymdesk360.repository;

import com.benz.gymdesk360.model.User;
import com.benz.gymdesk360.model.enums.Role;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "gym")
    List<User> findAllByGym_IdOrderByFullNameAsc(Long gymId);

    @EntityGraph(attributePaths = "gym")
    List<User> findAllByOrderByFullNameAsc();

    long countByRole(Role role);

    /** Filas [gymId, cantidad] de usuarios por gimnasio. */
    @Query("select u.gym.id, count(u) from User u where u.gym is not null group by u.gym.id")
    List<Object[]> countUsersPerGym();

    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countUsersPerRole();

    long countByGym_Id(Long gymId);

    @Modifying
    @Query("delete from User u where u.gym.id = :gymId")
    int deleteAllByGymId(@Param("gymId") Long gymId);
}

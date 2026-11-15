package com.benz.gymdesk360.repository;

import com.benz.gymdesk360.model.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select rt from RefreshToken rt join fetch rt.user where rt.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true "
            + "where rt.revoked = false and rt.user.id in (select u.id from User u where u.gym.id = :gymId)")
    int revokeAllForGym(@Param("gymId") Long gymId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :limit")
    int deleteExpiredBefore(@Param("limit") Instant limit);
}

package com.saasai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.saasai.entity.User;

import com.saasai.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findById(String userId);

    @Query("""
    SELECT COUNT(u)
    FROM User u
    WHERE u.lastLoginAt >= :since
    """)
    long countActiveUsersSince(@Param("since") LocalDateTime since);

}

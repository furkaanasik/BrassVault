package com.brassvault.api.repo;

import com.brassvault.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            JOIN TeamMember tm ON tm.id.userId = u.id AND tm.id.teamId = :teamId
            ORDER BY u.fullName
            """)
    List<User> findAllByTeamId(@Param("teamId") Long teamId);
}

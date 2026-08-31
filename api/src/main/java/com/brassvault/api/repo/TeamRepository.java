package com.brassvault.api.repo;

import com.brassvault.api.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);

    @Query("""
            SELECT t FROM Team t
            JOIN TeamMember tm ON tm.id.teamId = t.id AND tm.id.userId = :userId
            ORDER BY t.name
            """)
    List<Team> findAllByMemberUserId(@Param("userId") Long userId);
}

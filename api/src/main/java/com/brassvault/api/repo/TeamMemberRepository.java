package com.brassvault.api.repo;

import com.brassvault.api.domain.TeamMember;
import com.brassvault.api.domain.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
}

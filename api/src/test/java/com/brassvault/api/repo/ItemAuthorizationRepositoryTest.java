package com.brassvault.api.repo;

import com.brassvault.api.TestcontainersConfiguration;
import com.brassvault.api.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ItemAuthorizationRepositoryTest {

    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired TeamMemberRepository members;
    @Autowired ItemRepository items;

    private User member;
    private User outsider;
    private Team team;
    private Item item;

    @BeforeEach
    void setUp() {
        items.deleteAll();
        members.deleteAll();
        teams.deleteAll();
        users.deleteAll();

        member = user("member@brassvault.local");
        outsider = user("outsider@brassvault.local");
        team = team("Marco Polo");
        members.save(new TeamMember(team.getId(), member.getId()));
        item = item(team, "Prod DB");
    }

    @Test
    @DisplayName("Flyway schema is in place and entities persist")
    void schemaAndEntitiesWork() {
        assertThat(users.findByEmail("member@brassvault.local")).isPresent();
        assertThat(teams.existsByName("Marco Polo")).isTrue();
        assertThat(items.findById(item.getId())).isPresent();
    }

    @Test
    @DisplayName("team member can load the item through the authorized query")
    void memberSeesItem() {
        assertThat(items.findByIdForUser(item.getId(), member.getId())).isPresent();
        assertThat(items.findAllByTeamIdForUser(team.getId(), member.getId())).hasSize(1);
    }

    @Test
    @DisplayName("non-member gets empty result — the item does not exist for them")
    void outsiderSeesNothing() {
        assertThat(items.findByIdForUser(item.getId(), outsider.getId())).isEmpty();
        assertThat(items.findAllByTeamIdForUser(team.getId(), outsider.getId())).isEmpty();
    }

    @Test
    @DisplayName("removing membership removes access immediately")
    void removedMemberLosesAccess() {
        members.deleteById(new TeamMemberId(team.getId(), member.getId()));
        assertThat(items.findByIdForUser(item.getId(), member.getId())).isEmpty();
    }

    @Test
    @DisplayName("teams a user belongs to are listed through the membership join")
    void teamsByMember() {
        assertThat(teams.findAllByMemberUserId(member.getId())).extracting(Team::getName)
                .containsExactly("Marco Polo");
        assertThat(teams.findAllByMemberUserId(outsider.getId())).isEmpty();
    }

    @Test
    @DisplayName("entity toString leaks no encrypted password bytes")
    void toStringExcludesSecret() {
        assertThat(item.toString()).doesNotContain("encryptedPassword");
    }

    private User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFullName("Test User");
        u.setPasswordHash("$2a$12$notarealhashnotarealhashnotarealhash");
        u.setRole(Role.USER);
        return users.save(u);
    }

    private Team team(String name) {
        Team t = new Team();
        t.setName(name);
        return teams.save(t);
    }

    private Item item(Team t, String title) {
        Item i = new Item();
        i.setTeamId(t.getId());
        i.setTitle(title);
        i.setUsername("dbuser");
        i.setEncryptedPassword(new byte[]{1, 2, 3});
        i.setCreatedBy(member.getId());
        return items.save(i);
    }
}

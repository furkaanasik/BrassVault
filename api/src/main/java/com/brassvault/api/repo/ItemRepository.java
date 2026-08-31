package com.brassvault.api.repo;

import com.brassvault.api.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Authorization happens IN the query: every user-facing lookup either joins
 * team_members on the requesting user's id or matches the item's owner_id.
 * A user can never load an item of a team they are not a member of, nor
 * another user's personal item — such an item simply does not exist for
 * them (404, not 403).
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
            SELECT i FROM Item i
            JOIN TeamMember tm ON tm.id.teamId = i.teamId AND tm.id.userId = :userId
            WHERE i.teamId = :teamId
            ORDER BY i.title
            """)
    List<Item> findAllByTeamIdForUser(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Query("""
            SELECT i FROM Item i
            WHERE i.id = :itemId AND (
                i.ownerId = :userId OR
                EXISTS (SELECT 1 FROM TeamMember tm
                        WHERE tm.id.teamId = i.teamId AND tm.id.userId = :userId)
            )
            """)
    Optional<Item> findByIdForUser(@Param("itemId") Long itemId, @Param("userId") Long userId);

    @Query("""
            SELECT i FROM Item i
            WHERE i.ownerId = :ownerId
            ORDER BY i.title
            """)
    List<Item> findAllByOwnerId(@Param("ownerId") Long ownerId);

    Optional<Item> findByIdAndOwnerId(Long id, Long ownerId);
}

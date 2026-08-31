package com.brassvault.api.web;

import com.brassvault.api.security.AuthPrincipal;
import com.brassvault.api.service.ItemService;
import com.brassvault.api.web.dto.ItemDtos.ItemResponse;
import com.brassvault.api.web.dto.ItemDtos.SecretResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /** Item list for a team — passwords never leave the server here. */
    @GetMapping("/api/teams/{teamId}/items")
    public List<ItemResponse> teamItems(@PathVariable Long teamId,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return itemService.listTeamItems(teamId, principal.id()).stream()
                .map(ItemResponse::from)
                .toList();
    }

    /**
     * Separate endpoint so the audit trail answers "who looked at which secret".
     * Non-members get 404 — the item's existence is not leaked.
     */
    @GetMapping("/api/items/{id}/secret")
    public SecretResponse secret(@PathVariable Long id,
                                 @AuthenticationPrincipal AuthPrincipal principal,
                                 HttpServletRequest request) {
        String password = itemService.revealSecret(id, principal.id(), principal.email(),
                request.getRemoteAddr());
        return new SecretResponse(password);
    }
}

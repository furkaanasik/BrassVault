package com.brassvault.api.web;

import com.brassvault.api.security.AuthPrincipal;
import com.brassvault.api.service.ItemService;
import com.brassvault.api.web.dto.ItemDtos.CreatePersonalItemRequest;
import com.brassvault.api.web.dto.ItemDtos.ItemResponse;
import com.brassvault.api.web.dto.ItemDtos.UpdateItemRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vault/items")
public class VaultController {

    private final ItemService itemService;

    public VaultController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<ItemResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return itemService.listPersonalItems(principal.id()).stream()
                .map(ItemResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreatePersonalItemRequest body,
                                               @AuthenticationPrincipal AuthPrincipal principal,
                                               HttpServletRequest request) {
        var item = itemService.createPersonalItem(body.toInput(),
                principal.id(), principal.email(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(item));
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateItemRequest body,
                               @AuthenticationPrincipal AuthPrincipal principal,
                               HttpServletRequest request) {
        var item = itemService.updatePersonalItem(id, principal.id(), body.toInput(),
                principal.email(), request.getRemoteAddr());
        return ItemResponse.from(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthPrincipal principal,
                                       HttpServletRequest request) {
        itemService.deletePersonalItem(id, principal.id(), principal.email(),
                request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}

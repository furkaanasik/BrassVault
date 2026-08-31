package com.brassvault.api.web.admin;

import com.brassvault.api.security.AuthPrincipal;
import com.brassvault.api.service.ItemService;
import com.brassvault.api.web.dto.ItemDtos.CreateItemRequest;
import com.brassvault.api.web.dto.ItemDtos.ItemResponse;
import com.brassvault.api.web.dto.ItemDtos.UpdateItemRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/items")
public class AdminItemController {

    private final ItemService itemService;

    public AdminItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest body,
                                               @AuthenticationPrincipal AuthPrincipal principal,
                                               HttpServletRequest request) {
        var item = itemService.createItem(body.teamId(), body.title(), body.username(),
                body.password(), body.url(), body.notes(),
                principal.id(), principal.email(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(item));
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateItemRequest body,
                               @AuthenticationPrincipal AuthPrincipal principal,
                               HttpServletRequest request) {
        var item = itemService.updateItem(id, body.title(), body.username(), body.password(),
                body.url(), body.notes(), principal.email(), request.getRemoteAddr());
        return ItemResponse.from(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthPrincipal principal,
                                       HttpServletRequest request) {
        itemService.deleteItem(id, principal.email(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}

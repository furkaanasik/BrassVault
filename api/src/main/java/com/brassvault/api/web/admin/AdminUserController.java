package com.brassvault.api.web.admin;

import com.brassvault.api.service.UserAdminService;
import com.brassvault.api.web.dto.Dtos.CreateUserRequest;
import com.brassvault.api.web.dto.Dtos.CreatedUserResponse;
import com.brassvault.api.web.dto.Dtos.UpdateUserRequest;
import com.brassvault.api.web.dto.Dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @PostMapping
    public ResponseEntity<CreatedUserResponse> create(@Valid @RequestBody CreateUserRequest body) {
        var created = userAdminService.createUser(body.email(), body.fullName(), body.role());
        // The temporary password appears in this response once and is never retrievable again.
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedUserResponse(
                created.user().getId(), created.user().getEmail(), created.user().getFullName(),
                created.user().getRole(), created.tempPassword()));
    }

    @GetMapping
    public List<UserResponse> list() {
        return userAdminService.listUsers().stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest body) {
        return UserResponse.from(userAdminService.updateUser(id, body.role(), body.active()));
    }
}

package com.brassvault.api.security;

import com.brassvault.api.domain.Role;

public record AuthPrincipal(Long id, String email, Role role, boolean mustChangePassword) {
}

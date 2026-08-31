package com.brassvault.api.web;

import com.brassvault.api.domain.User;
import com.brassvault.api.security.JwtAuthFilter;
import com.brassvault.api.security.JwtService;
import com.brassvault.api.service.AuthService;
import com.brassvault.api.web.dto.Dtos.LoginRequest;
import com.brassvault.api.web.dto.Dtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest body,
                                              HttpServletRequest request) {
        User user = authService.login(body.email(), body.password(), request.getRemoteAddr());
        String token = jwtService.issue(user.getId(), user.getEmail(), user.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie(token, jwtService.ttl()).toString())
                .body(UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, accessCookie("", Duration.ZERO).toString())
                .build();
    }

    private static ResponseCookie accessCookie(String value, Duration maxAge) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}

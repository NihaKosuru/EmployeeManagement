package com.example.ems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Current User", description = "Who is logged in")
public class AuthController {

    @GetMapping
    @Operation(summary = "Get the logged-in username and role (ADMIN or USER)")
    public Map<String, String> currentUser(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return Map.of(
                "username", authentication.getName(),
                "role", isAdmin ? "ADMIN" : "USER");
    }
}

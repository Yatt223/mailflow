package com.mailflow.auth;

import com.mailflow.utilisateur.Role;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;

public record Acteur(UUID id, UUID organisationId, Role role) {
    public static Acteur de(Jwt jwt) {
        return new Acteur(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("org")),
                Role.valueOf(jwt.getClaimAsString("role")));
    }

    public boolean estGestionnaire() {
        return role == Role.ADMIN || role == Role.MANAGER;
    }
}
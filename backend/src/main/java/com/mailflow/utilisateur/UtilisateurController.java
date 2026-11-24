package com.mailflow.utilisateur;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UtilisateurController {
    private final UtilisateurService service;

    @GetMapping("/moi")
    public Map<String, Object> moi(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "id", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "role", jwt.getClaimAsString("role"),
                "organisationId", jwt.getClaimAsString("org"));
    }

    @GetMapping("/utilisateurs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UtilisateurResponse> lister(@AuthenticationPrincipal Jwt jwt) {
        UUID orgId = UUID.fromString(jwt.getClaimAsString("org"));
        return service.lister(orgId).stream().map(UtilisateurResponse::from).toList();
    }

    @PostMapping("/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurResponse creer(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody CreationUtilisateurRequest req) {
        UUID orgId = UUID.fromString(jwt.getClaimAsString("org"));
        return UtilisateurResponse.from(service.creer(orgId, req.email(), req.motDePasse(),
                req.prenom(), req.nom(), req.role()));
    }
}
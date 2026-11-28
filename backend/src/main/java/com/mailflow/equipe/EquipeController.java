package com.mailflow.equipe;

import com.mailflow.audit.Audit;
import com.mailflow.auth.Acteur;
import com.mailflow.equipe.EquipeDtos.*;
import com.mailflow.utilisateur.UtilisateurResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipes")
@RequiredArgsConstructor
public class EquipeController {
    private final EquipeService service;

    private static final String GESTIONNAIRES = "hasAnyRole('ADMIN','MANAGER')";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTIONNAIRES)
    @Audit(action = "EQUIPE_CREEE", type = "EQUIPE")
    public EquipeResponse creer(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody EquipeRequest req) {
        return EquipeResponse.from(service.creer(Acteur.de(jwt), req.nom()));
    }

    @GetMapping
    public List<EquipeResponse> lister(@AuthenticationPrincipal Jwt jwt) {
        return service.lister(Acteur.de(jwt)).stream().map(EquipeResponse::from).toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize(GESTIONNAIRES)
    @Audit(action = "EQUIPE_RENOMMEE", type = "EQUIPE")
    public EquipeResponse renommer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody EquipeRequest req) {
        return EquipeResponse.from(service.renommer(Acteur.de(jwt), id, req.nom()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(action = "EQUIPE_SUPPRIMMEE", type = "EQUIPE")
    public void supprimer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.supprimer(Acteur.de(jwt), id);
    }

    @GetMapping("/{id}/membres")
    public List<UtilisateurResponse> membres(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID id) {
        return service.membres(Acteur.de(jwt), id).stream().map(UtilisateurResponse::from).toList();
    }

    @PutMapping("/{id}/membres/{utilisateurId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(GESTIONNAIRES)
    @Audit(action = "EQUIPE_MEMBRE_AJOUTE", type = "EQUIPE")
    public void ajouterMembre(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @PathVariable UUID utilisateurId) {
        service.ajouterMembre(Acteur.de(jwt), id, utilisateurId);
    }

    @DeleteMapping("/{id}/membres/{utilisateurId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(GESTIONNAIRES)
    @Audit(action = "EQUIPE_MEMBRE_RETIRE", type = "EQUIPE")
    public void retirerMembre(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @PathVariable UUID utilisateurId) {
        service.retirerMembre(Acteur.de(jwt), id, utilisateurId);
    }
}
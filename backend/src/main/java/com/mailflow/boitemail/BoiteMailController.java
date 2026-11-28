package com.mailflow.boitemail;

import com.mailflow.auth.Acteur;
import com.mailflow.boitemail.BoiteMailDtos.*;
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
@RequestMapping("/api/boites-mail")
@RequiredArgsConstructor
public class BoiteMailController {
    private final BoiteMailService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public BoiteMailResponse creer(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody CreationBoiteRequest req) {
        return BoiteMailResponse.from(service.creer(Acteur.de(jwt), req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<BoiteMailResponse> lister(@AuthenticationPrincipal Jwt jwt) {
        return service.lister(Acteur.de(jwt)).stream().map(BoiteMailResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BoiteMailResponse lire(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return BoiteMailResponse.from(service.lire(Acteur.de(jwt), id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BoiteMailResponse modifier(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody ModificationBoiteRequest req) {
        return BoiteMailResponse.from(
                service.modifier(Acteur.de(jwt), id, req.actif(), req.motDePasse()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void supprimer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.supprimer(Acteur.de(jwt), id);
    }

    @PostMapping("/{id}/synchroniser")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SynchroResponse synchroniser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var r = service.synchroniser(Acteur.de(jwt), id);
        return new SynchroResponse(r.importes(), r.erreurs());
    }

    @PostMapping("/{id}/resynchroniser")
    @PreAuthorize("hasRole('ADMIN')")
    public SynchroResponse resynchroniser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var r = service.resynchroniser(Acteur.de(jwt), id);
        return new SynchroResponse(r.importes(), r.erreurs());
    }
}
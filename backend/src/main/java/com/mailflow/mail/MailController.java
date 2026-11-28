package com.mailflow.mail;

import com.mailflow.auth.Acteur;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.mailflow.audit.Audit;

@RestController
@RequestMapping("/api/mails")
@RequiredArgsConstructor
public class MailController {
    private final MailService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','AGENT')")
    @Audit(action = "MAIL_CREE", type = "MAIL")
    public MailResponse creer(@AuthenticationPrincipal Jwt jwt,
                              @Valid @RequestBody CreationMailRequest req) {
        return MailResponse.from(service.creer(Acteur.de(jwt), req));
    }

    @GetMapping
    public PagedModel<MailResumeResponse> rechercher(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatutMail statut,
            @RequestParam(required = false) UUID assigneA,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "recuLe"));
        String recherche = (q != null && q.length() > 100) ? q.substring(0, 100) : q;
        return new PagedModel<>(service
                .rechercher(Acteur.de(jwt), statut, assigneA, recherche, pageable)
                .map(MailResumeResponse::from));
    }

    @GetMapping("/{id}")
    @Audit(action = "MAIL_LU", type = "MAIL")
    public MailResponse lire(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return MailResponse.from(service.lire(Acteur.de(jwt), id));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','AGENT')")
    @Audit(action = "MAIL_STATUT_CHANGE", type = "MAIL")
    public MailResponse changerStatut(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody ChangementStatutRequest req) {
        return MailResponse.from(service.changerStatut(Acteur.de(jwt), id, req.statut()));
    }

    @PutMapping("/{id}/assignation")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Audit(action = "MAIL_ASSIGNE", type = "MAIL")
    public MailResponse assigner(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody AssignationRequest req) {
        return MailResponse.from(service.assigner(Acteur.de(jwt), id, req.utilisateurId()));
    }

    @PostMapping("/{id}/prendre")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','AGENT')")
    @Audit(action = "MAIL_PRIS", type = "MAIL")
    public MailResponse prendre(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return MailResponse.from(service.prendre(Acteur.de(jwt), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(action = "MAIL_SUPPRIME", type = "MAIL")
    public void supprimer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.supprimer(Acteur.de(jwt), id);
    }
}
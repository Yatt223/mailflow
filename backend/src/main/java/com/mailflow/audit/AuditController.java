package com.mailflow.audit;

import com.mailflow.audit.AuditDtos.EntreeAuditResponse;
import com.mailflow.auth.Acteur;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITEUR')")
    @Audit(action = "AUDIT_CONSULTE", type = "AUDIT")
    public PagedModel<EntreeAuditResponse> consulter(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam(required = false) String typeObjet,
            @RequestParam(required = false) UUID objetId,
            @RequestParam(required = false) Instant du,
            @RequestParam(required = false) Instant au,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Acteur a = Acteur.de(jwt);
        return new PagedModel<>(service.consulter(a.organisationId(), action, utilisateurId,
                typeObjet, objetId, du, au, Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
    }
}
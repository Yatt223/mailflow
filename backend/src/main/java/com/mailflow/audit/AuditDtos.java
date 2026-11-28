package com.mailflow.audit;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {}

    // `details` est du JSON généré et échappé par AuditService : il est renvoyé tel quel.
    public record EntreeAuditResponse(UUID id, UUID utilisateurId, String utilisateurEmail,
                                      String action, String typeObjet, UUID objetId,
                                      @JsonRawValue String details, Instant creeLe) {}
}
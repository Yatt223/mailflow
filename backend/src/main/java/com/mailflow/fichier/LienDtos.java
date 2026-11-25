package com.mailflow.fichier;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;

public final class LienDtos {
    private LienDtos() {}

    public record CreationLienRequest(@Min(1) @Max(168) Integer dureeHeures,
                                      @Min(1) @Max(100) Integer maxTelechargements) {}

    public record LienCreeResponse(UUID id, String jeton, String chemin,
                                   Instant expireLe, Integer maxTelechargements) {}

    public record LienResponse(UUID id, Instant expireLe, Integer maxTelechargements,
                               int nbTelechargements, boolean actif, Instant creeLe) {
        public static LienResponse from(LienPartage l) {
            boolean actif = l.getExpireLe().isAfter(Instant.now())
                    && (l.getMaxTelechargements() == null
                    || l.getNbTelechargements() < l.getMaxTelechargements());
            return new LienResponse(l.getId(), l.getExpireLe(), l.getMaxTelechargements(),
                    l.getNbTelechargements(), actif, l.getCreeLe());
        }
    }
}
package com.mailflow.fichier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class FichierDtos {
    private FichierDtos() {}

    public record FichierResponse(UUID id, String nom, String typeMime, long tailleOctets,
                                  UUID proprietaireId, UUID mailId, Instant creeLe) {
        public static FichierResponse from(Fichier f) {
            return new FichierResponse(f.getId(), f.getNom(), f.getTypeMime(),
                    f.getTailleOctets(), f.getProprietaireId(), f.getMailId(), f.getCreeLe());
        }
    }

    public record RenommageRequest(@NotBlank @Size(max = 255) String nom) {}

    public record PartageRequest(@NotNull UUID utilisateurId, @NotNull Droit droit,
                                 Instant expireLe) {}

    public record PartageResponse(UUID id, UUID utilisateurId, Droit droit,
                                  Instant expireLe, Instant creeLe) {
        public static PartageResponse from(Partage p) {
            return new PartageResponse(p.getId(), p.getUtilisateurId(), p.getDroit(),
                    p.getExpireLe(), p.getCreeLe());
        }
    }
}
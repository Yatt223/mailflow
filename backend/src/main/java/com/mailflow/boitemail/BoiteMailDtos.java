package com.mailflow.boitemail;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class BoiteMailDtos {
    private BoiteMailDtos() {}

    public record CreationBoiteRequest(
            @NotBlank @Size(max = 150) String nom,
            @NotBlank @Email @Size(max = 255) String adresse,
            @NotBlank @Size(max = 255) String imapHote,
            @Min(1) @Max(65535) int imapPort,
            @NotNull SecuriteConnexion imapSecurite,
            @NotBlank @Size(max = 255) String identifiant,
            @NotBlank @Size(max = 500) String motDePasse) {}

    public record ModificationBoiteRequest(Boolean actif,
                                           @Size(min = 1, max = 500) String motDePasse) {}

    // Jamais de mot de passe dans une réponse
    public record BoiteMailResponse(UUID id, String nom, String adresse, String imapHote,
                                    int imapPort, SecuriteConnexion imapSecurite,
                                    String identifiant, boolean actif, Instant derniereSynchro,
                                    String derniereErreur, Instant creeLe) {
        public static BoiteMailResponse from(BoiteMail b) {
            return new BoiteMailResponse(b.getId(), b.getNom(), b.getAdresse(), b.getImapHote(),
                    b.getImapPort(), b.getImapSecurite(), b.getIdentifiant(), b.isActif(),
                    b.getDerniereSynchro(), b.getDerniereErreur(), b.getCreeLe());
        }
    }

    public record SynchroResponse(int importes, int erreurs) {}
}
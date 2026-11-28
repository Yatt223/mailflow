package com.mailflow.fichier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fichier")
@Getter @Setter @NoArgsConstructor
public class Fichier {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "proprietaire_id")
    private UUID proprietaireId;

    @Column(name = "mail_id")
    private UUID mailId;

    @Column(nullable = false)
    private String nom;

    @Column(name = "type_mime", nullable = false)
    private String typeMime;

    @Column(name = "taille_octets", nullable = false)
    private long tailleOctets;

    @Column(name = "cle_stockage", nullable = false)
    private String cleStockage;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();
}
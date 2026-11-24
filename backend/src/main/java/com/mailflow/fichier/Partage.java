package com.mailflow.fichier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partage")
@Getter @Setter @NoArgsConstructor
public class Partage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fichier_id", nullable = false)
    private UUID fichierId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Droit droit;

    @Column(name = "expire_le")
    private Instant expireLe;

    @Column(name = "cree_par", nullable = false)
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();
}
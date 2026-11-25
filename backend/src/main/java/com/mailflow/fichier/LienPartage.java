package com.mailflow.fichier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lien_partage")
@Getter @Setter @NoArgsConstructor
public class LienPartage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fichier_id", nullable = false)
    private UUID fichierId;

    @Column(name = "jeton_hash", nullable = false)
    private String jetonHash;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "max_telechargements")
    private Integer maxTelechargements;

    @Column(name = "nb_telechargements", nullable = false)
    private int nbTelechargements;

    @Column(name = "cree_par", nullable = false)
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();
}
package com.mailflow.organisation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organisation")
@Getter @Setter @NoArgsConstructor
public class Organisation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();
}
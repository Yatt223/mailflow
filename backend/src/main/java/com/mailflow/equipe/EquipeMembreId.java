package com.mailflow.equipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class EquipeMembreId implements Serializable {
    @Column(name = "equipe_id")
    private UUID equipeId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;
}
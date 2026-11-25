package com.mailflow.equipe;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipe_membre")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EquipeMembre {
    @EmbeddedId
    private EquipeMembreId id;
}
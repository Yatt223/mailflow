package com.mailflow.equipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class EquipeDtos {
    private EquipeDtos() {}

    public record EquipeRequest(@NotBlank @Size(max = 150) String nom) {}

    public record EquipeResponse(UUID id, String nom) {
        public static EquipeResponse from(Equipe e) {
            return new EquipeResponse(e.getId(), e.getNom());
        }
    }
}
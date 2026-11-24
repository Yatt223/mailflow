package com.mailflow.utilisateur;

import jakarta.validation.constraints.*;

public record CreationUtilisateurRequest(
        @NotBlank String prenom,
        @NotBlank String nom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 100) String motDePasse,
        @NotNull Role role) {}
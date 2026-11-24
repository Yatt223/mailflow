package com.mailflow.auth;

import jakarta.validation.constraints.*;

public record InscriptionRequest(
        @NotBlank @Size(max = 200) String organisation,
        @NotBlank String prenom,
        @NotBlank String nom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 100) String motDePasse) {}
package com.mailflow.auth;

import jakarta.validation.constraints.*;

public record ConnexionRequest(@NotBlank @Email String email, @NotBlank String motDePasse) {}
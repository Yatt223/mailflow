package com.mailflow.mail;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record CreationMailRequest(
        @NotBlank @Email String expediteur,
        @NotEmpty List<@NotBlank @Email String> destinataires,
        List<@NotBlank @Email String> copie,
        @NotBlank @Size(max = 500) String objet,
        @NotNull @Size(max = 200000) String corps,
        Instant recuLe) {}
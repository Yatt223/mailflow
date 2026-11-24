package com.mailflow.mail;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignationRequest(@NotNull UUID utilisateurId) {}
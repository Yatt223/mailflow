package com.mailflow.mail;

import jakarta.validation.constraints.NotNull;

public record ChangementStatutRequest(@NotNull StatutMail statut) {}
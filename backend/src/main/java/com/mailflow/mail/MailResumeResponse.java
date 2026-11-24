package com.mailflow.mail;

import java.time.Instant;
import java.util.UUID;

// Version allégée pour les listes (pas de corps)
public record MailResumeResponse(UUID id, String expediteur, String objet,
                                 StatutMail statut, UUID assigneA, Instant recuLe) {
    public static MailResumeResponse from(Mail m) {
        return new MailResumeResponse(m.getId(), m.getExpediteur(), m.getObjet(),
                m.getStatut(), m.getAssigneA(), m.getRecuLe());
    }
}
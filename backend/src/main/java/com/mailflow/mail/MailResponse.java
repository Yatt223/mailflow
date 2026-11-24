package com.mailflow.mail;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record MailResponse(UUID id, String expediteur, List<String> destinataires,
                           List<String> copie, String objet, String corps,
                           StatutMail statut, UUID assigneA, Instant recuLe, Instant creeLe) {

    public static MailResponse from(Mail m) {
        return new MailResponse(m.getId(), m.getExpediteur(), separer(m.getDestinataires()),
                separer(m.getCopie()), m.getObjet(), m.getCorps(), m.getStatut(),
                m.getAssigneA(), m.getRecuLe(), m.getCreeLe());
    }

    private static List<String> separer(String valeur) {
        if (valeur == null || valeur.isBlank()) return List.of();
        return Arrays.stream(valeur.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList();
    }
}
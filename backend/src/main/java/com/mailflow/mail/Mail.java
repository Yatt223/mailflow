package com.mailflow.mail;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mail")
@Getter @Setter @NoArgsConstructor
public class Mail {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "dossier_id")
    private UUID dossierId;

    @Column(name = "message_id")
    private String messageId;

    @Column(nullable = false)
    private String expediteur;

    @Column(nullable = false)
    private String destinataires;

    private String copie;

    @Column(nullable = false)
    private String objet = "";

    @Column(nullable = false)
    private String corps = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMail statut = StatutMail.NOUVEAU;

    @Column(name = "assigne_a")
    private UUID assigneA;

    @Column(name = "boite_mail_id")
    private UUID boiteMailId;

    @Column(nullable = false)
    private String direction = "ENTRANT";

    @Column(name = "imap_uid")
    private Long imapUid;

    @Column(name = "pieces_ignorees", nullable = false)
    private int piecesIgnorees;

    @Column(name = "recu_le", nullable = false)
    private Instant recuLe = Instant.now();

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();
}
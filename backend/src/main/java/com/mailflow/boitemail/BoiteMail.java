package com.mailflow.boitemail;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "boite_mail")
@Getter @Setter @NoArgsConstructor
public class BoiteMail {
    @Id // attribué par l'application : il sert de contexte au chiffrement du mot de passe
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String adresse;

    @Column(name = "imap_hote", nullable = false)
    private String imapHote;

    @Column(name = "imap_port", nullable = false)
    private int imapPort;

    @Enumerated(EnumType.STRING)
    @Column(name = "imap_securite", nullable = false)
    private SecuriteConnexion imapSecurite;

    @Column(nullable = false)
    private String identifiant;

    @Column(name = "secret_chiffre", nullable = false)
    private String secretChiffre;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "derniere_uid")
    private Long derniereUid;

    @Column(name = "uid_validity")
    private Long uidValidity;

    @Column(name = "derniere_synchro")
    private Instant derniereSynchro;

    @Column(name = "derniere_erreur", length = 500)
    private String derniereErreur;

    @Column(name = "cree_par", nullable = false)
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();
}
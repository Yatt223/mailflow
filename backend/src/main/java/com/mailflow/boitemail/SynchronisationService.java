package com.mailflow.boitemail;

import com.mailflow.boitemail.IngestionMailService.DonneesMail;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronisationService {
    private static final int LOT = 50;                 // mails importés par passage
    private static final int IMPORT_INITIAL = 200;     // première synchronisation : les 200 derniers mails
    private static final long TAILLE_MAX_MESSAGE = 35L * 1024 * 1024;

    private final BoiteMailRepository boites;
    private final ConnexionsImap connexions;
    private final ChiffrementSecrets chiffrement;
    private final ExtracteurMime extracteur;
    private final IngestionMailService ingestion;
    private final Set<UUID> enCours = ConcurrentHashMap.newKeySet();

    public record Resultat(int importes, int erreurs) {}

    private record Candidat(long uid, Message message) {}

    @Scheduled(fixedDelayString = "${mailflow.mail.intervalle-synchro-ms:60000}",
            initialDelayString = "${mailflow.mail.delai-initial-ms:30000}")
    public void synchroniserToutes() {
        for (BoiteMail b : boites.findByActifTrue()) {
            try {
                synchroniser(b.getId());
            } catch (Exception e) {
                log.warn("Synchronisation ignorée pour la boîte {} : {}", b.getId(), e.getMessage());
            }
        }
    }

    public Resultat synchroniser(UUID boiteId) {
        if (!enCours.add(boiteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Synchronisation déjà en cours");
        }
        try {
            return executer(boiteId);
        } finally {
            enCours.remove(boiteId);
        }
    }

    private Resultat executer(UUID boiteId) {
        BoiteMail b = boites.findById(boiteId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boîte introuvable"));
        if (!b.isActif()) return new Resultat(0, 0);

        int importes = 0;
        int erreurs = 0;
        String erreur = null;
        try (Store store = connexions.ouvrir(b.getImapHote(), b.getImapPort(), b.getImapSecurite(),
                b.getIdentifiant(), chiffrement.dechiffrer(b.getSecretChiffre(), b.getId()))) {
            Folder dossier = store.getFolder("INBOX");
            dossier.open(Folder.READ_ONLY);
            try {
                UIDFolder uf = (UIDFolder) dossier;
                long validity = uf.getUIDValidity();
                Long curseur = b.getDerniereUid();
                if (curseur != null && b.getUidValidity() != null && b.getUidValidity() != validity) {
                    curseur = 0L; // UIDVALIDITY a changé : on relit tout, les doublons sont écartés
                }
                for (Candidat c : selectionner(dossier, uf, curseur)) {
                    try {
                        if (c.message().getSize() > TAILLE_MAX_MESSAGE) {
                            throw new IOException("message trop volumineux");
                        }
                        DonneesMail d = lire(b, c.uid(), validity, (MimeMessage) c.message());
                        if (ingestion.enregistrer(b, c.uid(), validity, d)) importes++;
                    } catch (Exception e) {
                        erreurs++;
                        erreur = "UID " + c.uid() + " ignoré (" + e.getClass().getSimpleName() + ")";
                        log.error("Import impossible : boîte {} UID {}", b.getId(), c.uid(), e);
                        ingestion.sauter(b.getId(), c.uid(), validity);
                    }
                }
            } finally {
                dossier.close(false);
            }
        } catch (AuthenticationFailedException e) {
            erreur = "Authentification refusée par le serveur";
        } catch (MessagingException e) {
            erreur = "Connexion IMAP impossible";
            log.warn("Synchronisation impossible pour la boîte {} : {}", boiteId, e.getMessage());
        } catch (ResponseStatusException e) {
            erreur = "Connexion refusée : " + e.getReason();
        } catch (RuntimeException e) {
            erreur = "Erreur interne de synchronisation";
            log.error("Synchronisation en échec pour la boîte {}", boiteId, e);
        }
        boites.noterResultat(boiteId, Instant.now(), tronquer(erreur, 500));
        return new Resultat(importes, erreurs);
    }

    private List<Candidat> selectionner(Folder dossier, UIDFolder uf, Long curseur)
            throws MessagingException {
        Message[] messages;
        if (curseur == null) {
            int n = dossier.getMessageCount();
            if (n == 0) return List.of();
            messages = dossier.getMessages(Math.max(1, n - IMPORT_INITIAL + 1), n);
        } else {
            messages = uf.getMessagesByUID(curseur + 1, UIDFolder.LASTUID);
        }
        FetchProfile fp = new FetchProfile();
        fp.add(UIDFolder.FetchProfileItem.UID);
        fp.add(FetchProfile.Item.ENVELOPE);
        dossier.fetch(messages, fp);

        long plancher = curseur == null ? 0 : curseur;
        List<Candidat> candidats = new ArrayList<>();
        for (Message m : messages) {
            if (m == null) continue;
            long uid = uf.getUID(m);
            // IMAP renvoie le dernier mail même quand la plage est vide : on filtre
            if (uid > plancher) candidats.add(new Candidat(uid, m));
        }
        candidats.sort(Comparator.comparingLong(Candidat::uid));
        return candidats.size() > LOT ? candidats.subList(0, LOT) : candidats;
    }

    private DonneesMail lire(BoiteMail b, long uid, long validity, MimeMessage m)
            throws MessagingException, IOException {
        String messageId = tronquer(nettoyer(m.getMessageID()), 500);
        if (messageId == null || messageId.isBlank()) {
            messageId = "<imap-" + b.getId() + "-" + validity + "-" + uid + "@mailflow.local>";
        }
        String expediteur = tronquer(premiere(m.getFrom()), 255);
        String objet = tronquer(nettoyer(m.getSubject()), 500);
        Date recu = m.getReceivedDate() != null ? m.getReceivedDate() : m.getSentDate();
        Instant quand = recu != null && !recu.after(new Date()) ? recu.toInstant() : Instant.now();
        return new DonneesMail(messageId,
                expediteur == null || expediteur.isBlank() ? "(inconnu)" : expediteur,
                liste(m.getRecipients(Message.RecipientType.TO)),
                liste(m.getRecipients(Message.RecipientType.CC)),
                objet == null ? "" : objet,
                quand,
                extracteur.extraire(m));
    }

    private static String premiere(Address[] adresses) {
        return adresses == null || adresses.length == 0 ? null : texte(adresses[0]);
    }

    private static String liste(Address[] adresses) {
        if (adresses == null) return "";
        return Arrays.stream(adresses).map(SynchronisationService::texte)
                .filter(Objects::nonNull).filter(s -> !s.isBlank())
                .collect(Collectors.joining(","));
    }

    private static String texte(Address a) {
        String s = a instanceof InternetAddress ia ? ia.getAddress() : a.toString();
        return nettoyer(s);
    }

    private static String nettoyer(String s) {
        return s == null ? null : s.replaceAll("\\p{Cntrl}", " ").trim();
    }

    private static String tronquer(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
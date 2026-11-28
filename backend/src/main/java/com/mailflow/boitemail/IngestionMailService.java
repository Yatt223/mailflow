package com.mailflow.boitemail;

import com.mailflow.fichier.FichierService;
import com.mailflow.mail.Mail;
import com.mailflow.mail.MailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionMailService {
    private final MailRepository mails;
    private final BoiteMailRepository boites;
    private final FichierService fichiers;

    public record DonneesMail(String messageId, String expediteur, String destinataires,
                              String copie, String objet, Instant recuLe,
                              ExtracteurMime.Extrait contenu) {}

    /** Enregistre un mail et fait avancer le curseur dans la même transaction. */
    @Transactional
    public boolean enregistrer(BoiteMail b, long uid, long validity, DonneesMail d) {
        boolean nouveau = !mails.existsByBoiteMailIdAndMessageId(b.getId(), d.messageId());
        if (nouveau) {
            Mail m = new Mail();
            m.setOrganisationId(b.getOrganisationId());
            m.setBoiteMailId(b.getId());
            m.setDirection("ENTRANT");
            m.setImapUid(uid);
            m.setMessageId(d.messageId());
            m.setExpediteur(d.expediteur());
            m.setDestinataires(d.destinataires());
            if (!d.copie().isBlank()) m.setCopie(d.copie());
            m.setObjet(d.objet());
            m.setCorps(d.contenu().corps());
            m.setRecuLe(d.recuLe());
            m = mails.saveAndFlush(m);

            int ignorees = d.contenu().ignorees();
            for (ExtracteurMime.Piece p : d.contenu().pieces()) {
                if (fichiers.enregistrerPieceJointeEntrante(b.getOrganisationId(), m.getId(),
                        p.nom(), p.contenu()).isEmpty()) {
                    ignorees++;
                }
            }
            if (ignorees > 0) m.setPiecesIgnorees(ignorees);
        }
        boites.avancer(b.getId(), uid, validity);
        return nouveau;
    }

    /** Passe un mail impossible à importer, pour qu'il ne bloque pas les suivants. */
    @Transactional
    public void sauter(UUID boiteId, long uid, long validity) {
        boites.avancer(boiteId, uid, validity);
    }
}
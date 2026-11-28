package com.mailflow.fichier;

import com.mailflow.auth.Acteur;
import com.mailflow.fichier.TypesAutorises.Type;
import com.mailflow.mail.MailService;
import com.mailflow.utilisateur.Role;
import com.mailflow.utilisateur.Utilisateur;
import com.mailflow.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.mailflow.equipe.Equipe;
import com.mailflow.equipe.EquipeRepository;
import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FichierService {
    private final FichierRepository fichiers;
    private final PartageRepository partages;
    private final UtilisateurRepository utilisateurs;
    private final StockageFichiers stockage;
    private final MailService mailService;
    private final EquipeRepository equipes;

    public record Telechargement(Fichier fichier, InputStream contenu) {}

    // ---------- Droits ----------

    private Droit droitSur(Acteur a, Fichier f) {
        if (a.id().equals(f.getProprietaireId())) return Droit.PROPRIETAIRE;
        Droit droit = Droit.AUCUN;
        for (Partage p : partages.actifsPourUtilisateur(f.getId(), a.id(), Instant.now())) {
            if (p.getDroit().ordinal() > droit.ordinal()) droit = p.getDroit();
        }
        // un auditeur reste en lecture seule, même via une équipe
        if (a.role() == Role.AUDITEUR && droit == Droit.MODIFICATION) droit = Droit.LECTURE;
        if (droit == Droit.AUCUN && f.getMailId() != null
                && mailService.peutVoir(a, f.getMailId())) {
            droit = Droit.LECTURE;
        }
        return droit;
    }

    /** Pièce jointe d'un mail reçu : sans propriétaire, lisible par ceux qui voient le mail. */
    public Optional<Fichier> enregistrerPieceJointeEntrante(UUID organisationId, UUID mailId,
                                                            String nomBrut, byte[] contenu) {
        if (contenu.length == 0) return Optional.empty();
        String nom;
        Type type;
        try {
            nom = TypesAutorises.nettoyerNom(nomBrut);
            type = TypesAutorises.verifier(TypesAutorises.extension(nom),
                    Arrays.copyOf(contenu, Math.min(contenu.length, 8)));
        } catch (ResponseStatusException e) {
            return Optional.empty(); // type ou contenu non autorisé
        }
        String cle = organisationId + "/" + UUID.randomUUID();
        try (InputStream in = new ByteArrayInputStream(contenu)) {
            stockage.ecrire(cle, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            Fichier f = new Fichier();
            f.setOrganisationId(organisationId);
            f.setProprietaireId(null);
            f.setMailId(mailId);
            f.setNom(nom);
            f.setTypeMime(type.mime());
            f.setTailleOctets(contenu.length);
            f.setCleStockage(cle);
            return Optional.of(fichiers.saveAndFlush(f));
        } catch (RuntimeException e) {
            supprimerSilencieusement(cle);
            throw e;
        }
    }

    // 404 si aucun accès (on ne révèle pas l'existence), 403 si droit insuffisant
    private Fichier charger(Acteur a, UUID id, Droit requis) {
        Fichier f = fichiers.findByIdAndOrganisationId(id, a.organisationId())
                .orElseThrow(FichierService::introuvable);
        Droit droit = droitSur(a, f);
        if (droit == Droit.AUCUN) throw introuvable();
        if (!droit.permet(requis)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droit insuffisant");
        }
        return f;
    }

    // ---------- Fichiers ----------

    public Fichier televerser(Acteur a, MultipartFile fichier, UUID mailId) {
        if (a.role() == Role.AUDITEUR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lecture seule");
        }
        if (mailId != null && !mailService.peutVoir(a, mailId)) throw introuvable();
        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        String nom = TypesAutorises.nettoyerNom(fichier.getOriginalFilename());
        Type type = TypesAutorises.verifier(TypesAutorises.extension(nom), entete(fichier));

        String cle = a.organisationId() + "/" + UUID.randomUUID();
        ecrire(cle, fichier);
        try {
            Fichier f = new Fichier();
            f.setOrganisationId(a.organisationId());
            f.setProprietaireId(a.id());
            f.setMailId(mailId);
            f.setNom(nom);
            f.setTypeMime(type.mime());
            f.setTailleOctets(fichier.getSize());
            f.setCleStockage(cle);
            return fichiers.saveAndFlush(f);
        } catch (RuntimeException e) {
            supprimerSilencieusement(cle);
            throw e;
        }
    }

    public Telechargement telecharger(Acteur a, UUID id) {
        Fichier f = charger(a, id, Droit.LECTURE);
        try {
            return new Telechargement(f, stockage.lire(f.getCleStockage()));
        } catch (IOException e) {
            log.error("Contenu introuvable pour le fichier {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Contenu indisponible");
        }
    }

    public Fichier lireMeta(Acteur a, UUID id) {
        return charger(a, id, Droit.LECTURE);
    }

    public Fichier renommer(Acteur a, UUID id, String nouveauNom) {
        Fichier f = charger(a, id, Droit.MODIFICATION);
        String nom = TypesAutorises.nettoyerNom(nouveauNom);
        if (!TypesAutorises.extension(nom).equals(TypesAutorises.extension(f.getNom()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'extension ne peut pas changer");
        }
        f.setNom(nom);
        return fichiers.save(f);
    }

    public Fichier remplacer(Acteur a, UUID id, MultipartFile contenu) {
        Fichier f = charger(a, id, Droit.MODIFICATION);
        if (contenu.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        // le nouveau contenu doit correspondre à l'extension actuelle du fichier
        Type type = TypesAutorises.verifier(TypesAutorises.extension(f.getNom()), entete(contenu));
        String ancienneCle = f.getCleStockage();
        String nouvelleCle = f.getOrganisationId() + "/" + UUID.randomUUID();
        ecrire(nouvelleCle, contenu);
        try {
            f.setCleStockage(nouvelleCle);
            f.setTypeMime(type.mime());
            f.setTailleOctets(contenu.getSize());
            f = fichiers.saveAndFlush(f);
        } catch (RuntimeException e) {
            supprimerSilencieusement(nouvelleCle);
            throw e;
        }
        supprimerSilencieusement(ancienneCle);
        return f;
    }

    public void supprimer(Acteur a, UUID id) {
        Fichier f = charger(a, id, Droit.PROPRIETAIRE);
        fichiers.delete(f); // les partages disparaissent via ON DELETE CASCADE
        supprimerSilencieusement(f.getCleStockage());
    }

    @Transactional(readOnly = true)
    public Page<Fichier> mesFichiers(Acteur a, Pageable pageable) {
        return fichiers.mesFichiers(a.organisationId(), a.id(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Fichier> partagesAvecMoi(Acteur a, Pageable pageable) {
        return fichiers.partagesAvec(a.organisationId(), a.id(), Instant.now(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Fichier> piecesJointes(Acteur a, UUID mailId) {
        if (!mailService.peutVoir(a, mailId)) throw introuvable();
        return fichiers.findByMailIdAndOrganisationIdOrderByCreeLeAsc(mailId, a.organisationId());
    }

    // ---------- Partage ----------

    @Transactional
    public Partage partager(Acteur a, UUID fichierId, UUID utilisateurId, UUID equipeId,
                            Droit droit, Instant expireLe) {
        Fichier f = charger(a, fichierId, Droit.PROPRIETAIRE);
        if ((utilisateurId == null) == (equipeId == null)) {
            throw invalide("Indiquez un utilisateur OU une équipe");
        }
        if (droit != Droit.LECTURE && droit != Droit.MODIFICATION) {
            throw invalide("Droit de partage : LECTURE ou MODIFICATION");
        }
        if (expireLe != null && !expireLe.isAfter(Instant.now())) {
            throw invalide("La date d'expiration est déjà passée");
        }

        Partage p;
        if (utilisateurId != null) {
            Utilisateur cible = utilisateurs.findById(utilisateurId)
                    .filter(u -> u.getOrganisationId().equals(a.organisationId()))
                    .filter(Utilisateur::isActif)
                    .orElseThrow(() -> invalide("Utilisateur invalide"));
            if (cible.getId().equals(a.id())) {
                throw invalide("Vous êtes déjà propriétaire de ce fichier");
            }
            if (cible.getRole() == Role.AUDITEUR && droit == Droit.MODIFICATION) {
                throw invalide("Un auditeur est en lecture seule");
            }
            p = partages.findByFichierIdAndUtilisateurId(f.getId(), cible.getId())
                    .orElseGet(Partage::new);
            if (p.getId() == null) {
                p.setFichierId(f.getId());
                p.setUtilisateurId(cible.getId());
                p.setCreePar(a.id());
            }
        } else {
            Equipe equipe = equipes.findByIdAndOrganisationId(equipeId, a.organisationId())
                    .orElseThrow(() -> invalide("Équipe invalide"));
            p = partages.findByFichierIdAndEquipeId(f.getId(), equipe.getId())
                    .orElseGet(Partage::new);
            if (p.getId() == null) {
                p.setFichierId(f.getId());
                p.setEquipeId(equipe.getId());
                p.setCreePar(a.id());
            }
        }
        p.setDroit(droit);
        p.setExpireLe(expireLe);
        try {
            return partages.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Partage déjà existant");
        }
    }
    public Fichier chargerProprietaire(Acteur a, UUID id) {
        return charger(a, id, Droit.PROPRIETAIRE);
    }

    @Transactional(readOnly = true)
    public List<Partage> lesPartages(Acteur a, UUID fichierId) {
        Fichier f = charger(a, fichierId, Droit.PROPRIETAIRE);
        return partages.findByFichierId(f.getId());
    }

    @Transactional
    public void retirerPartage(Acteur a, UUID fichierId, UUID partageId) {
        Fichier f = charger(a, fichierId, Droit.PROPRIETAIRE);
        Partage p = partages.findById(partageId)
                .filter(x -> x.getFichierId().equals(f.getId()))
                .orElseThrow(FichierService::introuvable);
        partages.delete(p);
    }

    // ---------- Utilitaires ----------

    private static byte[] entete(MultipartFile f) {
        try (InputStream in = f.getInputStream()) {
            return in.readNBytes(8);
        } catch (IOException e) {
            throw invalide("Fichier illisible");
        }
    }

    private void ecrire(String cle, MultipartFile f) {
        try (InputStream in = f.getInputStream()) {
            stockage.ecrire(cle, in);
        } catch (IOException e) {
            log.error("Écriture impossible pour la clé {}", cle, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stockage indisponible");
        }
    }

    private void supprimerSilencieusement(String cle) {
        try {
            stockage.supprimer(cle);
        } catch (IOException e) {
            log.warn("Suppression du contenu impossible pour la clé {}", cle, e);
        }
    }

    private static ResponseStatusException introuvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
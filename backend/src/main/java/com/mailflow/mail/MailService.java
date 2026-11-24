package com.mailflow.mail;

import com.mailflow.auth.Acteur;
import com.mailflow.utilisateur.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailService {
    private final MailRepository mails;
    private final UtilisateurRepository utilisateurs;

    @Transactional
    public Mail creer(Acteur a, CreationMailRequest r) {
        Mail m = new Mail();
        m.setOrganisationId(a.organisationId());
        m.setExpediteur(r.expediteur().trim());
        m.setDestinataires(String.join(",", r.destinataires()));
        if (r.copie() != null && !r.copie().isEmpty()) {
            m.setCopie(String.join(",", r.copie()));
        }
        m.setObjet(r.objet().trim());
        m.setCorps(r.corps());
        if (r.recuLe() != null) m.setRecuLe(r.recuLe());
        return mails.save(m);
    }

    @Transactional(readOnly = true)
    public Page<Mail> rechercher(Acteur a, StatutMail statut, UUID assigneA,
                                 String q, Pageable pageable) {
        String motif = (q == null || q.isBlank()) ? null
                : "%" + echapper(q.trim().toLowerCase()) + "%";

        Specification<Mail> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("organisationId"), a.organisationId()));
            if (a.role() == Role.AGENT) {
                p.add(cb.or(cb.equal(root.get("assigneA"), a.id()),
                        cb.isNull(root.get("assigneA"))));
            }
            if (statut != null) p.add(cb.equal(root.get("statut"), statut));
            if (assigneA != null) p.add(cb.equal(root.get("assigneA"), assigneA));
            if (motif != null) {
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("objet")), motif, '\\'),
                        cb.like(cb.lower(root.<String>get("expediteur")), motif, '\\'),
                        cb.like(cb.lower(root.<String>get("corps")), motif, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return mails.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Mail lire(Acteur a, UUID id) {
        return trouver(a, id);
    }

    @Transactional
    public Mail changerStatut(Acteur a, UUID id, StatutMail cible) {
        Mail m = trouver(a, id);
        if (a.role() == Role.AGENT && !a.id().equals(m.getAssigneA())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Prenez d'abord ce mail en charge");
        }
        StatutMail actuel = m.getStatut();
        boolean autorise = switch (actuel) {
            case NOUVEAU -> cible == StatutMail.EN_COURS;
            case EN_COURS -> cible == StatutMail.TRAITE;
            case TRAITE -> cible == StatutMail.EN_COURS && a.estGestionnaire();
        };
        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transition interdite : " + actuel + " vers " + cible);
        }
        m.setStatut(cible);
        return m;
    }

    @Transactional
    public Mail assigner(Acteur a, UUID id, UUID utilisateurId) {
        Mail m = trouver(a, id);
        Utilisateur cible = utilisateurs.findById(utilisateurId)
                .filter(u -> u.getOrganisationId().equals(a.organisationId()))
                .filter(Utilisateur::isActif)
                .filter(u -> u.getRole() != Role.AUDITEUR)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Utilisateur invalide"));
        m.setAssigneA(cible.getId());
        if (m.getStatut() == StatutMail.NOUVEAU) m.setStatut(StatutMail.EN_COURS);
        return m;
    }

    @Transactional
    public Mail prendre(Acteur a, UUID id) {
        trouver(a, id); // 404 si le mail n'est pas visible
        if (mails.prendreSiLibre(id, a.organisationId(), a.id()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mail déjà pris en charge");
        }
        Mail m = mails.findById(id).orElseThrow();
        if (m.getStatut() == StatutMail.NOUVEAU) m.setStatut(StatutMail.EN_COURS);
        return m;
    }

    @Transactional
    public void supprimer(Acteur a, UUID id) {
        mails.delete(trouver(a, id));
    }

    private Mail trouver(Acteur a, UUID id) {
        Mail m = mails.findByIdAndOrganisationId(id, a.organisationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Mail introuvable"));
        if (a.role() == Role.AGENT && m.getAssigneA() != null
                && !m.getAssigneA().equals(a.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mail introuvable");
        }
        return m;
    }

    private static String echapper(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
package com.mailflow.equipe;

import com.mailflow.auth.Acteur;
import com.mailflow.utilisateur.Utilisateur;
import com.mailflow.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquipeService {
    private final EquipeRepository equipes;
    private final EquipeMembreRepository appartenances;
    private final UtilisateurRepository utilisateurs;

    @Transactional
    public Equipe creer(Acteur a, String nom) {
        String n = nom.trim();
        if (equipes.findByOrganisationIdAndNomIgnoreCase(a.organisationId(), n).isPresent()) {
            throw doublon();
        }
        Equipe e = new Equipe();
        e.setOrganisationId(a.organisationId());
        e.setNom(n);
        return enregistrer(e);
    }

    @Transactional(readOnly = true)
    public List<Equipe> lister(Acteur a) {
        return a.estGestionnaire()
                ? equipes.findByOrganisationIdOrderByNomAsc(a.organisationId())
                : equipes.dont(a.organisationId(), a.id());
    }

    @Transactional
    public Equipe renommer(Acteur a, UUID id, String nom) {
        Equipe e = trouver(a, id);
        String n = nom.trim();
        if (equipes.findByOrganisationIdAndNomIgnoreCase(a.organisationId(), n)
                .filter(x -> !x.getId().equals(id)).isPresent()) {
            throw doublon();
        }
        e.setNom(n);
        return enregistrer(e);
    }

    @Transactional
    public void supprimer(Acteur a, UUID id) {
        // les membres et les partages de l'équipe disparaissent via ON DELETE CASCADE
        equipes.delete(trouver(a, id));
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> membres(Acteur a, UUID equipeId) {
        Equipe e = trouver(a, equipeId);
        if (!a.estGestionnaire() && !appartenances.estMembre(e.getId(), a.id())) {
            throw introuvable();
        }
        return appartenances.utilisateursDe(e.getId());
    }

    @Transactional
    public void ajouterMembre(Acteur a, UUID equipeId, UUID utilisateurId) {
        Equipe e = trouver(a, equipeId);
        Utilisateur u = utilisateurs.findById(utilisateurId)
                .filter(x -> x.getOrganisationId().equals(a.organisationId()))
                .filter(Utilisateur::isActif)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Utilisateur invalide"));
        if (!appartenances.estMembre(e.getId(), u.getId())) {
            appartenances.save(new EquipeMembre(new EquipeMembreId(e.getId(), u.getId())));
        }
    }

    @Transactional
    public void retirerMembre(Acteur a, UUID equipeId, UUID utilisateurId) {
        Equipe e = trouver(a, equipeId);
        appartenances.retirer(e.getId(), utilisateurId);
    }

    private Equipe trouver(Acteur a, UUID id) {
        return equipes.findByIdAndOrganisationId(id, a.organisationId())
                .orElseThrow(EquipeService::introuvable);
    }

    private Equipe enregistrer(Equipe e) {
        try {
            return equipes.saveAndFlush(e);
        } catch (DataIntegrityViolationException ex) {
            throw doublon();
        }
    }

    private static ResponseStatusException introuvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipe introuvable");
    }

    private static ResponseStatusException doublon() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Une équipe porte déjà ce nom");
    }
}
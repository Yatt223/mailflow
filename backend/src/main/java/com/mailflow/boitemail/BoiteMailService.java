package com.mailflow.boitemail;

import com.mailflow.auth.Acteur;
import com.mailflow.boitemail.BoiteMailDtos.CreationBoiteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoiteMailService {
    private final BoiteMailRepository boites;
    private final ChiffrementSecrets chiffrement;
    private final ConnexionsImap connexions;
    private final ValidateurHote validateur;
    private final SynchronisationService synchronisation;

    public BoiteMail creer(Acteur a, CreationBoiteRequest r) {
        String adresse = r.adresse().trim().toLowerCase();
        if (boites.existsByOrganisationIdAndAdresseIgnoreCase(a.organisationId(), adresse)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette boîte existe déjà");
        }
        validateur.verifierImap(r.imapHote(), r.imapPort(), r.imapSecurite());
        // Rien n'est enregistré si la connexion échoue
        connexions.tester(r.imapHote(), r.imapPort(), r.imapSecurite(),
                r.identifiant(), r.motDePasse());

        BoiteMail b = new BoiteMail();
        b.setId(UUID.randomUUID());
        b.setOrganisationId(a.organisationId());
        b.setNom(r.nom().trim());
        b.setAdresse(adresse);
        b.setImapHote(r.imapHote().trim());
        b.setImapPort(r.imapPort());
        b.setImapSecurite(r.imapSecurite());
        b.setIdentifiant(r.identifiant().trim());
        b.setSecretChiffre(chiffrement.chiffrer(r.motDePasse(), b.getId()));
        b.setCreePar(a.id());
        return boites.save(b);
    }

    public List<BoiteMail> lister(Acteur a) {
        return boites.findByOrganisationIdOrderByNomAsc(a.organisationId());
    }

    public BoiteMail lire(Acteur a, UUID id) {
        return trouver(a, id);
    }

    public BoiteMail modifier(Acteur a, UUID id, Boolean actif, String motDePasse) {
        BoiteMail b = trouver(a, id);
        if (motDePasse != null) {
            connexions.tester(b.getImapHote(), b.getImapPort(), b.getImapSecurite(),
                    b.getIdentifiant(), motDePasse);
            b.setSecretChiffre(chiffrement.chiffrer(motDePasse, b.getId()));
            b.setDerniereErreur(null);
        }
        if (actif != null) b.setActif(actif);
        return boites.save(b);
    }

    public void supprimer(Acteur a, UUID id) {
        boites.delete(trouver(a, id)); // les mails importés sont conservés (boite_mail_id passe à NULL)
    }

    public SynchronisationService.Resultat synchroniser(Acteur a, UUID id) {
        return synchronisation.synchroniser(trouver(a, id).getId());
    }

    public SynchronisationService.Resultat resynchroniser(Acteur a, UUID id) {
        BoiteMail b = trouver(a, id);
        boites.reinitialiserCurseur(b.getId()); // relit la boîte depuis le début, sans doublons
        return synchronisation.synchroniser(b.getId());
    }

    private BoiteMail trouver(Acteur a, UUID id) {
        return boites.findByIdAndOrganisationId(id, a.organisationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Boîte introuvable"));
    }
}
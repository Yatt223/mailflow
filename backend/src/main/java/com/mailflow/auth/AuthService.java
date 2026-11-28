package com.mailflow.auth;

import com.mailflow.audit.AuditService;
import com.mailflow.organisation.Organisation;
import com.mailflow.organisation.OrganisationRepository;
import com.mailflow.utilisateur.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    // Comparé quand l'email est inconnu, pour que la réponse prenne le même temps
    private static final String HASH_FACTICE = new BCryptPasswordEncoder().encode("hash-factice");

    private final OrganisationRepository organisations;
    private final UtilisateurRepository utilisateurs;
    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;

    @Transactional
    public TokenResponse inscrire(InscriptionRequest req, String ip) {
        Organisation org = new Organisation();
        org.setNom(req.organisation().trim());
        org = organisations.save(org);
        Utilisateur admin = utilisateurService.creer(org.getId(), req.email(),
                req.motDePasse(), req.prenom(), req.nom(), Role.ADMIN);
        audit.enregistrer(org.getId(), admin.getId(), "ORGANISATION_CREEE", "ORGANISATION",
                org.getId(), Map.of("ip", String.valueOf(ip)));
        return jwtService.generer(admin);
    }

    public TokenResponse connecter(ConnexionRequest req, String ip) {
        Optional<Utilisateur> candidat = utilisateurs.findByEmailIgnoreCase(req.email().trim());
        String hash = candidat.map(Utilisateur::getMotDePasseHash).orElse(HASH_FACTICE);
        boolean motDePasseValide = passwordEncoder.matches(req.motDePasse(), hash);
        boolean valide = motDePasseValide && candidat.filter(Utilisateur::isActif).isPresent();

        if (!valide) {
            // Email inconnu : aucune organisation à laquelle rattacher l'événement, donc pas de ligne d'audit
            candidat.ifPresent(u -> audit.enregistrer(u.getOrganisationId(), u.getId(),
                    "CONNEXION_ECHEC", "UTILISATEUR", u.getId(), Map.of("ip", String.valueOf(ip))));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        Utilisateur u = candidat.get();
        audit.enregistrer(u.getOrganisationId(), u.getId(), "CONNEXION", "UTILISATEUR",
                u.getId(), Map.of("ip", String.valueOf(ip)));
        return jwtService.generer(u);
    }
}
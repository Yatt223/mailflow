package com.mailflow.auth;

import com.mailflow.organisation.Organisation;
import com.mailflow.organisation.OrganisationRepository;
import com.mailflow.utilisateur.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final OrganisationRepository organisations;
    private final UtilisateurRepository utilisateurs;
    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse inscrire(InscriptionRequest req) {
        Organisation org = new Organisation();
        org.setNom(req.organisation().trim());
        org = organisations.save(org);
        Utilisateur admin = utilisateurService.creer(org.getId(), req.email(),
                req.motDePasse(), req.prenom(), req.nom(), Role.ADMIN);
        return jwtService.generer(admin);
    }

    public TokenResponse connecter(ConnexionRequest req) {
        Utilisateur u = utilisateurs.findByEmailIgnoreCase(req.email().trim())
                .filter(Utilisateur::isActif)
                .filter(x -> passwordEncoder.matches(req.motDePasse(), x.getMotDePasseHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Identifiants invalides"));
        return jwtService.generer(u);
    }
}
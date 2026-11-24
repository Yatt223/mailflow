package com.mailflow.utilisateur;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilisateurService {
    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Utilisateur creer(UUID organisationId, String email, String motDePasse,
                             String prenom, String nom, Role role) {
        String emailNormalise = email.trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(emailNormalise)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        Utilisateur u = new Utilisateur();
        u.setOrganisationId(organisationId);
        u.setEmail(emailNormalise);
        u.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        u.setPrenom(prenom.trim());
        u.setNom(nom.trim());
        u.setRole(role);
        return repository.save(u);
    }

    public List<Utilisateur> lister(UUID organisationId) {
        return repository.findByOrganisationId(organisationId);
    }
}
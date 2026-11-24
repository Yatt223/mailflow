package com.mailflow.utilisateur;

import java.util.UUID;

public record UtilisateurResponse(UUID id, String email, String prenom, String nom,
                                  Role role, boolean actif) {
    public static UtilisateurResponse from(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getEmail(), u.getPrenom(),
                u.getNom(), u.getRole(), u.isActif());
    }
}
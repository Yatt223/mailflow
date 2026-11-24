# Authentification

## Rôles

| Rôle     | Droits                                              |
| -------- | --------------------------------------------------- |
| ADMIN    | Tout dans son organisation, création d'utilisateurs |
| MANAGER  | Liste des utilisateurs, supervision                 |
| AGENT    | Traitement des mails                                |
| AUDITEUR | Lecture seule, consultation de l'audit              |

## Endpoints

| Méthode | URL                   | Accès                                     |
| ------- | --------------------- | ----------------------------------------- |
| POST    | /api/auth/inscription | Public : crée l'organisation et son ADMIN |
| POST    | /api/auth/connexion   | Public : renvoie un token JWT             |
| GET     | /api/moi              | Authentifié                               |
| GET     | /api/utilisateurs     | ADMIN, MANAGER                            |
| POST    | /api/utilisateurs     | ADMIN                                     |

Le token se passe dans l'en-tête `Authorization: Bearer <token>` (validité 60 min).

## Configuration

Variable d'environnement obligatoire : `JWT_SECRET` (32 caractères minimum).
Ne jamais la committer.

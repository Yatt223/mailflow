# API des mails

Tous les endpoints exigent un token JWT. Un mail d'une autre organisation renvoie 404.

| Méthode | URL | Rôles |
|---|---|---|
| POST | /api/mails | ADMIN, MANAGER, AGENT |
| GET | /api/mails?statut=&assigneA=&q=&page=&size= | Tous (AGENT : ses mails + non assignés) |
| GET | /api/mails/{id} | Tous (même restriction) |
| PATCH | /api/mails/{id}/statut | ADMIN, MANAGER, AGENT (ses mails) |
| PUT | /api/mails/{id}/assignation | ADMIN, MANAGER |
| POST | /api/mails/{id}/prendre | ADMIN, MANAGER, AGENT |
| DELETE | /api/mails/{id} | ADMIN |

## Statuts
NOUVEAU → EN_COURS → TRAITE. Réouverture (TRAITE → EN_COURS) : ADMIN, MANAGER.
Assigner ou prendre un mail NOUVEAU le passe en EN_COURS.

## Recherche
`q` cherche dans l'objet, l'expéditeur et le corps. Taille de page max : 100.
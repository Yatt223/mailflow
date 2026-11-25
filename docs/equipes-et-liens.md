# Équipes et liens de partage

## Équipes

| Méthode      | URL                                       | Rôles                                           |
| ------------ | ----------------------------------------- | ----------------------------------------------- |
| POST         | /api/equipes                              | ADMIN, MANAGER                                  |
| GET          | /api/equipes                              | ADMIN/MANAGER : toutes ; autres : les leurs     |
| PATCH        | /api/equipes/{id}                         | ADMIN, MANAGER                                  |
| DELETE       | /api/equipes/{id}                         | ADMIN (supprime aussi les partages de l'équipe) |
| GET          | /api/equipes/{id}/membres                 | Gestionnaires ou membres                        |
| PUT / DELETE | /api/equipes/{id}/membres/{utilisateurId} | ADMIN, MANAGER                                  |

## Partage avec une équipe

POST /api/fichiers/{id}/partages avec `equipeId` OU `utilisateurId` (jamais les deux), `droit`
(LECTURE ou MODIFICATION) et `expireLe` optionnel. Tous les membres reçoivent le droit.
Le droit le plus élevé s'applique. Un auditeur reste en lecture seule.

## Liens à durée limitée (propriétaire du fichier uniquement)

| Méthode | URL                               | Accès                                                                             |
| ------- | --------------------------------- | --------------------------------------------------------------------------------- |
| POST    | /api/fichiers/{id}/liens          | Propriétaire : `dureeHeures` (1-168, défaut 24), `maxTelechargements` (optionnel) |
| GET     | /api/fichiers/{id}/liens          | Propriétaire                                                                      |
| DELETE  | /api/fichiers/{id}/liens/{lienId} | Propriétaire (révocation)                                                         |
| GET     | /api/liens/{jeton}                | Public, sans authentification                                                     |

Le jeton n'est affiché qu'à la création. Un lien expiré, révoqué, épuisé ou inconnu renvoie 404.

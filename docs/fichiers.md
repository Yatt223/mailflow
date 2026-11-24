# Fichiers et partage

## Types autorisés

pdf, png, jpg, jpeg, gif, webp, docx, xlsx, pptx, odt, ods, txt, csv.
Le contenu est vérifié (signature) et doit correspondre à l'extension. Taille max : 25 Mo.
Les formats HTML, SVG, exécutables et les anciens formats Office avec macros (doc, xls, ppt) sont refusés.

## Droits

| Action              | Propriétaire | MODIFICATION | LECTURE | Autre |
| ------------------- | ------------ | ------------ | ------- | ----- |
| Voir, télécharger   | oui          | oui          | oui     | 404   |
| Renommer, remplacer | oui          | oui          | 403     | 404   |
| Supprimer, partager | oui          | 403          | 403     | 404   |

Les pièces jointes sont lisibles par tous ceux qui voient le mail.
Un partage peut avoir une date d'expiration. Un auditeur n'a jamais que la LECTURE.

## Endpoints

| Méthode    | URL                                        | Rôle                                      |
| ---------- | ------------------------------------------ | ----------------------------------------- |
| POST       | /api/fichiers (multipart, champ `fichier`) | ADMIN, MANAGER, AGENT                     |
| GET        | /api/fichiers                              | Mes fichiers                              |
| GET        | /api/fichiers/partages                     | Fichiers partagés avec moi                |
| GET        | /api/fichiers/{id}                         | Métadonnées                               |
| GET        | /api/fichiers/{id}/contenu                 | Téléchargement (toujours en pièce jointe) |
| PUT        | /api/fichiers/{id}/contenu                 | Remplacer le contenu                      |
| PATCH      | /api/fichiers/{id}                         | Renommer                                  |
| DELETE     | /api/fichiers/{id}                         | Propriétaire                              |
| GET / POST | /api/fichiers/{id}/partages                | Propriétaire                              |
| DELETE     | /api/fichiers/{id}/partages/{partageId}    | Propriétaire                              |
| POST / GET | /api/mails/{mailId}/fichiers               | Pièces jointes                            |

Stockage : dossier défini par `mailflow.stockage.dossier` (hors dépôt Git).

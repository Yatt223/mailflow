# Modèle de données

Toutes les tables métier portent `organisation_id` (isolation multi-tenant).

| Table                  | Rôle                                                                                                 |
| ---------------------- | ---------------------------------------------------------------------------------------------------- |
| organisation           | Entreprise cliente (tenant)                                                                          |
| utilisateur            | Compte, rôle : ADMIN, MANAGER, AGENT, AUDITEUR                                                       |
| equipe / equipe_membre | Groupes d'utilisateurs                                                                               |
| dossier                | Arborescence de classement des mails                                                                 |
| mail                   | Mail reçu ou envoyé, statut NOUVEAU / EN_COURS / TRAITE                                              |
| fichier                | Document, image ou pièce jointe (rattaché à un mail si mail_id)                                      |
| partage                | Partage d'un fichier avec un utilisateur ou une équipe (LECTURE / MODIFICATION, expiration possible) |
| lien_partage           | Lien temporaire (jeton stocké sous forme de hash)                                                    |
| journal_audit          | Traçabilité des actions                                                                              |

Migrations : `backend/src/main/resources/db/migration`. Ne jamais modifier une migration déjà appliquée.

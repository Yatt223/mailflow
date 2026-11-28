# Boîtes mail (réception IMAP)

Une boîte mail relie MailFlow à une adresse de l'entreprise. Les nouveaux mails de la boîte INBOX
sont importés toutes les 60 secondes (ou à la demande) comme mails ENTRANT, statut NOUVEAU.
Le serveur n'est jamais modifié : lecture seule, rien n'est supprimé ni marqué comme lu.

## Endpoints
| Méthode | URL | Rôles |
|---|---|---|
| POST | /api/boites-mail | ADMIN (la connexion est testée avant l'enregistrement) |
| GET | /api/boites-mail, /api/boites-mail/{id} | ADMIN, MANAGER |
| PATCH | /api/boites-mail/{id} | ADMIN : `actif`, `motDePasse` |
| DELETE | /api/boites-mail/{id} | ADMIN (les mails importés sont conservés) |
| POST | /api/boites-mail/{id}/synchroniser | ADMIN, MANAGER |
| POST | /api/boites-mail/{id}/resynchroniser | ADMIN : relit toute la boîte, sans doublons |

## Règles
- Ports IMAP 993 (SSL) ou 143 (STARTTLS). Une connexion non chiffrée est refusée.
- Les adresses internes (localhost, réseaux privés, 169.254.x.x) sont refusées.
- Première synchronisation : les 200 derniers mails. 50 mails importés par passage.
- Texte uniquement : le HTML est converti en texte. Mails de plus de 35 Mo ignorés.
- Pièces jointes : types autorisés, 25 Mo max chacune. Les autres sont comptées dans `piecesIgnorees`.
- `derniereErreur` indique le dernier problème ; `resynchroniser` récupère un mail ignoré.

## Configuration serveur
`MAILFLOW_CRYPTO_KEY` (obligatoire) : 32 octets en base64, chiffre les mots de passe des boîtes.
À sauvegarder : sa perte oblige à ressaisir tous les mots de passe.
`mailflow.mail.dev=true` n'est réservé qu'au développement local.
CREATE TABLE boite_mail (
    id               UUID PRIMARY KEY,
    organisation_id  UUID NOT NULL REFERENCES organisation(id),
    nom              VARCHAR(150) NOT NULL,
    adresse          VARCHAR(255) NOT NULL,
    imap_hote        VARCHAR(255) NOT NULL,
    imap_port        INT NOT NULL CHECK (imap_port BETWEEN 1 AND 65535),
    imap_securite    VARCHAR(10) NOT NULL CHECK (imap_securite IN ('SSL', 'STARTTLS', 'AUCUNE')),
    identifiant      VARCHAR(255) NOT NULL,
    secret_chiffre   TEXT NOT NULL,
    actif            BOOLEAN NOT NULL DEFAULT TRUE,
    derniere_uid     BIGINT,
    uid_validity     BIGINT,
    derniere_synchro TIMESTAMPTZ,
    derniere_erreur  VARCHAR(500),
    cree_par         UUID NOT NULL REFERENCES utilisateur(id),
    cree_le          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organisation_id, adresse)
);
CREATE INDEX ix_boite_mail_org ON boite_mail (organisation_id);

ALTER TABLE mail
    ADD COLUMN boite_mail_id UUID REFERENCES boite_mail(id) ON DELETE SET NULL,
    ADD COLUMN direction VARCHAR(10) NOT NULL DEFAULT 'ENTRANT'
        CHECK (direction IN ('ENTRANT', 'SORTANT')),
    ADD COLUMN imap_uid BIGINT,
    ADD COLUMN pieces_ignorees INT NOT NULL DEFAULT 0;

-- Le même Message-ID peut arriver dans deux boîtes différentes : unicité par boîte
DROP INDEX ux_mail_message_id;
CREATE UNIQUE INDEX ux_mail_boite_message ON mail (boite_mail_id, message_id)
    WHERE boite_mail_id IS NOT NULL AND message_id IS NOT NULL;

-- Les pièces jointes des mails reçus n'ont pas de propriétaire
ALTER TABLE fichier ALTER COLUMN proprietaire_id DROP NOT NULL;
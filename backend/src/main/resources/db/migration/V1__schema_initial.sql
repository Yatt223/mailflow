-- Organisations (tenants)
CREATE TABLE organisation (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom       VARCHAR(200) NOT NULL,
    cree_le   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Utilisateurs
CREATE TABLE utilisateur (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id   UUID NOT NULL REFERENCES organisation(id),
    email             VARCHAR(255) NOT NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    prenom            VARCHAR(100) NOT NULL,
    nom               VARCHAR(100) NOT NULL,
    role              VARCHAR(20) NOT NULL
        CHECK (role IN ('ADMIN', 'MANAGER', 'AGENT', 'AUDITEUR')),
    actif             BOOLEAN NOT NULL DEFAULT TRUE,
    cree_le           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_utilisateur_email ON utilisateur (lower(email));
CREATE INDEX ix_utilisateur_org ON utilisateur (organisation_id);

-- Équipes
CREATE TABLE equipe (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisation(id),
    nom             VARCHAR(150) NOT NULL,
    UNIQUE (organisation_id, nom)
);

CREATE TABLE equipe_membre (
    equipe_id      UUID NOT NULL REFERENCES equipe(id) ON DELETE CASCADE,
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    PRIMARY KEY (equipe_id, utilisateur_id)
);

-- Dossiers (arborescence de classement des mails)
CREATE TABLE dossier (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisation(id),
    parent_id       UUID REFERENCES dossier(id),
    nom             VARCHAR(150) NOT NULL
);
CREATE INDEX ix_dossier_org ON dossier (organisation_id);

-- Mails
CREATE TABLE mail (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisation(id),
    dossier_id      UUID REFERENCES dossier(id),
    message_id      VARCHAR(500),
    expediteur      VARCHAR(255) NOT NULL,
    destinataires   TEXT NOT NULL,
    copie           TEXT,
    objet           VARCHAR(500) NOT NULL DEFAULT '',
    corps           TEXT NOT NULL DEFAULT '',
    statut          VARCHAR(20) NOT NULL DEFAULT 'NOUVEAU'
        CHECK (statut IN ('NOUVEAU', 'EN_COURS', 'TRAITE')),
    assigne_a       UUID REFERENCES utilisateur(id),
    recu_le         TIMESTAMPTZ NOT NULL DEFAULT now(),
    cree_le         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_mail_org_statut ON mail (organisation_id, statut);
CREATE INDEX ix_mail_assigne ON mail (assigne_a);
CREATE UNIQUE INDEX ux_mail_message_id ON mail (organisation_id, message_id)
    WHERE message_id IS NOT NULL;

-- Fichiers (documents, images, pièces jointes)
CREATE TABLE fichier (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id  UUID NOT NULL REFERENCES organisation(id),
    proprietaire_id  UUID NOT NULL REFERENCES utilisateur(id),
    mail_id          UUID REFERENCES mail(id) ON DELETE CASCADE,
    nom              VARCHAR(255) NOT NULL,
    type_mime        VARCHAR(150) NOT NULL,
    taille_octets    BIGINT NOT NULL CHECK (taille_octets >= 0),
    cle_stockage     VARCHAR(500) NOT NULL,
    cree_le          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_fichier_org ON fichier (organisation_id);
CREATE INDEX ix_fichier_proprietaire ON fichier (proprietaire_id);
CREATE INDEX ix_fichier_mail ON fichier (mail_id);

-- Partage d'un fichier avec un utilisateur OU une équipe
CREATE TABLE partage (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fichier_id      UUID NOT NULL REFERENCES fichier(id) ON DELETE CASCADE,
    utilisateur_id  UUID REFERENCES utilisateur(id) ON DELETE CASCADE,
    equipe_id       UUID REFERENCES equipe(id) ON DELETE CASCADE,
    droit           VARCHAR(20) NOT NULL
        CHECK (droit IN ('LECTURE', 'MODIFICATION')),
    expire_le       TIMESTAMPTZ,
    cree_par        UUID NOT NULL REFERENCES utilisateur(id),
    cree_le         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((utilisateur_id IS NOT NULL) <> (equipe_id IS NOT NULL))
);
CREATE INDEX ix_partage_fichier ON partage (fichier_id);
CREATE INDEX ix_partage_utilisateur ON partage (utilisateur_id);
CREATE INDEX ix_partage_equipe ON partage (equipe_id);

-- Liens de partage à durée limitée
CREATE TABLE lien_partage (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fichier_id  UUID NOT NULL REFERENCES fichier(id) ON DELETE CASCADE,
    jeton_hash  VARCHAR(128) NOT NULL UNIQUE,
    expire_le   TIMESTAMPTZ NOT NULL,
    cree_par    UUID NOT NULL REFERENCES utilisateur(id),
    cree_le     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Journal d'audit (qui a fait quoi, quand)
CREATE TABLE journal_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisation(id),
    utilisateur_id  UUID REFERENCES utilisateur(id),
    action          VARCHAR(50) NOT NULL,
    type_objet      VARCHAR(50) NOT NULL,
    objet_id        UUID,
    details         JSONB,
    cree_le         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_org_date ON journal_audit (organisation_id, cree_le DESC);
CREATE INDEX ix_audit_objet ON journal_audit (type_objet, objet_id);
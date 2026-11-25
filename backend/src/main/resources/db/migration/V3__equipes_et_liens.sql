ALTER TABLE lien_partage
    ADD COLUMN max_telechargements INT CHECK (max_telechargements IS NULL OR max_telechargements > 0),
    ADD COLUMN nb_telechargements  INT NOT NULL DEFAULT 0;

CREATE INDEX ix_lien_partage_fichier ON lien_partage (fichier_id);
CREATE INDEX ix_equipe_membre_utilisateur ON equipe_membre (utilisateur_id);
CREATE UNIQUE INDEX ux_partage_fichier_equipe
    ON partage (fichier_id, equipe_id) WHERE equipe_id IS NOT NULL;
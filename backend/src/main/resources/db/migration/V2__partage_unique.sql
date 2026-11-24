CREATE UNIQUE INDEX ux_partage_fichier_utilisateur
    ON partage (fichier_id, utilisateur_id) WHERE utilisateur_id IS NOT NULL;
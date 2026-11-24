package com.mailflow.fichier;

public enum Droit {
    AUCUN, LECTURE, MODIFICATION, PROPRIETAIRE;

    public boolean permet(Droit requis) {
        return ordinal() >= requis.ordinal();
    }
}
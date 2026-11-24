package com.mailflow.fichier;

import java.io.IOException;
import java.io.InputStream;

public interface StockageFichiers {
    void ecrire(String cle, InputStream contenu) throws IOException;
    InputStream lire(String cle) throws IOException;
    void supprimer(String cle) throws IOException;
}
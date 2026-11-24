package com.mailflow.fichier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Component
public class StockageLocal implements StockageFichiers {
    private final Path racine;

    public StockageLocal(@Value("${mailflow.stockage.dossier}") String dossier) throws IOException {
        this.racine = Path.of(dossier).toAbsolutePath().normalize();
        Files.createDirectories(racine);
    }

    private Path chemin(String cle) {
        Path p = racine.resolve(cle).normalize();
        if (!p.startsWith(racine)) {
            throw new IllegalArgumentException("Clé de stockage invalide");
        }
        return p;
    }

    @Override
    public void ecrire(String cle, InputStream contenu) throws IOException {
        Path p = chemin(cle);
        Files.createDirectories(p.getParent());
        Files.copy(contenu, p, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream lire(String cle) throws IOException {
        return Files.newInputStream(chemin(cle));
    }

    @Override
    public void supprimer(String cle) throws IOException {
        Files.deleteIfExists(chemin(cle));
    }
}
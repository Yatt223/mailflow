package com.mailflow.fichier;

import com.mailflow.auth.Acteur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LienPartageService {
    private static final int DUREE_DEFAUT_HEURES = 24;
    private static final int DUREE_MAX_HEURES = 168;
    private static final Pattern FORMAT_JETON = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final SecureRandom ALEA = new SecureRandom();

    private final LienPartageRepository liens;
    private final FichierRepository fichiers;
    private final FichierService fichierService;
    private final StockageFichiers stockage;

    public record LienCree(LienPartage lien, String jeton) {}

    @Transactional
    public LienCree creer(Acteur a, UUID fichierId, Integer dureeHeures, Integer maxTelechargements) {
        Fichier f = fichierService.chargerProprietaire(a, fichierId);
        int heures = dureeHeures == null ? DUREE_DEFAUT_HEURES : dureeHeures;
        if (heures < 1 || heures > DUREE_MAX_HEURES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Durée : 1 à " + DUREE_MAX_HEURES + " heures");
        }
        if (maxTelechargements != null && maxTelechargements < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nombre de téléchargements doit être positif");
        }
        byte[] octets = new byte[32];
        ALEA.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

        LienPartage l = new LienPartage();
        l.setFichierId(f.getId());
        l.setJetonHash(hacher(jeton));
        l.setExpireLe(Instant.now().plus(Duration.ofHours(heures)));
        l.setMaxTelechargements(maxTelechargements);
        l.setCreePar(a.id());
        return new LienCree(liens.save(l), jeton);
    }

    @Transactional(readOnly = true)
    public List<LienPartage> lister(Acteur a, UUID fichierId) {
        Fichier f = fichierService.chargerProprietaire(a, fichierId);
        return liens.findByFichierIdOrderByCreeLeDesc(f.getId());
    }

    @Transactional
    public void revoquer(Acteur a, UUID fichierId, UUID lienId) {
        Fichier f = fichierService.chargerProprietaire(a, fichierId);
        LienPartage l = liens.findById(lienId)
                .filter(x -> x.getFichierId().equals(f.getId()))
                .orElseThrow(LienPartageService::introuvable);
        liens.delete(l);
    }

    // Accès public : aucune authentification. Tout échec renvoie le même 404.
    @Transactional
    public FichierService.Telechargement telecharger(String jeton) {
        if (jeton == null || !FORMAT_JETON.matcher(jeton).matches()) throw introuvable();
        String hash = hacher(jeton);
        if (liens.consommer(hash, Instant.now()) == 0) throw introuvable();

        LienPartage l = liens.findByJetonHash(hash).orElseThrow(LienPartageService::introuvable);
        Fichier f = fichiers.findById(l.getFichierId()).orElseThrow(LienPartageService::introuvable);
        try {
            InputStream contenu = stockage.lire(f.getCleStockage());
            log.info("Téléchargement par lien {} du fichier {}", l.getId(), f.getId());
            return new FichierService.Telechargement(f, contenu);
        } catch (IOException e) {
            log.error("Contenu introuvable pour le fichier {}", f.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Contenu indisponible");
        }
    }

    private static String hacher(String jeton) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ResponseStatusException introuvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Lien introuvable");
    }
}
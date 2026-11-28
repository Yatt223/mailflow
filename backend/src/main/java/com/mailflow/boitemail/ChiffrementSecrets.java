package com.mailflow.boitemail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
public class ChiffrementSecrets {
    private static final int TAILLE_IV = 12;
    private static final int TAILLE_TAG_BITS = 128;

    private final SecretKey cle;
    private final SecureRandom alea = new SecureRandom();

    public ChiffrementSecrets(@Value("${mailflow.crypto.cle:}") String cleBase64) {
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(cleBase64.trim());
        } catch (IllegalArgumentException e) {
            octets = new byte[0];
        }
        if (octets.length != 32) {
            throw new IllegalStateException(
                    "MAILFLOW_CRYPTO_KEY manquante ou invalide (32 octets encodés en base64)");
        }
        this.cle = new SecretKeySpec(octets, "AES");
    }

    // Le contexte (id de la boîte) est authentifié : un secret copié sur une autre ligne ne se déchiffre pas
    public String chiffrer(String clair, UUID contexte) {
        try {
            byte[] iv = new byte[TAILLE_IV];
            alea.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            c.updateAAD(contexte.toString().getBytes(StandardCharsets.UTF_8));
            byte[] chiffre = c.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + chiffre.length).put(iv).put(chiffre).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement impossible", e);
        }
    }

    public String dechiffrer(String valeur, UUID contexte) {
        try {
            byte[] tout = Base64.getDecoder().decode(valeur);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, tout, 0, TAILLE_IV));
            c.updateAAD(contexte.toString().getBytes(StandardCharsets.UTF_8));
            return new String(c.doFinal(tout, TAILLE_IV, tout.length - TAILLE_IV),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Déchiffrement impossible (clé modifiée ?)", e);
        }
    }
}
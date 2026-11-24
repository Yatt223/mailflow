package com.mailflow.fichier;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

final class TypesAutorises {
    private TypesAutorises() {}

    record Type(String mime, byte[] signature) {}

    private static final byte[] PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF = "GIF8".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ZIP = {'P', 'K', 3, 4};

    private static final Map<String, Type> PAR_EXTENSION = Map.ofEntries(
            Map.entry("pdf", new Type("application/pdf", PDF)),
            Map.entry("png", new Type("image/png", PNG)),
            Map.entry("jpg", new Type("image/jpeg", JPG)),
            Map.entry("jpeg", new Type("image/jpeg", JPG)),
            Map.entry("gif", new Type("image/gif", GIF)),
            Map.entry("webp", new Type("image/webp", RIFF)),
            Map.entry("docx", new Type("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ZIP)),
            Map.entry("xlsx", new Type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ZIP)),
            Map.entry("pptx", new Type("application/vnd.openxmlformats-officedocument.presentationml.presentation", ZIP)),
            Map.entry("odt", new Type("application/vnd.oasis.opendocument.text", ZIP)),
            Map.entry("ods", new Type("application/vnd.oasis.opendocument.spreadsheet", ZIP)),
            Map.entry("txt", new Type("text/plain", null)),
            Map.entry("csv", new Type("text/csv", null)));

    static Type verifier(String extension, byte[] entete) {
        Type t = PAR_EXTENSION.get(extension);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Type de fichier non autorisé");
        }
        if (t.signature() != null && !commencePar(entete, t.signature())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Le contenu ne correspond pas à l'extension");
        }
        return t;
    }

    static String nettoyerNom(String original) {
        if (original == null) {
            throw invalide("Nom de fichier manquant");
        }
        String nom = original.replace('\\', '/');
        nom = nom.substring(nom.lastIndexOf('/') + 1).replaceAll("\\p{Cc}", "").trim();
        if (nom.isEmpty() || nom.length() > 255) {
            throw invalide("Nom de fichier invalide");
        }
        return nom;
    }

    static String extension(String nom) {
        int i = nom.lastIndexOf('.');
        return i < 0 ? "" : nom.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean commencePar(byte[] donnees, byte[] prefixe) {
        if (donnees.length < prefixe.length) return false;
        for (int i = 0; i < prefixe.length; i++) {
            if (donnees[i] != prefixe[i]) return false;
        }
        return true;
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
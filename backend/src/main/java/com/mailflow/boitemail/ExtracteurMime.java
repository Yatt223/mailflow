package com.mailflow.boitemail;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Extrait le texte et les pièces jointes d'un mail, avec des limites strictes. */
@Component
public class ExtracteurMime {
    static final int TAILLE_MAX_PIECE = 25 * 1024 * 1024;
    static final int TAILLE_MAX_TOTAL = 30 * 1024 * 1024;
    static final int MAX_PIECES = 20;
    static final int MAX_PARTIES = 100;
    static final int MAX_PROFONDEUR = 10;
    static final int MAX_TEXTE = 200_000;

    public record Piece(String nom, byte[] contenu) {}

    public record Extrait(String corps, List<Piece> pieces, int ignorees) {}

    private static final class Etat {
        String texte;
        String html;
        int ignorees;
        int parties;
        long total;
        final List<Piece> pieces = new ArrayList<>();
    }

    public Extrait extraire(Part racine) throws MessagingException, IOException {
        Etat e = new Etat();
        parcourir(racine, e, 0);
        String corps = e.texte != null ? e.texte : (e.html != null ? htmlVersTexte(e.html) : "");
        corps = corps.replace("\u0000", ""); // PostgreSQL refuse le caractère NUL
        if (corps.length() > MAX_TEXTE) corps = corps.substring(0, MAX_TEXTE);
        return new Extrait(corps, e.pieces, e.ignorees);
    }

    private void parcourir(Part p, Etat e, int profondeur) throws MessagingException, IOException {
        if (profondeur > MAX_PROFONDEUR || ++e.parties > MAX_PARTIES) {
            e.ignorees++;
            return;
        }
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) parcourir(mp.getBodyPart(i), e, profondeur + 1);
            return;
        }
        String nom = p.getFileName();
        boolean piece = Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition()) || nom != null;
        if (!piece && p.isMimeType("text/plain")) {
            if (e.texte == null) e.texte = lireTexte(p);
            return;
        }
        if (!piece && p.isMimeType("text/html")) {
            if (e.html == null) e.html = lireTexte(p);
            return;
        }
        if (!piece) {
            if (p.isMimeType("message/rfc822")) e.ignorees++; // mail imbriqué : non importé
            return;
        }
        try (InputStream in = p.getInputStream()) {
            byte[] data = in.readNBytes(TAILLE_MAX_PIECE + 1);
            if (data.length > TAILLE_MAX_PIECE || e.total + data.length > TAILLE_MAX_TOTAL
                    || e.pieces.size() >= MAX_PIECES) {
                e.ignorees++;
                return;
            }
            e.total += data.length;
            e.pieces.add(new Piece(nom != null ? nom : "piece-jointe", data));
        }
    }

    private static String lireTexte(Part p) throws MessagingException, IOException {
        try {
            if (p.getContent() instanceof String s) return s;
        } catch (UnsupportedEncodingException ex) {
            // jeu de caractères inconnu : lecture brute ci-dessous
        }
        try (InputStream in = p.getInputStream()) {
            return new String(in.readNBytes(MAX_TEXTE * 4), StandardCharsets.ISO_8859_1);
        }
    }

    // Le HTML n'est jamais conservé : seul le texte l'est
    static String htmlVersTexte(String html) {
        String h = html.length() > 500_000 ? html.substring(0, 500_000) : html;
        String t = h.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</tr>|</li>", "\n")
                .replaceAll("(?s)<[^>]+>", "")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        return t.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }
}
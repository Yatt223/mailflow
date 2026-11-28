package com.mailflow.boitemail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.regex.Pattern;

/** Empêche qu'un tenant fasse connecter le serveur à une adresse interne (SSRF). */
@Component
public class ValidateurHote {
    private static final Set<Integer> PORTS_IMAP = Set.of(143, 993);
    private static final Pattern HOTE = Pattern.compile("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$");

    private final boolean dev;

    public ValidateurHote(@Value("${mailflow.mail.dev:false}") boolean dev) {
        this.dev = dev;
    }

    public void verifierImap(String hote, int port, SecuriteConnexion securite) {
        if (!dev) {
            if (!PORTS_IMAP.contains(port)) throw refus("Port IMAP autorisé : 143 ou 993");
            if (securite == SecuriteConnexion.AUCUNE) throw refus("Une connexion chiffrée est obligatoire");
        }
        verifierHote(hote);
    }

    public void verifierHote(String hote) {
        String h = hote == null ? "" : hote.trim();
        if (h.length() > 255 || !HOTE.matcher(h).matches()) throw refus("Nom d'hôte invalide");
        if (dev) return;
        try {
            for (InetAddress a : InetAddress.getAllByName(h)) {
                if (interdite(a)) throw refus("Cet hôte n'est pas autorisé");
            }
        } catch (UnknownHostException e) {
            throw refus("Hôte introuvable");
        }
    }

    private static boolean interdite(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF;
            return b0 == 0 || b0 >= 240 || (b0 == 100 && b1 >= 64 && b1 <= 127);
        }
        return b.length == 16 && (b[0] & 0xFE) == 0xFC; // fc00::/7
    }

    private static ResponseStatusException refus(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
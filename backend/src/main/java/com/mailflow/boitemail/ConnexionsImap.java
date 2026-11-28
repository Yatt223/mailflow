package com.mailflow.boitemail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnexionsImap {
    private final ValidateurHote validateur;

    public Store ouvrir(String hote, int port, SecuriteConnexion securite,
                        String identifiant, String secret) throws MessagingException {
        validateur.verifierImap(hote, port, securite); // revérifié à chaque connexion
        String proto = securite == SecuriteConnexion.SSL ? "imaps" : "imap";

        Properties p = new Properties();
        p.put("mail." + proto + ".connectiontimeout", "10000");
        p.put("mail." + proto + ".timeout", "30000");
        p.put("mail." + proto + ".writetimeout", "30000");
        p.put("mail." + proto + ".peek", "true"); // ne marque aucun mail comme lu
        p.put("mail.mime.address.strict", "false");
        p.put("mail.mime.decodefilename", "true");
        if (securite != SecuriteConnexion.AUCUNE) {
            p.put("mail." + proto + ".ssl.checkserveridentity", "true");
            p.put("mail." + proto + ".ssl.protocols", "TLSv1.2 TLSv1.3");
        }
        if (securite == SecuriteConnexion.STARTTLS) {
            p.put("mail.imap.starttls.enable", "true");
            p.put("mail.imap.starttls.required", "true");
        }
        Store store = Session.getInstance(p).getStore(proto);
        store.connect(hote.trim(), port, identifiant, secret);
        return store;
    }

    public void tester(String hote, int port, SecuriteConnexion securite,
                       String identifiant, String secret) {
        try (Store store = ouvrir(hote, port, securite, identifiant, secret)) {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            inbox.close(false);
        } catch (AuthenticationFailedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Authentification refusée par le serveur");
        } catch (MessagingException e) {
            log.warn("Test de connexion IMAP en échec : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Connexion impossible : vérifiez l'hôte, le port et le chiffrement");
        }
    }
}
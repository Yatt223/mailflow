package com.mailflow.auth;

import com.mailflow.utilisateur.Utilisateur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Duration duree;

    public JwtService(JwtEncoder encoder,
                      @Value("${mailflow.jwt.expiration-minutes}") long minutes) {
        this.encoder = encoder;
        this.duree = Duration.ofMinutes(minutes);
    }

    public TokenResponse generer(Utilisateur u) {
        Instant maintenant = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(u.getId().toString())
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(duree))
                .claim("org", u.getOrganisationId().toString())
                .claim("role", u.getRole().name())
                .claim("email", u.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", duree.toSeconds());
    }
}
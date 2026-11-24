package com.mailflow.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    private static SecretKey cle(String secret) {
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET manquant ou trop court (32 caractères minimum)");
        }
        return new SecretKeySpec(octets, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${mailflow.jwt.secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cle(secret)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${mailflow.jwt.secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(cle(secret))
                .macAlgorithm(MacAlgorithm.HS256).build();
    }
}
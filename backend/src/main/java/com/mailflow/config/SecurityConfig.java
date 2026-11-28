package com.mailflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(roles);

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/liens/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/utilisateurs").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/utilisateurs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/mails").hasAnyRole("ADMIN", "MANAGER", "AGENT")
                        .requestMatchers(HttpMethod.PUT, "/api/mails/*/assignation").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/mails/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/fichiers", "/api/mails/*/fichiers").hasAnyRole("ADMIN", "MANAGER", "AGENT")
                        .requestMatchers(HttpMethod.PUT, "/api/fichiers/*/contenu").hasAnyRole("ADMIN", "MANAGER", "AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/equipes").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/equipes/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/equipes/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/equipes/*/membres/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/equipes/*/membres/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/boites-mail").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/boites-mail", "/api/boites-mail/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/boites-mail/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/boites-mail/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/boites-mail/*/synchroniser").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/boites-mail/*/resynchroniser").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/audit").hasAnyRole("ADMIN", "AUDITEUR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
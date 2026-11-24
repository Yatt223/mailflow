package com.mailflow.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse inscription(@Valid @RequestBody InscriptionRequest req) {
        return service.inscrire(req);
    }

    @PostMapping("/connexion")
    public TokenResponse connexion(@Valid @RequestBody ConnexionRequest req) {
        return service.connecter(req);
    }
}
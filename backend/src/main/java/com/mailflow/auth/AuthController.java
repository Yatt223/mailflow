package com.mailflow.auth;

import jakarta.servlet.http.HttpServletRequest;
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
    public TokenResponse inscription(@Valid @RequestBody InscriptionRequest req,
                                     HttpServletRequest http) {
        return service.inscrire(req, http.getRemoteAddr());
    }

    @PostMapping("/connexion")
    public TokenResponse connexion(@Valid @RequestBody ConnexionRequest req,
                                   HttpServletRequest http) {
        return service.connecter(req, http.getRemoteAddr());
    }
}
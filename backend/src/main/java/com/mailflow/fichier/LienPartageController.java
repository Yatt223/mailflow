package com.mailflow.fichier;

import com.mailflow.auth.Acteur;
import com.mailflow.fichier.LienDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LienPartageController {
    private final LienPartageService service;

    @PostMapping("/fichiers/{id}/liens")
    @ResponseStatus(HttpStatus.CREATED)
    public LienCreeResponse creer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody(required = false) CreationLienRequest req) {
        Integer duree = req == null ? null : req.dureeHeures();
        Integer max = req == null ? null : req.maxTelechargements();
        var cree = service.creer(Acteur.de(jwt), id, duree, max);
        return new LienCreeResponse(cree.lien().getId(), cree.jeton(),
                "/api/liens/" + cree.jeton(), cree.lien().getExpireLe(),
                cree.lien().getMaxTelechargements());
    }

    @GetMapping("/fichiers/{id}/liens")
    public List<LienResponse> lister(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.lister(Acteur.de(jwt), id).stream().map(LienResponse::from).toList();
    }

    @DeleteMapping("/fichiers/{id}/liens/{lienId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoquer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                         @PathVariable UUID lienId) {
        service.revoquer(Acteur.de(jwt), id, lienId);
    }

    // Public : téléchargement par lien
    @GetMapping("/liens/{jeton}")
    public ResponseEntity<InputStreamResource> telecharger(@PathVariable String jeton) {
        var t = service.telecharger(jeton);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(t.fichier().getTypeMime()))
                .contentLength(t.fichier().getTailleOctets())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(t.fichier().getNom(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(t.contenu()));
    }
}
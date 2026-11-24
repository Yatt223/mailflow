package com.mailflow.fichier;

import com.mailflow.auth.Acteur;
import com.mailflow.fichier.FichierDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FichierController {
    private final FichierService service;

    private static final String ECRIVAINS = "hasAnyRole('ADMIN','MANAGER','AGENT')";

    @PostMapping(path = "/fichiers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ECRIVAINS)
    public FichierResponse televerser(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam("fichier") MultipartFile fichier) {
        return FichierResponse.from(service.televerser(Acteur.de(jwt), fichier, null));
    }

    @GetMapping("/fichiers")
    public PagedModel<FichierResponse> mesFichiers(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(service.mesFichiers(Acteur.de(jwt), pageable(page, size))
                .map(FichierResponse::from));
    }

    @GetMapping("/fichiers/partages")
    public PagedModel<FichierResponse> partagesAvecMoi(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(service.partagesAvecMoi(Acteur.de(jwt), pageable(page, size))
                .map(FichierResponse::from));
    }

    @GetMapping("/fichiers/{id}")
    public FichierResponse lire(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return FichierResponse.from(service.lireMeta(Acteur.de(jwt), id));
    }

    @GetMapping("/fichiers/{id}/contenu")
    public ResponseEntity<InputStreamResource> telecharger(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID id) {
        var t = service.telecharger(Acteur.de(jwt), id);
        // Toujours en pièce jointe : jamais affiché ni exécuté dans le navigateur.
        // Spring Security ajoute déjà X-Content-Type-Options: nosniff.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(t.fichier().getTypeMime()))
                .contentLength(t.fichier().getTailleOctets())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(t.fichier().getNom(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(t.contenu()));
    }

    @PutMapping(path = "/fichiers/{id}/contenu", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ECRIVAINS)
    public FichierResponse remplacer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @RequestParam("fichier") MultipartFile fichier) {
        return FichierResponse.from(service.remplacer(Acteur.de(jwt), id, fichier));
    }

    @PatchMapping("/fichiers/{id}")
    public FichierResponse renommer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                    @Valid @RequestBody RenommageRequest req) {
        return FichierResponse.from(service.renommer(Acteur.de(jwt), id, req.nom()));
    }

    @DeleteMapping("/fichiers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.supprimer(Acteur.de(jwt), id);
    }

    // ---------- Partages ----------

    @GetMapping("/fichiers/{id}/partages")
    public List<PartageResponse> lesPartages(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID id) {
        return service.lesPartages(Acteur.de(jwt), id).stream().map(PartageResponse::from).toList();
    }

    @PostMapping("/fichiers/{id}/partages")
    @ResponseStatus(HttpStatus.CREATED)
    public PartageResponse partager(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                    @Valid @RequestBody PartageRequest req) {
        return PartageResponse.from(service.partager(Acteur.de(jwt), id,
                req.utilisateurId(), req.droit(), req.expireLe()));
    }

    @DeleteMapping("/fichiers/{id}/partages/{partageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirerPartage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @PathVariable UUID partageId) {
        service.retirerPartage(Acteur.de(jwt), id, partageId);
    }

    // ---------- Pièces jointes d'un mail ----------

    @PostMapping(path = "/mails/{mailId}/fichiers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ECRIVAINS)
    public FichierResponse joindre(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID mailId,
                                   @RequestParam("fichier") MultipartFile fichier) {
        return FichierResponse.from(service.televerser(Acteur.de(jwt), fichier, mailId));
    }

    @GetMapping("/mails/{mailId}/fichiers")
    public List<FichierResponse> piecesJointes(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID mailId) {
        return service.piecesJointes(Acteur.de(jwt), mailId).stream()
                .map(FichierResponse::from).toList();
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "creeLe"));
    }
}
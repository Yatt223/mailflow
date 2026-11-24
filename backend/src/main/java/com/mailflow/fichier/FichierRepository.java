package com.mailflow.fichier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FichierRepository extends JpaRepository<Fichier, UUID> {

    Optional<Fichier> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<Fichier> findByMailIdAndOrganisationIdOrderByCreeLeAsc(UUID mailId, UUID organisationId);

    @Query("select f from Fichier f where f.organisationId = :org "
            + "and f.proprietaireId = :uid and f.mailId is null")
    Page<Fichier> mesFichiers(@Param("org") UUID org, @Param("uid") UUID uid, Pageable pageable);

    @Query("select f from Fichier f where f.organisationId = :org and f.id in "
            + "(select p.fichierId from Partage p where p.utilisateurId = :uid "
            + "and (p.expireLe is null or p.expireLe > :maintenant))")
    Page<Fichier> partagesAvec(@Param("org") UUID org, @Param("uid") UUID uid,
                               @Param("maintenant") Instant maintenant, Pageable pageable);
}
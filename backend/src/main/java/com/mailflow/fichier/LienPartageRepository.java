package com.mailflow.fichier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LienPartageRepository extends JpaRepository<LienPartage, UUID> {

    List<LienPartage> findByFichierIdOrderByCreeLeDesc(UUID fichierId);

    Optional<LienPartage> findByJetonHash(String jetonHash);

    // Consommation atomique : deux téléchargements simultanés ne peuvent pas dépasser la limite.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LienPartage l set l.nbTelechargements = l.nbTelechargements + 1 "
            + "where l.jetonHash = :hash and l.expireLe > :maintenant "
            + "and (l.maxTelechargements is null or l.nbTelechargements < l.maxTelechargements)")
    int consommer(@Param("hash") String hash, @Param("maintenant") Instant maintenant);
}
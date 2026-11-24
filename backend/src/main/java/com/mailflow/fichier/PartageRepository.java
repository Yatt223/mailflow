package com.mailflow.fichier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartageRepository extends JpaRepository<Partage, UUID> {

    List<Partage> findByFichierId(UUID fichierId);

    Optional<Partage> findByFichierIdAndUtilisateurId(UUID fichierId, UUID utilisateurId);

    @Query("select p from Partage p where p.fichierId = :fid and p.utilisateurId = :uid "
            + "and (p.expireLe is null or p.expireLe > :maintenant)")
    List<Partage> actifs(@Param("fid") UUID fid, @Param("uid") UUID uid,
                         @Param("maintenant") Instant maintenant);
}
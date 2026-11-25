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

    Optional<Partage> findByFichierIdAndEquipeId(UUID fichierId, UUID equipeId);

    // Partages actifs d'un fichier pour un utilisateur : directs ou via une équipe dont il est membre
    @Query("select p from Partage p where p.fichierId = :fid "
            + "and (p.expireLe is null or p.expireLe > :maintenant) "
            + "and (p.utilisateurId = :uid or p.equipeId in "
            + "(select m.id.equipeId from EquipeMembre m where m.id.utilisateurId = :uid))")
    List<Partage> actifsPourUtilisateur(@Param("fid") UUID fid, @Param("uid") UUID uid,
                                        @Param("maintenant") Instant maintenant);
}
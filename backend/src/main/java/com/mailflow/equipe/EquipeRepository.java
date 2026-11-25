package com.mailflow.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipeRepository extends JpaRepository<Equipe, UUID> {

    Optional<Equipe> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<Equipe> findByOrganisationIdAndNomIgnoreCase(UUID organisationId, String nom);

    List<Equipe> findByOrganisationIdOrderByNomAsc(UUID organisationId);

    @Query("select e from Equipe e where e.organisationId = :org and e.id in "
            + "(select m.id.equipeId from EquipeMembre m where m.id.utilisateurId = :uid) "
            + "order by e.nom")
    List<Equipe> dont(@Param("org") UUID org, @Param("uid") UUID uid);
}
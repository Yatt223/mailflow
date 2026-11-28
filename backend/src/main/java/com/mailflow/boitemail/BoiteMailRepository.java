package com.mailflow.boitemail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoiteMailRepository extends JpaRepository<BoiteMail, UUID> {

    Optional<BoiteMail> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<BoiteMail> findByOrganisationIdOrderByNomAsc(UUID organisationId);

    List<BoiteMail> findByActifTrue();

    boolean existsByOrganisationIdAndAdresseIgnoreCase(UUID organisationId, String adresse);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BoiteMail b set b.derniereUid = :uid, b.uidValidity = :validity "
            + "where b.id = :id and (b.derniereUid is null or b.derniereUid < :uid "
            + "or b.uidValidity is null or b.uidValidity <> :validity)")
    int avancer(@Param("id") UUID id, @Param("uid") long uid, @Param("validity") long validity);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BoiteMail b set b.derniereSynchro = :quand, b.derniereErreur = :erreur where b.id = :id")
    int noterResultat(@Param("id") UUID id, @Param("quand") Instant quand, @Param("erreur") String erreur);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BoiteMail b set b.derniereUid = 0 where b.id = :id")
    int reinitialiserCurseur(@Param("id") UUID id);
}
package com.mailflow.mail;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface MailRepository extends JpaRepository<Mail, UUID>, JpaSpecificationExecutor<Mail> {

    Optional<Mail> findByIdAndOrganisationId(UUID id, UUID organisationId);

    // Prise en charge atomique : si deux agents cliquent en même temps, un seul gagne.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Mail m set m.assigneA = :uid "
            + "where m.id = :id and m.organisationId = :org and m.assigneA is null")
    int prendreSiLibre(@Param("id") UUID id, @Param("org") UUID org, @Param("uid") UUID uid);
    boolean existsByBoiteMailIdAndMessageId(UUID boiteMailId, String messageId);
}
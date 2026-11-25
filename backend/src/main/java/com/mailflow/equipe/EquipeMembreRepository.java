package com.mailflow.equipe;

import com.mailflow.utilisateur.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface EquipeMembreRepository extends JpaRepository<EquipeMembre, EquipeMembreId> {

    @Query("select count(m) > 0 from EquipeMembre m "
            + "where m.id.equipeId = :eid and m.id.utilisateurId = :uid")
    boolean estMembre(@Param("eid") UUID equipeId, @Param("uid") UUID utilisateurId);

    @Query("select u from Utilisateur u where u.id in "
            + "(select m.id.utilisateurId from EquipeMembre m where m.id.equipeId = :eid) "
            + "order by u.nom, u.prenom")
    List<Utilisateur> utilisateursDe(@Param("eid") UUID equipeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EquipeMembre m where m.id.equipeId = :eid and m.id.utilisateurId = :uid")
    int retirer(@Param("eid") UUID equipeId, @Param("uid") UUID utilisateurId);
}
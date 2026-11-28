package com.mailflow.audit;

import com.mailflow.audit.AuditDtos.EntreeAuditResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final JdbcClient jdbc;
    private final EntityManager em;

    // Rejoint la transaction en cours (atomique avec l'action), sinon en ouvre une.
    @Transactional
    public void enregistrer(UUID organisationId, UUID utilisateurId, String action,
                            String typeObjet, UUID objetId, Map<String, String> details) {
        em.flush(); // les entités créées dans la même transaction doivent exister avant l'insertion
        jdbc.sql("""
                insert into journal_audit
                    (organisation_id, utilisateur_id, action, type_objet, objet_id, details)
                values (:org, :uid, :action, :type, :objet, cast(:details as jsonb))
                """)
                .param("org", organisationId)
                .param("uid", utilisateurId)
                .param("action", action)
                .param("type", typeObjet)
                .param("objet", objetId)
                .param("details", enJson(details))
                .update();
    }

    @Transactional(readOnly = true)
    public Page<EntreeAuditResponse> consulter(UUID organisationId, String action, UUID utilisateurId,
                                               String typeObjet, UUID objetId, Instant du, Instant au,
                                               int page, int taille) {
        StringBuilder where = new StringBuilder(" where e.organisation_id = :org");
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("org", organisationId);
        if (action != null && !action.isBlank()) {
            where.append(" and e.action = :action");
            p.put("action", action.trim());
        }
        if (utilisateurId != null) {
            where.append(" and e.utilisateur_id = :uid");
            p.put("uid", utilisateurId);
        }
        if (typeObjet != null && !typeObjet.isBlank()) {
            where.append(" and e.type_objet = :type");
            p.put("type", typeObjet.trim());
        }
        if (objetId != null) {
            where.append(" and e.objet_id = :objet");
            p.put("objet", objetId);
        }
        if (du != null) {
            where.append(" and e.cree_le >= :du");
            p.put("du", OffsetDateTime.ofInstant(du, ZoneOffset.UTC));
        }
        if (au != null) {
            where.append(" and e.cree_le < :au");
            p.put("au", OffsetDateTime.ofInstant(au, ZoneOffset.UTC));
        }

        long total = jdbc.sql("select count(*) from journal_audit e" + where)
                .params(p).query(Long.class).single();

        Map<String, Object> pp = new LinkedHashMap<>(p);
        pp.put("limite", taille);
        pp.put("decalage", (long) page * taille);
        List<EntreeAuditResponse> lignes = jdbc.sql("""
                select e.id, e.utilisateur_id, u.email as utilisateur_email, e.action,
                       e.type_objet, e.objet_id, cast(e.details as text) as details, e.cree_le
                from journal_audit e
                left join utilisateur u on u.id = e.utilisateur_id
                """ + where + " order by e.cree_le desc, e.id desc limit :limite offset :decalage")
                .params(pp)
                .query((rs, n) -> new EntreeAuditResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("utilisateur_id", UUID.class),
                        rs.getString("utilisateur_email"),
                        rs.getString("action"),
                        rs.getString("type_objet"),
                        rs.getObject("objet_id", UUID.class),
                        rs.getString("details"),
                        rs.getObject("cree_le", OffsetDateTime.class).toInstant()))
                .list();
        return new PageImpl<>(lignes, PageRequest.of(page, taille), total);
    }

    // JSON plat (clés et valeurs texte), échappé à la main pour ne dépendre d'aucune bibliothèque
    private static String enJson(Map<String, String> details) {
        if (details == null || details.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        boolean premier = true;
        for (Map.Entry<String, String> e : details.entrySet()) {
            if (!premier) sb.append(',');
            premier = false;
            sb.append('"').append(echapper(e.getKey())).append("\":\"")
                    .append(echapper(e.getValue())).append('"');
        }
        return sb.append('}').toString();
    }

    private static String echapper(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
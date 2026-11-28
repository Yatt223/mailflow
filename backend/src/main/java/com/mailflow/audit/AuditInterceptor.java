package com.mailflow.audit;

import com.mailflow.auth.Acteur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {
    static final String ATTR_REPONSE = "mailflow.audit.reponse";

    // Seuls ces champs de la réponse sont enregistrés (jamais le corps d'un mail, un jeton, un email...)
    private static final Set<String> CHAMPS_UTILES = Set.of("statut", "droit", "role",
            "assigneA", "equipeId", "utilisateurId", "expireLe", "maxTelechargements", "actif");

    private final AuditService audit;

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod hm)) return;
        Audit a = hm.getMethodAnnotation(Audit.class);
        if (a == null) return;

        boolean refus = res.getStatus() == 403 || ex instanceof AccessDeniedException;
        boolean succes = ex == null && res.getStatus() >= 200 && res.getStatus() < 300;
        if (!refus && !succes) return;
        if (!(SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken jwt)) return;

        try {
            Acteur acteur = Acteur.de(jwt.getToken());
            Map<String, String> details = new LinkedHashMap<>();
            UUID objetId = null;

            @SuppressWarnings("unchecked")
            Map<String, String> variables = (Map<String, String>)
                    req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (variables != null) {
                objetId = versUuid(variables.get("id"));
                if (objetId == null) objetId = versUuid(variables.get("mailId"));
                for (Map.Entry<String, String> e : variables.entrySet()) {
                    UUID v = versUuid(e.getValue());
                    if (v != null && !v.equals(objetId)) details.put(e.getKey(), v.toString());
                }
            }

            if (refus) {
                details.put("tentative", a.action());
            } else {
                Object corps = req.getAttribute(ATTR_REPONSE);
                if (corps != null && corps.getClass().isRecord()) {
                    for (RecordComponent c : corps.getClass().getRecordComponents()) {
                        Object v = c.getAccessor().invoke(corps);
                        if (v == null) continue;
                        if (c.getName().equals("id") && v instanceof UUID u) {
                            if (objetId == null) objetId = u;
                            else if (!u.equals(objetId)) details.put("creeId", u.toString());
                        } else if (CHAMPS_UTILES.contains(c.getName())) {
                            details.put(c.getName(), v.toString());
                        }
                    }
                }
            }
            details.put("ip", String.valueOf(req.getRemoteAddr()));

            audit.enregistrer(acteur.organisationId(), acteur.id(),
                    refus ? "ACCES_REFUSE" : a.action(), a.type(), objetId, details);
        } catch (Exception e) {
            log.error("Écriture d'audit impossible pour l'action {}", a.action(), e);
        }
    }

    private static UUID versUuid(String valeur) {
        if (valeur == null) return null;
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
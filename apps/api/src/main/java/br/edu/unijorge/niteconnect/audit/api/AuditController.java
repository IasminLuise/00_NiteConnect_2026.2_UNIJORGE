package br.edu.unijorge.niteconnect.audit.api;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditController {
    private final IdentityDirectory identities;
    private final AccessService access;
    private final AuditTrail audit;
    public AuditController(IdentityDirectory identities, AccessService access, AuditTrail audit) {
        this.identities = identities; this.access = access; this.audit = audit;
    }
    @GetMapping("/api/v1/audit-events") @Transactional(readOnly=true)
    PageResult<AuditTrail.Event> events(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue="0") int page,
                                       @RequestParam(defaultValue="20") int size) {
        access.requireAdministrator(identities.member(jwt));
        return audit.list(page, size);
    }
}

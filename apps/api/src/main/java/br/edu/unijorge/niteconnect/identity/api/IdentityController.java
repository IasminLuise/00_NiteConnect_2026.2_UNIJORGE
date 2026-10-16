package br.edu.unijorge.niteconnect.identity.api;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.members.domain.Member;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
public class IdentityController {
    private final IdentityDirectory identities;
    private final AccessService access;
    public IdentityController(IdentityDirectory identities, AccessService access) { this.identities = identities; this.access = access; }
    @GetMapping("/api/v1/me") @Transactional(readOnly=true)
    Me me(@AuthenticationPrincipal Jwt jwt) {
        UUID member = identities.member(jwt);
        return new Me(access.requireActive(member), access.effectiveRoles(member));
    }
    public record Me(Member member, List<String> roles) {}
}

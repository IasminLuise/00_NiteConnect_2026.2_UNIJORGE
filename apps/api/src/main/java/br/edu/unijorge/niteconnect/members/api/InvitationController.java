package br.edu.unijorge.niteconnect.members.api;

import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.members.application.InvitationService;
import br.edu.unijorge.niteconnect.members.domain.Member;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@ConditionalOnProperty(name="nite.entra.enabled", havingValue="true")
public class InvitationController {
    private final InvitationService invitations;
    private final IdentityDirectory identities;
    public InvitationController(InvitationService invitations, IdentityDirectory identities) { this.invitations=invitations; this.identities=identities; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    InvitationService.Invitation create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Create input) {
        return invitations.create(identities.member(jwt), input.memberId(), input.objectId(), input.expiresAt(), input.reason());
    }
    @GetMapping
    PageResult<InvitationService.Invitation> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return invitations.list(identities.member(jwt), page, size);
    }
    @PostMapping("/{id}/accept")
    Member accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return invitations.accept(id, identities.tenant(jwt), identities.object(jwt));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody Revoke input) {
        invitations.revoke(identities.member(jwt), id, input.reason());
    }
    public record Create(@NotNull UUID memberId, @NotNull UUID objectId, @NotNull @Future Instant expiresAt,
                         @NotBlank @Size(max=1000) String reason) {}
    public record Revoke(@NotBlank @Size(max=1000) String reason) {}
}

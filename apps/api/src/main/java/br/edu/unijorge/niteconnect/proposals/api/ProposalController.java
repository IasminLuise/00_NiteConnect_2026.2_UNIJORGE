package br.edu.unijorge.niteconnect.proposals.api;

import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.projects.domain.Project;
import br.edu.unijorge.niteconnect.proposals.application.ProposalService;
import br.edu.unijorge.niteconnect.proposals.domain.Proposal;
import br.edu.unijorge.niteconnect.proposals.domain.ProposalStatus;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/proposals")
public class ProposalController {
    private final ProposalService proposals;
    private final IdentityDirectory identities;
    public ProposalController(ProposalService proposals, IdentityDirectory identities) { this.proposals = proposals; this.identities = identities; }
    @GetMapping
    PageResult<Proposal> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue="0") int page,
                             @RequestParam(defaultValue="20") int size, @RequestParam(required=false) ProposalStatus status) {
        return proposals.list(identities.member(jwt), status, page, size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Proposal create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProposalContracts.Create input) {
        return proposals.create(identities.member(jwt), input.title(), input.description(), input.reason());
    }
    @GetMapping("/{id}")
    Proposal read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return proposals.read(identities.member(jwt), id); }
    @PatchMapping("/{id}")
    Proposal update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProposalContracts.Update input) {
        return proposals.update(identities.member(jwt), id, input.title(), input.description(), input.expectedVersion(), input.reason());
    }
    @PostMapping("/{id}/decisions")
    Proposal decide(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProposalContracts.Decision input) {
        return proposals.decide(identities.member(jwt), id, input.status(), input.expectedVersion(), input.reason());
    }
    @PostMapping("/{id}/convert") @ResponseStatus(HttpStatus.CREATED)
    Project convert(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProposalContracts.Convert input) {
        return proposals.convert(identities.member(jwt), id, input.expectedVersion(), input.reason());
    }
}
